/*
 * Kodkod -- Copyright (c) 2005-present, Emina Torlak
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package kodkod.engine.optimization;

import kodkod.ast.Formula;
import kodkod.engine.Solution;
import kodkod.instance.Bounds;

/**
 * Composite that runs a number of optimization passes one after another.
 */
public class OptimizationSeries implements OptimizationPass {

    private OptimizationPass[] passes;

    OptimizationSeries(OptimizationPass[] passes) {
        this.passes = passes;
    }

    /**
     *  Transforms the specified formula and bounds with respect to the previously transformed
     *  formulas and bounds (if any) and returns the transformed formula and bounds.
     */
    public FormulaWithBounds transform(FormulaWithBounds input) {
        FormulaWithBounds current = input;

        for (int i = 0; i < passes.length; i += 1) {
            current = passes[i].transform(current);
        }

        return current;
    }

    /**
     * Translates a solution on the transormed problem into a solution on the original problem.
     */
    public Solution translate(Solution solution) {
        Solution current = solution;

        for (int i = passes.length - 1; i >= 0; i -= 1) {
            current = passes[i].translate(current);
        }

        return current;
    }
}