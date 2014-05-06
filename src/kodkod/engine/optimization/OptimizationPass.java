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
 * Interface that must be implemented by optimization passes.
 * Optimization passes are responsible for transforming problems specified by a Formula and Bounds
 * into another problem that is equivalent to the original problem and then translating solutions
 * to the transformed problem back into solutions to the original problem.
 *
 * To allow use with incremental solving the transform method can be called multiple times to further
 * refine the problem similar to the IncrementalSolver, in such case only new formulas and bounds
 * will be returned rather than the entire transformed problem.
 *
 * @author Chris Kleynhans
 */
public interface OptimizationPass {
    public class FormulaWithBounds {
        private final Formula formula;
        private final Bounds bounds;

        public FormulaWithBounds(Formula f, Bounds b) {
            this.formula = f;
            this.bounds = b;
        }

        public Formula formula() { return formula; }
        public Bounds bounds() { return bounds; }
    }

    /**
     *  Transforms the specified formula and bounds with respect to the previously transformed
     *  formulas and bounds (if any) and returns the transformed formula and bounds.
     */
    FormulaWithBounds transform(FormulaWithBounds input);

    /**
     * Translates a solution on the transormed problem into a solution on the original problem.
     */
    Solution translate(Solution solution);
}