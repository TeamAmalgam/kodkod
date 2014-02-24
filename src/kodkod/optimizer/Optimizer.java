package kodkod.optimizer;

import kodkod.ast.Formula;
import kodkod.engine.Solution;
import kodkod.instance.Bounds;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * A series of Optimization passes.
 *
 * @see kodkod.optimizer.OptimizationPass
 */
public final class Optimizer implements OptimizationPass {

    private static OptimizationPass[] DefaultPasses = new OptimizationPass[]{

    };

    private List<OptimizationPass> passes;

    public Optimizer() {
        this(Arrays.asList(DefaultPasses));
    }

    public Optimizer(List<OptimizationPass> passes) {
        this.passes = passes;
    }

    public FormulaWithBounds optimize(FormulaWithBounds fb) {
        FormulaWithBounds current_fb = fb;
        ListIterator<OptimizationPass> iterator = passes.listIterator();
        while (iterator.hasNext()) {
            OptimizationPass pass = iterator.next();
            current_fb = pass.optimize(current_fb);
        }
        return current_fb;
    }

    public FormulaWithBounds optimizeIncremental(FormulaWithBounds fb) {
        FormulaWithBounds current_fb = fb;
        ListIterator<OptimizationPass> iterator = passes.listIterator();
        while (iterator.hasNext()) {
            OptimizationPass pass = iterator.next();
            current_fb = pass.optimizeIncremental(current_fb);
        }
        return current_fb;
    }

    public Solution translateSolution(Solution s) {
        Solution current_s = s;
        ListIterator<OptimizationPass> iterator = passes.listIterator(passes.size() - 1);
        while (iterator.hasPrevious()) {
            OptimizationPass pass = iterator.previous();
            current_s = pass.translateSolution(current_s);
        }
        return current_s;
    }

}