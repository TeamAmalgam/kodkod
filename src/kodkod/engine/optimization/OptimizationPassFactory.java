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

public abstract class OptimizationPassFactory {
    public abstract OptimizationPass create();

    public static OptimizationPassFactory NONE = new OptimizationPassFactory() {
        public OptimizationPass create() {
            return new OptimizationSeries(new OptimizationPass[]{});
        }
    };

    public static OptimizationPassFactory optimizationSeries(OptimizationPassFactory... passes) {
        final OptimizationPassFactory[] passesToCreate = passes;

        return new OptimizationPassFactory() {

            public OptimizationPass create() {
                OptimizationPass[] materializedPasses = new OptimizationPass[passesToCreate.length];

                for (int i =  0; i < passesToCreate.length; i += 1) {
                    materializedPasses[i] = passesToCreate[i].create();
                }

                return new OptimizationSeries(materializedPasses);
            }

        };
    }
}