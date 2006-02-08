package kodkod.engine;

import kodkod.engine.satlab.MiniSAT;

import org.sat4j.minisat.SolverFactory;

/**
 * This class stores information about various
 * user-level translation options.  It can be
 * used to choose the SAT solver, set skolemization
 * depth, etc.
 * 
 * @specfield solver: SATSolver // SAT solver to use
 * @specfield seed: long // random seed to be used by the SAT solver
 * @specfield timeout:  int // SAT solver timeout, in seconds
 * @specfield symmetryBreaking: int // the amount of symmetry breaking to perform
 * @specfield skolemize: boolean // skolemize existential quantifiers?
 * @specfield flatten: boolean // eliminate extraneous intermediate variables?
 * @specfield logEncodeFunctions: boolean // use a compact encoding for functions?
 * @specfield trackExpressionVars: boolean // keep track of intermediate variables assigned to expressions?
 * @author Emina Torlak
 */
public final class Options {
	private SATSolver solver = SATSolver.DefaultSAT4J;
	private long seed = 0L;
	private int timeout = Integer.MAX_VALUE;
	private int symmetryBreaking = 20;
	private boolean skolemize = true;
	private boolean flatten = true;
	private boolean logEncodeFunctions = true;
	private boolean trackExpressionVars = false;
	
	
	/**
	 * Constructs an Options object initalized with 
	 * default values.
	 * @effects this.solver' = SATSolver.DefaultSAT4J
	 *          this.seed' = 0
	 *          this.timeout' = Integer.MAX_VALUE 
	 *          this.symmetryBreaking' = 20
	 *          this.skolemize' = true
	 *          this.flatten' = true
	 *          this.logEncodeFunctions' = true
	 *          this.trackExpressionVars' = false
	 */
	public Options() {}
	
	/**
	 * Constructs an Options object using the given
	 * value for the solver option and default values
	 * for other options.
	 * @effects this.solver' = solver
	 *          this.seed' = 0
	 *          this.timeout' = Integer.MAX_VALUE
	 *          this.symmetryBreaking' = 20
	 *          this.skolemize' = true
	 *          this.flatten' = true
	 *          this.logEncodeFunctions' = true
	 *          this.trackExpressionVars' = false
	 * @throws NullPointerException - solver = null
	 */
	public Options(SATSolver solver) {
		this();
		setSolver(solver);
	}
	
	/**
	 * Returns the value of the solver options.
	 * The default is SATSolver.DefaultSAT4J.
	 * @return this.solver
	 */
	public SATSolver solver() {
		return solver;
	}
	
	/**
	 * Sets the solver option to the given value.
	 * @effects this.solver' = solver
	 * @throws NullPointerException - solver = null
	 */
	public void setSolver(SATSolver solver) {
		if (solver==null)
			throw new NullPointerException();
		this.solver = solver;
	}
	
	/**
	 * Returns the value of the random seed used
	 * by the solver.  The default is 0 for no-randomness.
	 * @return this.seed
	 */
	public long seed() {
		return seed;
	}
	
	/**
	 * Sets the seed option to the given value.
	 * @effects this.seed' = seed
	 */
	public void setSeed(long seed) {
		this.seed = seed;
	}
	
	/**
	 * Returns the number of seconds that the 
	 * SAT solver is allowed to spend on any given problem.
	 * The default is Integer.MAX_VALUE:  the 
	 * SAT solver is allowed to run as long as
	 * necessary to determine the satisfiability
	 * of a formula.
	 * @return this.timeout
	 */
	public int timeout() {
		return timeout;
	}
	
	/**
	 * @throws IllegalArgumentException - arg !in [min..max]
	 */
	private void checkRange(int arg, int min, int max) {
		if (arg < min || arg > max)
			throw new IllegalArgumentException(arg + " !in [" + min + ".." + max + "]");
	}
	
	/**
	 * Sets the timeout option to the given value.
	 * @effects  this.timeout' = timeout
	 * @throws IllegalArgumentException - timeout !in [0..Integer.MAX_VALUE]
	 */
	public void setTimeout(int timeout) {
		checkRange(timeout, 0, Integer.MAX_VALUE);
		this.timeout = timeout;
	}
	
