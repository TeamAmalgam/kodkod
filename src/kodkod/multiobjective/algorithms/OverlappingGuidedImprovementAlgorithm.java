package kodkod.multiobjective.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import kodkod.ast.Formula;
import kodkod.engine.IncrementalSolver;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.instance.Bounds;
import kodkod.multiobjective.MetricPoint;
import kodkod.multiobjective.MultiObjectiveOptions;
import kodkod.multiobjective.MultiObjectiveProblem;
import kodkod.multiobjective.concurrency.SolutionDeduplicator;
import kodkod.multiobjective.concurrency.SolutionNotifier;
import kodkod.multiobjective.statistics.StepCounter;

public class OverlappingGuidedImprovementAlgorithm extends MultiObjectiveAlgorithm{

    public OverlappingGuidedImprovementAlgorithm(String desc, MultiObjectiveOptions options) {
        super(desc, options, Logger.getLogger(IncrementalGuidedImprovementAlgorithm.class.toString()));

        magnifyingGlassSolverPool = new LinkedList<Solver>();
        magnifyingGlassSolverPool.add(getSolver());
        paretoPointDeduplicator = new SolutionDeduplicator();
    }

    private final Queue<Solver> magnifyingGlassSolverPool;
    private final SolutionDeduplicator paretoPointDeduplicator;

    private Solver GetMagnifyingGlassSolver() {
        synchronized (magnifyingGlassSolverPool) {
            if( magnifyingGlassSolverPool.size() == 0 ) {
                return new Solver(options.getKodkodOptions());
            } else {
                return magnifyingGlassSolverPool.remove();
            }
        }
    }

    private void PutBackMagnifyingGlassSolver(Solver solver) {
        synchronized (magnifyingGlassSolverPool) {
            magnifyingGlassSolverPool.add(solver);
        }
    }

    @Override
    public void multiObjectiveSolve(final MultiObjectiveProblem problem, final SolutionNotifier notifier) {

        // set the bit width
        setBitWidth(problem.getBitWidth());

        // for the evaluation we need a step counter
        this.counter = new StepCounter();

        int numberOfThreads = Math.min(8, Runtime.getRuntime().availableProcessors());

        final ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        final ConcurrentLinkedQueue<Future<?>> waitQueue = new ConcurrentLinkedQueue<Future<?>>();

        for( int i=0; i<numberOfThreads; i++ ) {
            waitQueue.add( executorService.submit( new SolverSubtask(problem, notifier, executorService, waitQueue) ) );
        }

        try {
            // There is only one consumer, so if the following condition is satisfied, it is guaranteed to be non-empty.
            while( !waitQueue.isEmpty() )
                waitQueue.remove().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException();
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

        logger.log(Level.FINE, "All Pareto points found. At time: {0}", Integer.valueOf((int)(System.currentTimeMillis()-startTime)/1000));

        executorService.shutdown();
        end(notifier);
        debugWriteStatistics();
    }

    private class MagnifyingGlassSubtask implements Runnable {
        private final Formula formula;
        private final Bounds bounds;
        private final MetricPoint metricPoint;
        private final SolutionNotifier notifier;

        public MagnifyingGlassSubtask(final Formula formula, final Bounds bounds, final MetricPoint metricPoint, final SolutionNotifier notifier) {
            this.formula = formula;
            this.bounds = bounds;
            this.metricPoint = metricPoint;
            this.notifier = notifier;
        }

        @Override
        public void run() {

            Solver solverToUse = GetMagnifyingGlassSolver();
            int solutionsFound = magnifier(formula, bounds, metricPoint, notifier, solverToUse);
            PutBackMagnifyingGlassSolver(solverToUse);

            logger.log(Level.FINE, "Magnifying glass found {0} solution(s). At time: {1}", new Object[] {Integer.valueOf(solutionsFound), Integer.valueOf((int)((System.currentTimeMillis()-startTime)/1000))});
        }
    }

    private class SolverSubtask implements Runnable {

        private final MultiObjectiveProblem problem;
        private final SolutionNotifier notifier;
        private final ExecutorService executorService;
        private final Queue<Future<?>> waitQueue;

        SolverSubtask(final MultiObjectiveProblem problem, final SolutionNotifier notifier, final ExecutorService executorService, final Queue<Future<?>> waitQueue) {
            this.problem = problem;
            this.notifier = notifier;
            this.executorService = executorService;
            this.waitQueue = waitQueue;
        }

        @Override
        public void run() {
            IncrementalSolver solver = IncrementalSolver.solver(getOptions());

            final List<Formula> exclusionConstraints = new ArrayList<Formula>();
            exclusionConstraints.add(problem.getConstraints());

            // Throw a dart and get a starting point.
            Formula constraint = Formula.and(exclusionConstraints);
            Solution solution = solver.solve(constraint, problem.getBounds());

            incrementStats(solution, problem, constraint, true, null);
            solveFirstStats(solution);

            // While the current solution is satisfiable try to find a better one.
            while (isSat(solution)) {
                MetricPoint currentValues = null;
                Solution previousSolution = null;

                // Work our way up to the pareto front.
                while (isSat(solution)) {
                    currentValues = MetricPoint.measure(solution, problem.getObjectives(), getOptions());
                    logger.log(Level.FINE, "Found a solution. At time: {0}, Improving on {1}", new Object[] { Integer.valueOf((int)((System.currentTimeMillis()-startTime)/1000)),  currentValues.values() });

                    final Formula improvementConstraints = currentValues.parametrizedImprovementConstraints();

                    previousSolution = solution;
                    solution = solver.solve(improvementConstraints, new Bounds(problem.getBounds().universe()));
                    incrementStats(solution, problem, improvementConstraints, false, improvementConstraints);

                    //counter.countStep();
                }

                // We can't find anything better, so the previous solution is a pareto point.
                foundParetoPoint(currentValues);

                // Free the solver's resources since we will be creating a new solver.
                solver.free();

                if( paretoPointDeduplicator.pushNewSolution(currentValues.values())) {
                    if (!options.allSolutionsPerPoint()) {
                        tell(notifier, previousSolution, currentValues);
                    } else {
                        // magnifying glass
                        final Collection<Formula> assignmentsConstraints = currentValues.assignmentConstraints();
                        assignmentsConstraints.add(problem.getConstraints());
                        waitQueue.add( executorService.submit(new MagnifyingGlassSubtask(Formula.and(assignmentsConstraints), problem.getBounds(), currentValues, notifier)));
                        // the values in currentValues MUST NOT BE MODIFIED after this point.
                    }
                }

                // Find another starting point.
                solver = IncrementalSolver.solver(getOptions());
                exclusionConstraints.add(currentValues.exclusionConstraint());

                constraint = Formula.and(exclusionConstraints);
                solution = solver.solve(constraint, problem.getBounds());
                incrementStats(solution, problem, constraint, false, null);

                //count this step but first go to new index because it's a new base point
                //counter.nextIndex();
                //counter.countStep();
            }
        }

    }
}
