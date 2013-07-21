package kodkod.multiobjective.algorithms;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
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

        final List<Formula> exclusionConstraints = new ArrayList<Formula>();
        exclusionConstraints.add(problem.getConstraints());

        // Disable MetricPoint logger temporarily
        // Multiple threads will be calling the logger, so the output won't make sense
        Logger metricPointLogger = Logger.getLogger(MetricPoint.class.toString());
        Level metricPointLevel = metricPointLogger.getLevel();
        metricPointLogger.setLevel(Level.INFO);
        
        // Skip boundary finding if there's only one objective
        if (problem.getObjectives().size() > 10) {
	        // Throw a dart and get a starting point
	        Solution solution = getSolver().solve(problem.getConstraints(), problem.getBounds());
	        incrementStats(solution, problem, problem.getConstraints(), true, null);
	        solveFirstStats(solution);
	
	        MetricPoint startingValues = MetricPoint.measure(solution, problem.getObjectives(), getOptions());
	        logger.log(Level.FINE, "Found a solution. At time: {0}, with values {1}", new Object[] { Integer.valueOf((int)(System.currentTimeMillis()-startTime)/1000), startingValues.values() });
		        
	        // Find the boundaries of the search space
	        Formula boundaryConstraints = findBoundaries(problem, startingValues);
	        exclusionConstraints.add(boundaryConstraints);
        }
        
        // For now, special case 2 and 3 objectives
        // All others will fall back to IGIA
        
        if (problem.getObjectives().size() == 2 || problem.getObjectives().size() == 3) {
            // Throw the dart within the current partition
            IncrementalSolver solver = IncrementalSolver.solver(getOptions());
            Formula constraint = Formula.and(exclusionConstraints);
            Solution solution = solver.solve(constraint, problem.getBounds());
            incrementStats(solution, problem, constraint, false, constraint);
            
            // Work up to the Pareto point
            MetricPoint currentValues = null;
            Solution previousSolution = null;
            while (isSat(solution)) {
	            currentValues = MetricPoint.measure(solution, problem.getObjectives(), getOptions());
	            Formula improvementConstraints = currentValues.parametrizedImprovementConstraints();
	            previousSolution = solution;
	            solution = solver.solve(improvementConstraints, new Bounds(problem.getBounds().universe()));
	            incrementStats(solution, problem, improvementConstraints, false, improvementConstraints);
            }
            foundParetoPoint(currentValues);
            
	        // Free the solver's resources, since we will be creating a new solver
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
	            logger.log(Level.FINE, "Magnifying glass on {0} found {1} solution(s). At time: {2}", new Object[] {currentValues.values(), Integer.valueOf(solutionsFound), Integer.valueOf((int)((System.currentTimeMillis()-startTime)/1000))});
	        }
	        exclusionConstraints.add(currentValues.exclusionConstraint());
	        
	        // Now split into partitions and schedule those tasks
	        
        	if (problem.getObjectives().size() == 2) {
		        // Number of threads is MIN(# objectives = 2, # cores)
		        BlockingQueue<Future<Formula>> futures = new LinkedBlockingQueue<Future<Formula>>();
		        int poolSize = Math.min(2, Runtime.getRuntime().availableProcessors());
		        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize);
		        logger.log(Level.FINE, "Starting a thread pool with {0} threads", new Object[] { poolSize });
		        
		        // Wrap the computation in a Future, so we can block until all tasks are done
		        // Queue up the lower right partition
		        BitSet set = BitSet.valueOf(new long[] { 1 });
		        constraint = Formula.and(exclusionConstraints).and(currentValues.partitionConstraints(set));
		        Future<Formula> future = threadPool.submit(new PartitionedSearcher(problem, constraint, notifier, null));
		        futures.add(future);
		        // Queue up the upper left partition
		        set = BitSet.valueOf(new long[] { 2 });
		        constraint = Formula.and(exclusionConstraints).and(currentValues.partitionConstraints(set));
		        future = threadPool.submit(new PartitionedSearcher(problem, constraint, notifier, null));
		        futures.add(future);
		        
		        // We know there are only two tasks in the pool, so try to take from the futures queue twice
		        for (int i = 0; i < 2; i++) {
		        	try {
		        		futures.take().get();
		        	} catch (InterruptedException | ExecutionException e) {
		        		e.printStackTrace();
		        		throw new RuntimeException(e);
		        	}
		        }
		        threadPool.shutdown();
        	} else if (problem.getObjectives().size() == 3) {
		        // Number of threads is MIN(# objectives = 3, # cores)
		        BlockingQueue<Future<Formula>> futures = new LinkedBlockingQueue<Future<Formula>>();
		        int poolSize = Math.min(3, Runtime.getRuntime().availableProcessors());
		        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize);
		        logger.log(Level.FINE, "Starting a thread pool with {0} threads", new Object[] { poolSize });
		        
		        // Wrap the computation in a Future, so we can block until all tasks are done
		        BitSet set = BitSet.valueOf(new long[] { 3 });
		        constraint = Formula.and(exclusionConstraints).and(currentValues.partitionConstraints(set));
		        Future<Formula> future3 = threadPool.submit(new PartitionedSearcher(problem, constraint, notifier, null));
		        futures.add(future3);
		        set = BitSet.valueOf(new long[] { 5 });
		        constraint = Formula.and(exclusionConstraints).and(currentValues.partitionConstraints(set));
		        Future<Formula> future5 = threadPool.submit(new PartitionedSearcher(problem, constraint, notifier, null));
		        futures.add(future5);
		        set = BitSet.valueOf(new long[] { 6 });
		        constraint = Formula.and(exclusionConstraints).and(currentValues.partitionConstraints(set));
		        Future<Formula> future6 = threadPool.submit(new PartitionedSearcher(problem, constraint, notifier, null));
		        futures.add(future6);

		        
		        // First batch of three done, next batch
		        set = BitSet.valueOf(new long[] { 4 });
		        constraint = Formula.and(exclusionConstraints).and(currentValues.partitionConstraints(set));
		        Queue<Future<Formula>> dependencies = new LinkedList<Future<Formula>>();
		        dependencies.add(future5);
		        dependencies.add(future6);
		        Future<Formula> future4 = threadPool.submit(new PartitionedSearcher(problem, constraint, notifier, dependencies));
		        futures.add(future4);
		        set = BitSet.valueOf(new long[] { 2 });
		        constraint = Formula.and(exclusionConstraints).and(currentValues.partitionConstraints(set));
		        dependencies = new LinkedList<Future<Formula>>();
		        dependencies.add(future6);
		        dependencies.add(future3);
		        Future<Formula> future2 = threadPool.submit(new PartitionedSearcher(problem, constraint, notifier, dependencies));
		        futures.add(future2);
		        set = BitSet.valueOf(new long[] { 1 });
		        constraint = Formula.and(exclusionConstraints).and(currentValues.partitionConstraints(set));
		        dependencies = new LinkedList<Future<Formula>>();
		        dependencies.add(future5);
		        dependencies.add(future3);
		        Future<Formula> future1 = threadPool.submit(new PartitionedSearcher(problem, constraint, notifier, dependencies));
		        futures.add(future1);
		        
		        // We know there are only three tasks in the pool, so try to take from the futures queue twice
		        for (int i = 0; i < 6; i++) {
		        	try {
		        		Formula f = futures.take().get();
		        	} catch (InterruptedException | ExecutionException e) {
		        		e.printStackTrace();
		        		throw new RuntimeException(e);
		        	}
		        }
		        
		        threadPool.shutdown();
        	}
		
	        logger.log(Level.FINE, "All Pareto points found. At time: {0}", Integer.valueOf((int)(System.currentTimeMillis()-startTime)/1000));
	
	        // Re-enable MetricPoint logging
	        metricPointLogger.setLevel(metricPointLevel);
	        
	        end(notifier);
	        debugWriteStatistics();
        } else {
        	MultiObjectiveAlgorithm algorithm = new IncrementalGuidedImprovementAlgorithm("IGIA", options);
        	algorithm.multiObjectiveSolve(problem, notifier);
        }
    }

    private class PartitionedSearcher implements Callable<Formula> {

        private final MultiObjectiveProblem problem;
        private Formula constraints;
        private Formula exclusionConstraints;
        private final SolutionNotifier notifier;
        private final Queue<Future<Formula>> dependencies;

        public PartitionedSearcher(MultiObjectiveProblem problem, Formula constraints, SolutionNotifier notifier, Queue<Future<Formula>> dependencies) {
            this.problem = problem;
            this.constraints = constraints;
            this.exclusionConstraints = Formula.constant(true);
            this.notifier = notifier;
            this.dependencies = dependencies;
        }

        @Override
        public Formula call() {
        	// Wait for dependencies
        	// Add the exclusion constraints from dependencies
        	if (dependencies != null) {
        		for (Future<Formula> dep : dependencies) {
        			try {
						Formula f = dep.get();
						constraints = constraints.and(f);
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
        		}
        	}
        	//logger.log(Level.FINE, "Entering region with constraints: {0}", exclusionConstraints.toString());

            // Throw the dart within the current partition
            IncrementalSolver solver = IncrementalSolver.solver(getOptions());
            Solution solution = solver.solve(constraints, problem.getBounds());
            incrementStats(solution, problem, constraints, false, constraints);

            // Unsat means nothing in this partition, so we're done
            if (!isSat(solution)) {
            	//logger.log(Level.FINE, "No solution found in the region: {0}", exclusionConstraints.toString());
                return Formula.constant(true);
            }

            // TODO: Semantics are wrong if this is the first Pareto point; don't want to call nextIndex() yet
            // counter.nextIndex();
            // counter.countStep();

            // Work up to the Pareto front
	        MetricPoint currentValues = null;
	        Solution previousSolution = null;
            while (isSat(solution)) {
		        while (isSat(solution)) {
		            currentValues = MetricPoint.measure(solution, problem.getObjectives(), getOptions());
		            Formula improvementConstraints = currentValues.parametrizedImprovementConstraints();
		            previousSolution = solution;
		            solution = solver.solve(improvementConstraints, new Bounds(problem.getBounds().universe()));
		            incrementStats(solution, problem, improvementConstraints, false, improvementConstraints);
		            //counter.countStep();
		        }
		        foundParetoPoint(currentValues);
		
		        // Free the solver's resources, since we will be creating a new solver
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
		            logger.log(Level.FINE, "Magnifying glass on {0} found {1} solution(s). At time: {2}", new Object[] {currentValues.values(), Integer.valueOf(solutionsFound), Integer.valueOf((int)((System.currentTimeMillis()-startTime)/1000))});
		        }
		        
		        // Find another starting point
				solver = IncrementalSolver.solver(getOptions());
				constraints = constraints.and(currentValues.exclusionConstraint());
				exclusionConstraints = exclusionConstraints.and(currentValues.exclusionConstraint());
				
				solution = solver.solve(constraints, problem.getBounds());
				incrementStats(solution, problem, constraints, false, null);
				
				//count this step but first go to new index because it's a new base point
				//counter.nextIndex();
				//counter.countStep();
            }

            return exclusionConstraints;
        }
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
}