	/**
	 * Returns the value of the flattening flag, which specifies whether
	 * to eliminate extraneous intermediate variables.  The flag is true by default.  
	 * Flattening must be off if the tracking of expression variables is enabled.  
	 * @return this.flatten
	 */
	public boolean flatten() {
		return flatten;
	}
	
	/**
	 * Sets the flattening option to the given value.
	 * @effects this.flatten' = flatten
	 * @throws IllegalArgumentException - this.trackExpressionVars && flatten
	 */
	public void setFlatten(boolean flatten) {
		if (trackExpressionVars && flatten)
			throw new IllegalStateException("trackExpressionVars enabled:  flattening must be off.");
		this.flatten = flatten;
	}
	
	/**
	 * Returns the 'amount' of symmetry breaking to perform.
	 * If a non-symmetric solver is chosen for this.solver,
	 * this value controls the maximum length of the generated
	 * lex-leader symmetry breaking predicate.  If a symmetric
	 * solver is chosen, this value controls the amount of 
	 * symmetry information to pass to the solver.  (For example,
	 * if a formula has 10 relations on which symmetry can be broken,
	 * and the symmetryBreaking option is set to 5, then symmetry information
	 * will be computed for only 5 of the 10 relations.)  In general, 
	 * the higher this value, the more symmetries will be broken, and the 
	 * faster the formula will be solved.  But, setting the value too high 
	 * may have the opposite effect and slow down the solving.  The default
	 * value for this property is 20.  
	 * @return this.symmetryBreaking
	 */
	public int symmetryBreaking() {
		return symmetryBreaking;
	}
	
	/**
	 * Sets the symmetryBreaking option to the given value.
	 * @effects this.symmetryBreaking' = symmetryBreaking
	 * @throws IllegalArgumentException - symmetryBreaking !in [0..Integer.MAX_VALUE]
	 */
	public void setSymmetryBreaking(int symmetryBreaking) {
		checkRange(symmetryBreaking, 0, Integer.MAX_VALUE);
		this.symmetryBreaking = symmetryBreaking;
	}
	
	/**
	 * Returns the value of the skolemization flag, which
	 * controls whether or not existentially quantified variables are
	 * skolemized.  Skolemization is turned on by default.
	 * @return this.skolemize
	 */
	public boolean skolemize() {
		return skolemize;
	}
	
	/**
	 * Sets the skolemization flag to the given value.
	 * @effects this.skolemize = skolemize
	 */
	public void setSkolemize(boolean skolemize) {
		this.skolemize = skolemize;
	}
	
	/**
	 * Returns true if a compact encoding should be used for functions.
	 * The compact encoding uses N(log M) boolean variables to represent 
	 * a function whose domain and range contain up to N and M values, 
	 * respectively.  (The regular encoding uses N*M variables.)  Although
	 * the compact encoding reduces the number of boolean variables, it
	 * increases the number of clauses which may slow down the SAT solver.  
	 * The default value of this flag is <code>true</code>.
	 * @return this.logEncodeFunctions
	 */
	public boolean logEncodeFunctions() {
		return logEncodeFunctions;
	}
	
	/**
	 * Returns true if a mapping from expressions to boolean variables that
	 * represent them should be generated during translation.  This is useful
	 * for determining which expressions occur in the unsat core of an 
	 * unsatisfiable formula.  The flatten flag must be off whenever 
	 * this flag is enabled.  Expression tracking is off by default, since 
	 * it incurs a non-trivial memory overheaad.
	 * @return this.trackExpressionVars
	 */
	public boolean trackExpressionVars() {
		return trackExpressionVars;
	}
	
	/**
	 * Sets the value of the expression tracking flag.  If the 
	 * flag is turned on, flatten is automatically set to false.
	 * @effects this.trackExpressionVars' = trackExpressionVars &&
	 *          trackExpressionVars => this.flatten' = false
	 */
	public void setTrackExpressionVars(boolean trackExpressionVars) {
		if (trackExpressionVars)
			flatten = false;
		this.trackExpressionVars = trackExpressionVars;
	}
	
