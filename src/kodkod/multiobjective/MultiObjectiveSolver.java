package kodkod.multiobjective;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import kodkod.ast.Formula;
import kodkod.engine.AbortedException;
import kodkod.engine.KodkodSolver;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.config.Options;
import kodkod.engine.fol2sat.HigherOrderDeclException;
import kodkod.engine.fol2sat.UnboundLeafException;
import kodkod.instance.Bounds;
import kodkod.multiobjective.algorithms.CheckpointedGuidedImprovementAlgorithm;
import kodkod.multiobjective.algorithms.IncrementalGuidedImprovementAlgorithm;
import kodkod.multiobjective.algorithms.PartitionedGuidedImprovementAlgorithm;
import kodkod.multiobjective.algorithms.MultiObjectiveAlgorithm;
import kodkod.multiobjective.concurrency.BlockingSolutionIterator;
import kodkod.multiobjective.concurrency.SolutionNotifier;
import kodkod.multiobjective.concurrency.TranslatingBlockingQueueSolutionNotifier;
import kodkod.multiobjective.statistics.Stats;
import kodkod.multiobjective.statistics.StepCounter;

public final class MultiObjectiveSolver implements KodkodSolver {

	final SolutionNotifier solutionNotifier;
	final BlockingSolutionIterator solutionIterator;
	final BlockingQueue<Solution> solutionQueue;
	final MultiObjectiveOptions options;
  
  MultiObjectiveAlgorithm algorithm;

  // Solver used for non-optimization problems.
  final Solver kodkodSolver;

	public MultiObjectiveSolver() {
    this(new MultiObjectiveOptions());
	}

  public MultiObjectiveSolver(MultiObjectiveOptions options) {
    this.options = options;
		solutionQueue = new LinkedBlockingQueue<Solution>();
		solutionIterator = new BlockingSolutionIterator(solutionQueue);
		solutionNotifier = new TranslatingBlockingQueueSolutionNotifier(solutionQueue);

    kodkodSolver = new Solver(options.getKodkodOptions());
  }
	
	public StepCounter getCountCallsOnEachMovementToParetoFront(){
		return algorithm.getCountCallsOnEachMovementToParetoFront();
	}
	
	public MultiObjectiveOptions multiObjectiveOptions() {
		return options;
	}
	
  @Override
	public Options options() {
		return options.getKodkodOptions();
	}

  @Override
  public Solution solve(Formula formula, Bounds bounds)
      throws HigherOrderDeclException, UnboundLeafException,
      AbortedException {

    return kodkodSolver.solve(formula, bounds);
  }

  @Override
	public void free() {
    kodkodSolver.free();
	}
	
	public Iterator<Solution> solveAll(final Formula formula, final Bounds bounds, final SortedSet<Objective>objectives ) 
			throws HigherOrderDeclException, UnboundLeafException, AbortedException {
		if (objectives != null) {
			final MultiObjectiveProblem problem = new MultiObjectiveProblem(bounds, formula, objectives);
      algorithm = options.getAlgorithm().instance(options);

			Thread solverThread = new Thread(new Runnable() {
				public void run() {
					algorithm.multiObjectiveSolve(problem, solutionNotifier);
				}
			});
			solverThread.start();
			
			return solutionIterator;
		} else {
      return kodkodSolver.solveAll(formula, bounds);
    }
	}
	
	public Stats getStats() {
		return algorithm.getStats();
	}

  public Solver getKodkodSolver() {
    return kodkodSolver;
  }
}
