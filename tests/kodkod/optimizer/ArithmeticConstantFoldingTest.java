package kodkod.optimizer;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;

import static kodkod.optimizer.OptimizationPass.FormulaWithBounds;
import kodkod.ast.*;
import kodkod.ast.operator.*;
import kodkod.engine.Solution;
import kodkod.instance.Bounds;
import kodkod.instance.Universe;

@RunWith(JUnit4.class)
public class ArithmeticConstantFoldingTest {

    private OptimizationPass optimization;

    @Before
    public void setUp() {
        optimization = new ArithmeticConstantFolding();
    }

    @Test
    public void simpleAddition() {
        Universe u = new Universe("dummy");
        Bounds b = new Bounds(u);
        Relation r = Relation.unary("DummyInt");

        Formula original_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(1).plus(IntConstant.constant(2)).toExpression());
        Formula expected_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(3).toExpression());

        FormulaWithBounds original = new FormulaWithBounds(original_f, b);
        FormulaWithBounds result = optimization.optimize(original);

        assertEquals("The bounds should be unchanged.", original.getBounds(), result.getBounds());
        assertEquals("The addition should be folded.", expected_f.toString(), result.getFormula().toString());
    }

    @Test
    public void simpleSubtraction() {
        Universe u = new Universe("dummy");
        Bounds b = new Bounds(u);
        Relation r = Relation.unary("DummyInt");

        Formula original_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(1).minus(IntConstant.constant(2)).toExpression());
        Formula expected_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(-1).toExpression());

        FormulaWithBounds original = new FormulaWithBounds(original_f, b);
        FormulaWithBounds result = optimization.optimize(original);

        assertEquals("The bounds should be unchanged.", original.getBounds(), result.getBounds());
        assertEquals("The subtraction should be folded.", expected_f.toString(), result.getFormula().toString());
    }

    @Test
    public void simpleMultiplication() {
        Universe u = new Universe("dummy");
        Bounds b = new Bounds(u);
        Relation r = Relation.unary("DummyInt");

        Formula original_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(2).multiply(IntConstant.constant(2)).toExpression());
        Formula expected_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(4).toExpression());

        FormulaWithBounds original = new FormulaWithBounds(original_f, b);
        FormulaWithBounds result = optimization.optimize(original);

        assertEquals("The bounds should be unchanged.", original.getBounds(), result.getBounds());
        assertEquals("The multiplication should be folded.", expected_f.toString(), result.getFormula().toString());
    }

    @Test
    public void simpleDivision() {
        Universe u = new Universe("dummy");
        Bounds b = new Bounds(u);
        Relation r = Relation.unary("DummyInt");

        Formula original_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(6).divide(IntConstant.constant(2)).toExpression());
        Formula expected_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(3).toExpression());

        FormulaWithBounds original = new FormulaWithBounds(original_f, b);
        FormulaWithBounds result = optimization.optimize(original);

        assertEquals("The bounds should be unchanged.", original.getBounds(), result.getBounds());
        assertEquals("The division should be folded.", expected_f.toString(), result.getFormula().toString());
    }

    @Test
    public void simpleModulo() {
        Universe u = new Universe("dummy");
        Bounds b = new Bounds(u);
        Relation r = Relation.unary("DummyInt");

        Formula original_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(7).modulo(IntConstant.constant(2)).toExpression());
        Formula expected_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(1).toExpression());

        FormulaWithBounds original = new FormulaWithBounds(original_f, b);
        FormulaWithBounds result = optimization.optimize(original);

        assertEquals("The bounds should be unchanged.", original.getBounds(), result.getBounds());
        assertEquals("The modulus should be folded.", expected_f.toString(), result.getFormula().toString());
    }

    @Test
    public void simpleAnd() {
        Universe u = new Universe("dummy");
        Bounds b = new Bounds(u);
        Relation r = Relation.unary("DummyInt");

        Formula original_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(3).and(IntConstant.constant(2)).toExpression());
        Formula expected_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(2).toExpression());

        FormulaWithBounds original = new FormulaWithBounds(original_f, b);
        FormulaWithBounds result = optimization.optimize(original);

        assertEquals("The bounds should be unchanged.", original.getBounds(), result.getBounds());
        assertEquals("The and should be folded.", expected_f.toString(), result.getFormula().toString());
    }

    @Test
    public void simpleOr() {
        Universe u = new Universe("dummy");
        Bounds b = new Bounds(u);
        Relation r = Relation.unary("DummyInt");

        Formula original_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(3).or(IntConstant.constant(2)).toExpression());
        Formula expected_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(3).toExpression());

        FormulaWithBounds original = new FormulaWithBounds(original_f, b);
        FormulaWithBounds result = optimization.optimize(original);

        assertEquals("The bounds should be unchanged.", original.getBounds(), result.getBounds());
        assertEquals("The or should be folded.", expected_f.toString(), result.getFormula().toString());
    }

    @Test
    public void simpleXor() {
        Universe u = new Universe("dummy");
        Bounds b = new Bounds(u);
        Relation r = Relation.unary("DummyInt");

        Formula original_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(3).xor(IntConstant.constant(2)).toExpression());
        Formula expected_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(1).toExpression());

        FormulaWithBounds original = new FormulaWithBounds(original_f, b);
        FormulaWithBounds result = optimization.optimize(original);

        assertEquals("The bounds should be unchanged.", original.getBounds(), result.getBounds());
        assertEquals("The xor should be folded.", expected_f.toString(), result.getFormula().toString());
    }

    @Test
    public void simpleShl() {
        Universe u = new Universe("dummy");
        Bounds b = new Bounds(u);
        Relation r = Relation.unary("DummyInt");

        Formula original_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(3).shl(IntConstant.constant(1)).toExpression());
        Formula expected_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(6).toExpression());

        FormulaWithBounds original = new FormulaWithBounds(original_f, b);
        FormulaWithBounds result = optimization.optimize(original);

        assertEquals("The bounds should be unchanged.", original.getBounds(), result.getBounds());
        assertEquals("The shl should be folded.", expected_f.toString(), result.getFormula().toString());
    }

    @Test
    public void simpleShr() {
        Universe u = new Universe("dummy");
        Bounds b = new Bounds(u);
        Relation r = Relation.unary("DummyInt");

        Formula original_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(-1).shr(IntConstant.constant(1)).toExpression());
        Formula expected_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant((-1) >>> 1).toExpression());

        FormulaWithBounds original = new FormulaWithBounds(original_f, b);
        FormulaWithBounds result = optimization.optimize(original);

        assertEquals("The bounds should be unchanged.", original.getBounds(), result.getBounds());
        assertEquals("The shr should be folded.", expected_f.toString(), result.getFormula().toString());
    }

    @Test
    public void simpleSha() {
        Universe u = new Universe("dummy");
        Bounds b = new Bounds(u);
        Relation r = Relation.unary("DummyInt");

        Formula original_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(-1).sha(IntConstant.constant(1)).toExpression());
        Formula expected_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant((-1) >> 1).toExpression());

        FormulaWithBounds original = new FormulaWithBounds(original_f, b);
        FormulaWithBounds result = optimization.optimize(original);

        assertEquals("The bounds should be unchanged.", original.getBounds(), result.getBounds());
        assertEquals("The sha should be folded.", expected_f.toString(), result.getFormula().toString());
    }

    @Test
    public void simpleAdditionIdentity() {
        Universe u = new Universe("dummy");
        Bounds b = new Bounds(u);
        Relation r = Relation.unary("DummyInt");
        Relation r2 = Relation.unary("DummyInt2");

        Formula original_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(0).plus(r2.apply(ExprCastOperator.SUM)).toExpression());
        Formula expected_f = r.compare(ExprCompOperator.EQUALS,
                                r2.apply(ExprCastOperator.SUM).toExpression());

        FormulaWithBounds original = new FormulaWithBounds(original_f, b);
        FormulaWithBounds result = optimization.optimize(original);

        assertEquals("The bounds should be unchanged.", original.getBounds(), result.getBounds());
        assertEquals("The addition should be folded.", expected_f.toString(), result.getFormula().toString());
    }

    @Test
    public void simpleMultiplicationIdentity() {
        Universe u = new Universe("dummy");
        Bounds b = new Bounds(u);
        Relation r = Relation.unary("DummyInt");
        Relation r2 = Relation.unary("DummyInt2");

        Formula original_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(1).multiply(r2.apply(ExprCastOperator.SUM)).toExpression());
        Formula expected_f = r.compare(ExprCompOperator.EQUALS,
                                r2.apply(ExprCastOperator.SUM).toExpression());

        FormulaWithBounds original = new FormulaWithBounds(original_f, b);
        FormulaWithBounds result = optimization.optimize(original);

        assertEquals("The bounds should be unchanged.", original.getBounds(), result.getBounds());
        assertEquals("The multiplication should be folded.", expected_f.toString(), result.getFormula().toString());
    }

    @Test
    public void simpleMultiplicationByZero() {
        Universe u = new Universe("dummy");
        Bounds b = new Bounds(u);
        Relation r = Relation.unary("DummyInt");
        Relation r2 = Relation.unary("DummyInt2");

        Formula original_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(0).multiply(r2.apply(ExprCastOperator.SUM)).toExpression());
        Formula expected_f = r.compare(ExprCompOperator.EQUALS,
                                IntConstant.constant(0).toExpression());

        FormulaWithBounds original = new FormulaWithBounds(original_f, b);
        FormulaWithBounds result = optimization.optimize(original);

        assertEquals("The bounds should be unchanged.", original.getBounds(), result.getBounds());
        assertEquals("The multiplication should be folded.", expected_f.toString(), result.getFormula().toString());
    }
}