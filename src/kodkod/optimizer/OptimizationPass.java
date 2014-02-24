package kodkod.optimizer;

import kodkod.ast.Formula;
import kodkod.engine.Solution;
import kodkod.instance.Bounds;

/**
 * A single pass in the formula/bounds optimization process.
 * Optimizers are initialized by calling optimize specifying the
 * initial formula and bounds.
 *
 * The optimize method will return a new formula that is an optimized
 * formulation of the formula. The optimized bounds can be obtained by
 * calling the getOptimizedBounds method.
 *
 * If incremental constraints are added to the problem, optimizeIncremental
 * must be called with the additional formula and any additional bounds 
 * to produce a formula with the optimizations.
 */
public interface OptimizationPass {

    public static final class FormulaWithBounds {
        private final Formula f;
        private final Bounds b;

        public FormulaWithBounds(Formula f, Bounds b) {
            this.f = f;
            this.b = b;
        }

        public Formula getFormula() {
            return f;
        }

        public Bounds getBounds() {
            return b;
        }
    }

    /**
     * This method should be called with the initial problem formula and bounds.
     * 
     * @return some f' : Formula | f' <=> f
     */
    public FormulaWithBounds optimize(FormulaWithBounds fb);

    /**
     * This method should be called with additional problem formula and bounds.
     *
     * @return some f' : Formula | previous_formulae' & f' <=> previous_formulae & f
     *
     * @throws IllegalStateException if optimize has not been called.
     */
    public FormulaWithBounds optimizeIncremental(FormulaWithBounds fb);

    /**
     * This method translates a solution under the optimized bounds into a solution
     * under the original bounds.
     *
     * @return solution under the original bounds.
     */
    public Solution translateSolution(Solution s);
}