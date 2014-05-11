package kodkod.multiobjective.algorithms;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import kodkod.ast.Formula;
import kodkod.engine.CheckpointedSolver;
import kodkod.engine.IncrementalSolver;
import kodkod.engine.Solution;
import kodkod.instance.Bounds;
import kodkod.multiobjective.MetricPoint;
import kodkod.multiobjective.MultiObjectiveOptions;
import kodkod.multiobjective.MultiObjectiveProblem;
import kodkod.multiobjective.Objective;
import kodkod.multiobjective.concurrency.SolutionNotifier;
import kodkod.multiobjective.statistics.StatKey;
import kodkod.multiobjective.statistics.StepCounter;

public class PartitionedGuidedImprovementAlgorithm extends MultiObjectiveAlgorithm {

    private CountDownLatch doneSignal;
    private ExecutorService threadPool;
    private SolutionNotifier notifier;

    public PartitionedGuidedImprovementAlgorithm(String desc, MultiObjectiveOptions options) {
        super(desc, options, Logger.getLogger(PartitionedGuidedImprovementAlgorithm.class.toString()));
    }

    @Override
    public void multiObjectiveSolveImpl(MultiObjectiveProblem problem, SolutionNotifier solutionNotifier) {
        notifier = solutionNotifier;

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

        // Work up to a single Pareto point
        // If there is only one objective, this is the only Pareto point
        // For more objectives, we use this Pareto point to split the search space
        MetricPoint firstParetoPoint = findFirstParetoPoint(problem, exclusionConstraints);

        // Start the subtasks and block until they complete
        // If there is only one objective, the method simply returns
        startSubtasks(problem, exclusionConstraints, firstParetoPoint);

        logger.log(Level.FINE, "All Pareto points found. At time: {0}", Integer.valueOf((int)(System.currentTimeMillis()-startTime)/1000));

        // Re-enable MetricPoint logging
        metricPointLogger.setLevel(metricPointLevel);

        end(notifier);
        debugWriteStatistics();
    }

    private void foundParetoPoint(int taskID, MetricPoint metricpoint) {
        getStats().increment(StatKey.OPTIMAL_METRIC_POINTS);
        logger.log(Level.FINE, "Task {0}: Found Pareto point with values: {1}", new Object[] { taskID, metricpoint.values() });
    }

    // Returns the Pareto point we just found
    private MetricPoint findFirstParetoPoint(MultiObjectiveProblem problem, List<Formula> exclusionConstraints) {
        // Use an incremental solver because we don't roll back here
        // The incremental solver uses less memory than the checkpointed solver
        IncrementalSolver solver = IncrementalSolver.solver(getOptions());
        Solution solution = solver.solve(Formula.and(exclusionConstraints), problem.getBounds());

        MetricPoint currentValues = null;
        Solution previousSolution = null;
        while (isSat(solution)) {
            currentValues = MetricPoint.measure(solution, problem.getObjectives(), getOptions());
            logger.log(Level.FINE, "Found a solution. At time: {0}, Improving on {1}", new Object[] { Integer.valueOf((int)((System.currentTimeMillis()-startTime)/1000)),  currentValues.values() });
            Formula improvementConstraints = currentValues.parametrizedImprovementConstraints();
            previousSolution = solution;
            solution = solver.solve(improvementConstraints, new Bounds(problem.getBounds().universe()));
            incrementStats(solution, problem, improvementConstraints, false, improvementConstraints);
        }
        foundParetoPoint(currentValues);
        solver.free();

        if (!options.allSolutionsPerPoint()) {
            // no magnifying glass
            // previous solution was on the Pareto front, so report it
            tell(notifier, previousSolution, currentValues);
        } else {
            // magnifying glass
            final Collection<Formula> assignmentsConstraints = currentValues.assignmentConstraints();
            assignmentsConstraints.add(problem.getConstraints());
            int solutionsFound = magnifier(Formula.and(assignmentsConstraints), problem.getBounds(), currentValues, notifier);
            logger.log(Level.FINE, "Magnifying glass on {0} found {1} solution(s). At time: {2}", new Object[] {currentValues.values(), Integer.valueOf(solutionsFound), Integer.valueOf((int)((System.currentTimeMillis()-startTime)/1000))});
        }
        exclusionConstraints.add(currentValues.exclusionConstraint());

        return currentValues;
    }