	/**
	 * Returns a string representation of this Options object.
	 * @return a string representation of this Options object.
	 */
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("Options:");
		b.append("\n  solver: ");
		b.append(solver);
		b.append("\n  seed: ");
		b.append(seed);
		b.append("\n timeout: ");
		b.append(timeout);
		b.append("\n flatten: ");
		b.append(flatten);
		b.append("\n symmetryBreaking: ");
		b.append(symmetryBreaking);
		b.append("\n skolemize: ");
		b.append(skolemize);
		b.append("\n logEncodeFunctions: ");
		b.append(logEncodeFunctions);
		b.append("\n trackExpressionVars: ");
		b.append(trackExpressionVars);
		return b.toString();
	}
	
	/**
	 * An enumeration of available SAT solvers.
	 */
	public static enum SATSolver {
		/**
		 * The default solver of the sat4j library.
		 * @see org.sat4j.core.ASolverFactory#defaultSolver()
		 */
		DefaultSAT4J { 
			public kodkod.engine.satlab.SATSolver instance() { 
				return new MiniSAT(SolverFactory.newMiniSAT2Heap()); 
			}
		},
		
		/**
		 * A solver that is suitable for solving many small instances of SAT problems.
		 * @see org.sat4j.core.ASolverFactory#lightSolver()
		 */
		LightSAT4J {
			public kodkod.engine.satlab.SATSolver instance() { 
				return new MiniSAT(SolverFactory.newMini3SAT()); 
			}
		},
		
		/**
		 * A "default" "minilearning" solver learning clauses of size
		 * smaller than 10 % of the total number of variables
		 * @see org.sat4j.minisat.SolverFactory#newMiniLearning()
		 */
		MiniLearning {
			public kodkod.engine.satlab.SATSolver instance() { 
				return new MiniSAT(SolverFactory.newMiniLearning()); 
			}
		},
		
		/**
		 * A SAT solver using First UIP clause generator, watched literals,
		 * VSIDS like heuristics learning only clauses having a great number
		 * of active variables, i.e. variables with an activity strictly
		 * greater than one.
		 * @see org.sat4j.minisat.SolverFactory#newActiveLearning()
		 */
		ActiveLearning {
			public kodkod.engine.satlab.SATSolver instance() { 
				return new MiniSAT(SolverFactory.newActiveLearning()); 
			}
		},
		
		/**
		 * MiniSAT with VSIDS heuristics, FirstUIP clause generator for
		 * backjumping but no learning.
		 * @see org.sat4j.minisat.SolverFactory#newBackjumping()
		 */
		Backjumping {
			public kodkod.engine.satlab.SATSolver instance() { 
				return new MiniSAT(SolverFactory.newBackjumping()); 
			}
		},
		
		/**
		 * MiniSAT with decision UIP clause generator.
		 * @see org.sat4j.minisat.SolverFactory#newRelsat()
		 */
		Relsat {
			public kodkod.engine.satlab.SATSolver instance() { 
				return new MiniSAT(SolverFactory.newRelsat()); 
			}
		};
		
		/**
		 * The enum also doubles as a factory for obtaining
		 * solver instances.
		 * @return an actual SATSolver that corresponds to this
		 * instance of the enum.
		 */
		public abstract kodkod.engine.satlab.SATSolver instance();
		
		/**
		 * Returns true if the SATSolver represented by this
		 * implements the SymmetryDrivenSolver interface.
		 * @return true if the SATSolver represented by this
		 * implements the SymmetryDrivenSolver interface.
		 */
		public boolean isSymmetryDriven() {
			return false;
		}
		
		/**
		 * Returns true if the SATSolver represented by this
		 * implements the UnsatProvingSolver interface.
		 * @return true if the SATSolver represented by this
		 * implements the UnsatProvingSolver interface.
		 */
		public boolean canProveUnsatisfiability() {
			return false;
		}
		
	}

}
