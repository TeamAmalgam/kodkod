package kodkod.multiobjective.algorithms;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import kodkod.ast.Formula;
import kodkod.engine.IncrementalSolver;
import kodkod.engine.Solution;
import kodkod.instance.Bounds;
import kodkod.multiobjective.MetricPoint;
import kodkod.multiobjective.MultiObjectiveOptions;
import kodkod.multiobjective.MultiObjectiveProblem;
import kodkod.multiobjective.Objective;
import kodkod.multiobjective.concurrency.SolutionNotifier;
import kodkod.multiobjective.statistics.StepCounter;

public class PartitionedGuidedImprovementAlgorithm extends MultiObjectiveAlgorithm {

    public PartitionedGuidedImprovementAlgorithm(String desc, MultiObjectiveOptions options) {
        super(desc, options, Logger.getLogger(PartitionedGuidedImprovementAlgorithm.class.toString()));
    }

    @Override
    public void multiObjectiveSolve(MultiObjectiveProblem problem, SolutionNotifier notifier) {
        // set the bit width
        setBitWidth(problem.getBitWidth());

        // for the evaluation we need a step counter
        this.counter = new StepCounter();

        //begin, amongst others, start the timer
        begin();

        // Throw a dart and get a starting point
        Solution solution = getSolver().solve(problem.getConstraints(), problem.getBounds());
        incrementStats(solution, problem, problem.getConstraints(), true, null);
        solveFirstStats(solution);

        MetricPoint startingValues = MetricPoint.measure(solution, problem.getObjectives(), getOptions());
        logger.log(Level.FINE, "Found a solution. At time: {0}, with values {1}", new Object[] { Integer.valueOf((int)(System.currentTimeMillis()-startTime)/1000), startingValues.values() });

        // Find the boundaries of the search space
        Formula boundaryConstraints = findBoundaries(problem, startingValues);

        // Create the ForkJoinPool
        // Number of threads is MIN(user value, # cores)
        // TODO: Make "user value" configurable
        int poolSize = Math.min(8, Runtime.getRuntime().availableProcessors());
        ForkJoinPool pool = new ForkJoinPool(poolSize);

        // Create the first task and submit it to the pool
        PartitionSpaceTask task = new PartitionSpaceTask(problem, boundaryConstraints, notifier);

        // Wait for the thread to return
        pool.invoke(task);

        end(notifier);
        debugWriteStatistics();
    }

