package kodkod.engine.satlab;

import kodkod.engine.TimeoutException;
import kodkod.util.IntSet;

/**
 * Provides an interface to a SAT solver.
 * 
 * @specfield literals: set int
 * @specfield clauses: set seq[literals]
 * @invariant all i: [2..) | i in literals => i-1 in literals
 * @author Emina Torlak
 */
public interface SATSolver {

	/**
	 * Returns the size of this solver's vocabulary.
	 * @return #this.literals
	 */
	public abstract int numberOfVariables();
	
	/**
	 * Returns the number of clauses added to the 
	 * solver so far.
	 * @return #this.clauses
	 */
	public abstract int numberOfClauses();
	
	/**
	 * Returns the maximum amount of time
	 * that this.solver will spend trying
	 * to find a solution. 
	 * @return the timeout (in seconds)
	 */
	public abstract int timeout();
	
	/**
	 * Sets the timeout of this solver to the specified
	 * number of seconds.  If a solution is 
	 * not found in the given timeframe, the solver
	 * terminates with a TimeoutException.
	 * @effects sets the timeout to the given number of seconds
	 * @throws IllegalArgumentException - seconds < 0
	 */
	public abstract void setTimeout(int seconds);
	
	/**
	 * Adds the specified number of new variables
	 * to the solver's vocabulary.
	 * @effects this.literals' = [1..#this.literals + numVars]
	 * @throws IllegalArgumentException - numVars < 0
	 */
	public abstract void addVariables(int numVars);
	
	/**
	 * Adds the specified sequence of literals to this.clauses.
	 * No reference to the specified array is kept, so it can
	 * be reused. 
	 * @effects this.clauses' = this.clauses + lits
	 * @throws NullPointerException - lits = null
	 * @throws IllegalArgumentException - some i: [0..lits.length) | |lits[0]| > #this.literals 
	 */
	public abstract void addClause(int[] lits);
	
	/**
	 * Returns true if there is a satisfying assignment for this.clauses.
	 * Otherwise returns false.  If this.clauses are satisfiable, the 
	 * satisfying assignment can be obtained by calling {@link #variablesThatAre(boolean, int, int)}
	 * or {@link #valueOf(int) }.
	 * @return true if this.clauses are satisfiable; otherwise false.
	 * @throws TimeoutException - the solver could not determine
	 * the satisfiability of the problem within this.timeout() seconds.
	 */
	public abstract boolean solve() throws TimeoutException;
	
	/**
	 * Returns the literals in the range [start..end] that 
	 * have been set to the given boolean value by the most recent call to {@link #solve() }.  
	 * @return the true literals between start and end
	 * @throws IllegalArgumentException - start > end || [start..end] !in this.literals
	 * @throws IllegalStateException - {@link #solve() } has not been called or the 
	 * outcome of the last call was not <code>true</code>.
	 */
	public abstract IntSet variablesThatAre(boolean truthValue, int start, int end);
	
	/**
	 * Returns the truth value assigned to the given literal by the
	 * most recent call to {@link #solve()}.  If the value
	 * of the given literal did not affect the result of the
	 * last solution, null is returned.
	 * @return the value of the given literal
	 * @throws IllegalArgumentException - literal !in this.literals
	 * @throws IllegalStateException - {@link #solve() } has not been called or the 
	 * outcome of the last call was not <code>true</code>.
	 */
	public abstract Boolean valueOf(int literal);
}