    // This method blocks
    // It creates the subtasks and blocks until they complete
    // If there is only one objective, the method returns
    private void startSubtasks(MultiObjectiveProblem problem, List<Formula> exclusionConstraints, MetricPoint firstParetoPoint) {
        if (problem.getObjectives().size() == 1) {
            return;
        }

        // Create the thread pool
        // Number of threads is MIN(user value, # cores)
        // TODO: Make "user value" configurable
        int poolSize = Math.min(8, Runtime.getRuntime().availableProcessors());
        threadPool = Executors.newFixedThreadPool(poolSize);
        logger.log(Level.FINE, "Starting a thread pool with {0} threads", new Object[] { poolSize });

        // Create all the tasks up front, adding them to an array
        logger.log(Level.FINE, "Partitioning the problem space");
        List<PartitionSearcherTask> tasks = new ArrayList<PartitionSearcherTask>();
        // Task at index 0 doesn't exist; it's in an excluded region
        // We only add null so all the tasks are added at the right index
        tasks.add(null);

        // Now we can split the search space based on the Pareto point and create new tasks
        // For n metrics, we want all combinations of m_i <= M_i and m_i >= M_i where M_i is the current value
        // To iterate over this, we map the bit_i of a bitset to metric_i
        // Note that bit_0 is the least significant bit
        // We skip 0 (the partition that is already dominated) and 2^n - 1 (the partition where we didn't find any solutions)
        // Give each task a CountDownLatch so this thread can wait until all 2^n - 2 tasks have completed
        int numObjectives = problem.getObjectives().size();

        // Convert the objective set into an array so we have a deterministic order
        // for mapping the bits in the bitset to objectives.
        Objective[] objective_order = problem.getObjectives().toArray(new Objective[0]);

        int maxMapping = (int) Math.pow(2, numObjectives) - 1;
        doneSignal = new CountDownLatch(maxMapping - 1);
        for (int mapping = 1; mapping < maxMapping; mapping++) {
            BitSet bitSet = BitSet.valueOf(new long[] { mapping });
            tasks.add(new PartitionSearcherTask(mapping, problem, exclusionConstraints, firstParetoPoint.partitionConstraints(bitSet, objective_order)));
        }

        // Link up the dependencies
        for (int mapping = 1; mapping < maxMapping; mapping++) {
            PartitionSearcherTask task = tasks.get(mapping);
            task.linkDependencies(tasks);
        }

        // Submit starting tasks (the ones without dependencies) to the thread pool
        // Starting tasks are mapped to the ints with exactly one 0 bit
        // So iterate over the bitset and clear one bit at a time
        for (int bitIndex = 0; bitIndex < numObjectives; bitIndex++) {
            BitSet bitSet = BitSet.valueOf(new long[] { maxMapping });
            bitSet.clear(bitIndex);
            int taskIndex = (int) bitSet.toLongArray()[0];
            threadPool.submit(tasks.get(taskIndex));
        }

        // Wait for all tasks to complete before shutting down the pool
        try {
            doneSignal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        threadPool.shutdown();
    }

    private class PartitionSearcherTask implements Runnable {

        private final int taskID;
        private final MultiObjectiveProblem problem;
        private Formula partitionConstraints;

        private Set<Formula> exclusionConstraints = new HashSet<Formula>();
        private List<PartitionSearcherTask> children = new ArrayList<PartitionSearcherTask>();
        private Map<Integer,Boolean> parentDoneStatus = new HashMap<Integer,Boolean>();

        private boolean started = false;
        private boolean submitted = false;

        public PartitionSearcherTask(int taskID, MultiObjectiveProblem problem, List<Formula> exclusionConstraints, Formula partitionConstraints) {
            this.taskID = taskID;
            this.problem = problem;
            this.partitionConstraints = partitionConstraints;
            this.exclusionConstraints.addAll(exclusionConstraints);
        }

        // Each task is represented by a binary string
        // "Adjacent" tasks have only one bit changed
        // If the adjacent task has one more 1 than this task, then it's a parent (this task depends on it)
        // If the adjacent task has one more 0 than this task, then it's a child (depends on this task)
        public void linkDependencies(List<PartitionSearcherTask> tasks) {
            if (started) {
                throw new IllegalStateException("Cannot link dependencies after task has already started.");
            }

            int numObjectives = problem.getObjectives().size();

            // Iterate over adjacent tasks
            for (int bitIndex = 0; bitIndex < numObjectives; bitIndex++) {
                BitSet neighbour = BitSet.valueOf(new long[] { taskID });

                // If this current bit is a 1, set it to a 0 to get the child
                // If this current bit is a 0, set it to a 1 to get the parent
                if (neighbour.get(bitIndex)) {
                    neighbour.clear(bitIndex);
                    // Skip over task of all bits are cleared (neighbour = 0) since that task doesn't exist
                    if (neighbour.cardinality() == 0) {
                        continue;
                    }
                    int childIndex = (int) neighbour.toLongArray()[0];
                    children.add(tasks.get(childIndex));
                } else {
                    neighbour.set(bitIndex);
                    // Skip over task if all bits are set (neighbour = 2^n - 1) since that task doesn't exist
                    if (neighbour.cardinality() == numObjectives) {
                        continue;
                    }
                    int parentIndex = (int) neighbour.toLongArray()[0];
                    parentDoneStatus.put(parentIndex, false);
                }
            }
        }

        // Called by this task's dependencies when the dependency has completed
        private synchronized void notifyDone(int parentID, Collection<Formula> exclusionConstraints) {
            if (started) {
                throw new IllegalStateException("Cannot update exclusion constraints after task has already started.");
            }

            // Take in the new constraints
            this.exclusionConstraints.addAll(exclusionConstraints);

            // Mark this dependency as completed
            parentDoneStatus.put(parentID, true);

            // If the parentDoneStatus map has no false values, then all dependencies are done
            // If this task has not been submitted, then submit it
            if (!parentDoneStatus.containsValue(false) && !submitted) {
                threadPool.submit(this);
                submitted = true;
            } else if (!parentDoneStatus.containsValue(false) && submitted) {
                // If we reach this point, it means the task has been submitted before it was notified
                // by a parent, which shouldn't be possible
                throw new RuntimeException("Task has already been submitted.");
            }
        }

        @Override
        public void run() {
          try {
            logger.log(Level.FINE, "Starting Task {0}. At time: {1}", new Object[] { taskID, Integer.valueOf((int)((System.currentTimeMillis()-startTime)/1000)) });
            started = true;

            // Run the regular algorithm within this partition
            CheckpointedSolver solver = CheckpointedSolver.solver(getOptions());
            Formula constraint = Formula.and(exclusionConstraints).and(partitionConstraints);
            Solution solution = solver.solve(constraint, problem.getBounds());
            incrementStats(solution, problem, constraint, false, constraint);
            solver.checkpoint();

            // Work up to the Pareto front
            MetricPoint currentValues = null;
            Solution previousSolution = null;
            while (isSat(solution)) {
                while (isSat(solution)) {
                    currentValues = MetricPoint.measure(solution, problem.getObjectives(), getOptions());
                    logger.log(Level.FINE, "Found a solution. At time: {0}, Improving on {1}", new Object[] { Integer.valueOf((int)((System.currentTimeMillis()-startTime)/1000)),  currentValues.values() });

                    Formula improvementConstraints = currentValues.parametrizedImprovementConstraints();
                    previousSolution = solution;
                    solution = solver.solve(improvementConstraints, new Bounds(problem.getBounds().universe()));
                    incrementStats(solution, problem, improvementConstraints, false, improvementConstraints);
                }
                foundParetoPoint(taskID, currentValues);

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

                solver.rollback();
                exclusionConstraints.add(currentValues.exclusionConstraint());
                solution = solver.solve(currentValues.exclusionConstraint(), new Bounds(problem.getBounds().universe()));
                incrementStats(solution, problem, constraint, false, null);
                solver.checkpoint();
            }

            logger.log(Level.FINE, "Finishing Task {0}. At time: {1}", new Object[] { taskID, Integer.valueOf((int)((System.currentTimeMillis()-startTime)/1000)) });

            // Done searching in this partition, so pass this dependency to children and notify them
            // Child will schedule itself if it's done
            for (PartitionSearcherTask child : children) {
                child.notifyDone(taskID, exclusionConstraints);
            }

            // Signal that this task has completed
            doneSignal.countDown();
          } catch (Exception e) {
            logger.log(Level.SEVERE, "Task failed.");
            logger.log(Level.SEVERE, e.toString());
            throw new RuntimeException(e);
          }
        }
    }
}