    private Formula findBoundaries(MultiObjectiveProblem problem, MetricPoint startingValues) {
        List<Formula> boundaries = new ArrayList<Formula>(problem.getObjectives().size());

        // Disable MetricPoint logger temporarily
        // Multiple threads will be calling the logger, so the output won't make sense
        Logger metricPointLogger = Logger.getLogger(MetricPoint.class.toString());
        Level metricPointLevel = metricPointLogger.getLevel();
        metricPointLogger.setLevel(Level.INFO);

        // Create a thread pool to execute the tasks
        // Number of threads is MIN(user value, # cores, # objectives)
        // TODO: Make "user value" configurable
        int numObjectives = problem.getObjectives().size();
        int numThreads = Math.min(8, Math.min(Runtime.getRuntime().availableProcessors(), numObjectives));
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        CompletionService<Formula> ecs = new ExecutorCompletionService<Formula>(threadPool);

        logger.log(Level.FINE, "Starting thread pool with {0} threads and {1} jobs", new Object[] { numThreads, numObjectives });

        // Create new BoundaryFinder tasks for each objective
        for (final Objective objective : problem.getObjectives()) {
            ecs.submit(new BoundaryFinder(problem, objective, startingValues));
        }

        // take() returns a Future<Formula> if one exists; blocks otherwise
        // Once we have the Formula result, add it to our list
        for (int i = 0; i < numObjectives; i++) {
            Formula result;
            try {
                result = ecs.take().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
            boundaries.add(result);
        }

        // Now we can shutdown the threadPool
        threadPool.shutdown();

        // Re-enable MetricPoint logging
        metricPointLogger.setLevel(metricPointLevel);

        StringBuilder sb = new StringBuilder("All boundaries found. At time: ");
        sb.append(Integer.valueOf((int)(System.currentTimeMillis()-startTime)/1000));
        sb.append(", Boundaries are conjunction of ");
        for (Formula boundary : boundaries) {
            sb.append("\n\t");
            sb.append(boundary);
        }
        logger.log(Level.FINE, sb.toString());

        return Formula.and(boundaries);
    }

    // A Callable is like a Runnable, but it returns a result
    // In this case, we want to return the Formula representing the boundary for the given objective
    private class BoundaryFinder implements Callable<Formula> {

        private final MultiObjectiveProblem problem;
        private final Objective objective;
        private MetricPoint currentValues;

        BoundaryFinder(MultiObjectiveProblem problem, Objective objective, MetricPoint startingValues) {
            this.problem = problem;
            this.objective = objective;
            this.currentValues = startingValues;
        }

        @Override
        public Formula call() throws Exception {
            IncrementalSolver incrementalSolver = IncrementalSolver.solver(getOptions());
            Formula boundaryConstraint = currentValues.objectiveImprovementConstraint(objective);

            Formula constraint = problem.getConstraints().and(boundaryConstraint);
            Solution solution = incrementalSolver.solve(constraint, problem.getBounds());
            incrementStats(solution, problem, constraint, false, null);

            // Work up to the boundary of this metric
            while (isSat(solution)) {
                currentValues = MetricPoint.measure(solution, problem.getObjectives(), getOptions());
                boundaryConstraint = currentValues.objectiveImprovementConstraint(objective);
                solution = incrementalSolver.solve(boundaryConstraint, new Bounds(problem.getBounds().universe()));
                incrementStats(solution, problem, constraint, false, null);
            }
            incrementalSolver.free();
            return currentValues.boundaryConstraint(objective);
        }

    }

    // RecursiveAction is a ForkJoinTask that doesn't return a result
    // One potential issue is that we insert solutions into a blocking queue and use a shared counter;
    // this is something that should be minimized (or eliminated) from ForkJoinTasks.
    private class PartitionSpaceTask extends RecursiveAction {

        private static final long serialVersionUID = 1L;
        private final MultiObjectiveProblem problem;
        private Formula boundaryConstraints;
        private final SolutionNotifier notifier;

        public PartitionSpaceTask(MultiObjectiveProblem problem, Formula boundaryConstraints, SolutionNotifier notifier) {
            this.problem = problem;
            this.boundaryConstraints = boundaryConstraints;
            this.notifier = notifier;
        }

        @Override
        protected void compute() {
            // Throw the dart within our partition
            IncrementalSolver solver = IncrementalSolver.solver(getOptions());
            Formula constraints = problem.getConstraints().and(boundaryConstraints);
            Solution solution = solver.solve(constraints, problem.getBounds());
            incrementStats(solution, problem, constraints, false, constraints);

            // If unsat, then we're done, so return without splitting
            if (!isSat(solution)) {
                return;
            }

            //count this step but first go to new index because it's a new base point
            // TODO: Semantics are wrong if this is the first task computed. We don't want to call nextIndex() yet
            //counter.nextIndex();
            //counter.countStep();

            // Work up to the Pareto front
            MetricPoint currentValues = null;
            Solution previousSolution = null;
            while (isSat(solution)) {
              currentValues = MetricPoint.measure(solution, problem.getObjectives(), getOptions());
              logger.log(Level.FINE, "Found a solution. At time: {0}, Improving on {1}", new Object[] { Integer.valueOf((int)(System.currentTimeMillis()-startTime)/1000), currentValues.values() });

              final Formula improvementConstraints = currentValues.parametrizedImprovementConstraints();

              previousSolution = solution;
              solution = solver.solve(improvementConstraints, new Bounds(problem.getBounds().universe()));
              incrementStats(solution, problem, improvementConstraints, false, improvementConstraints);

              //counter.countStep();
            }
            foundParetoPoint(currentValues);

            // Free the solver's resources since we will be creating a new solver
            solver.free();

            if (!options.allSolutionsPerPoint()) {
                // no magnifying glass
                // previous solution was on the pareto front: report it
                tell(notifier, previousSolution, currentValues);
            } else {
                // magnifying glass
                final Collection<Formula> assignmentsConstraints = currentValues.assignmentConstraints();
                assignmentsConstraints.add(problem.getConstraints());
                int solutionsFound = magnifier(Formula.and(assignmentsConstraints), problem.getBounds(), currentValues, notifier);
                logger.log(Level.FINE, "Magnifying glass found {0} solution(s). At time: {1}", new Object[] {Integer.valueOf(solutionsFound), Integer.valueOf((int)((System.currentTimeMillis()-startTime)/1000))});
            }

            // Now we can split the search space based on the Pareto point and create new tasks
            // For n metrics, we want all combinations of m_i <= M_i and m_i >= M_i where M_i is the current value
            // To iterate over this, we map the bits of a bitset to the different combinations of metrics
            // We skip 0 (the partition that is already dominated) and 2^n (the partition where we didn't find any solutions)
            // Store the tasks in a list so we can call invokeAll() on the entire list
            List<PartitionSpaceTask> partitionTasks = new ArrayList<PartitionSpaceTask>();
            int maxMapping = (int) Math.pow(2, problem.getObjectives().size());
            for (int mapping = 1; mapping < maxMapping; mapping++) {
                BitSet set = BitSet.valueOf(new long[] { mapping });
                // Get the constraints for this particular mapping, and add to a list of tasks
                Formula constraint = boundaryConstraints.and(currentValues.partitionConstraints(set));
                partitionTasks.add(new PartitionSpaceTask(problem, constraint, notifier));
            }

            // Fork and join all the tasks
            invokeAll(partitionTasks);
        }

    }
}
