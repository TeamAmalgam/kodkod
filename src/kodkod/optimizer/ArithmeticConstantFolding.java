package kodkod.optimizer;

import kodkod.ast.*;
import kodkod.ast.visitor.ReturnVisitor;
import kodkod.engine.Solution;
import kodkod.instance.Bounds;

/**
 * Pass that folds arithmetic on integer constants.
 */
public class ArithmeticConstantFolding implements OptimizationPass {

    public ArithmeticConstantFolding() {
    }

    /**
     * This method should be called with the initial problem formula and bounds.
     * 
     * @return some f' : Formula | f' <=> f
     */
    public FormulaWithBounds optimize(FormulaWithBounds fb) {
        ConstantFoldingVisitor cfv = new ConstantFoldingVisitor();
        Formula new_formula = fb.getFormula().accept(cfv);

        return new FormulaWithBounds(new_formula, fb.getBounds());
    }

    /**
     * This method should be called with additional problem formula and bounds.
     *
     * @return some f' : Formula | previous_formulae' & f' <=> previous_formulae & f
     *
     * @throws IllegalStateException if optimize has not been called.
     */
    public FormulaWithBounds optimizeIncremental(FormulaWithBounds fb) {
        ConstantFoldingVisitor cfv = new ConstantFoldingVisitor();
        Formula new_formula = (Formula)fb.getFormula().accept(cfv);

        return new FormulaWithBounds(new_formula, fb.getBounds());
    }

    /**
     * This method translates a solution under the optimized bounds into a solution
     * under the original bounds.
     *
     * @return solution under the original bounds.
     */
    public Solution translateSolution(Solution s) {
        return s;
    }

    private static class ConstantFoldingVisitor implements ReturnVisitor<Expression, Formula, Decls, IntExpression> {
        public ConstantFoldingVisitor() {
        }

        public Decls visit(Decls decls) {
            return decls;
        }

        public Decls visit(Decl decl) {
            return decl;
        }

        public Expression visit(Relation relation) {
            return relation;
        }

        public Expression visit(Variable variable) {
            return variable;
        }

        public Expression visit(ConstantExpression constExpr) {
            return constExpr;
        }

        public Expression visit(UnaryExpression unaryExpr) {
            return unaryExpr;
        }

        public Expression visit(BinaryExpression binaryExpr) {
            return binaryExpr;
        }

        public Expression visit(NaryExpression naryExpr) {
            return naryExpr;
        }

        public Expression visit(Comprehension comprehension) {
            return comprehension;
        }

        public Expression visit(IfExpression ifExpr) {
            return ifExpr;
        }

        public Expression visit(ProjectExpression project) {
            return project;
        }

        public Expression visit(IntToExprCast castExpr) {
            IntExpression expr = castExpr.intExpr();
            IntExpression expr_optimized = expr.accept(this);

            if (expr != expr_optimized) {
                return expr_optimized.cast(castExpr.op());
            }

            return castExpr;
        }

        public IntExpression visit(IntConstant intConst) {
            return intConst;
        }

        public IntExpression visit(IfIntExpression intExpr) {
            return intExpr;
        }

        public IntExpression visit(ExprToIntCast intExpr) {
            return intExpr;
        }

        public IntExpression visit(NaryIntExpression intExpr) {
            return intExpr;
        }

        public IntExpression visit(BinaryIntExpression intExpr) {
            IntExpression left = intExpr.left();
            IntExpression right = intExpr.right();
            IntExpression left_optimized = left.accept(this);
            IntExpression right_optimized = right.accept(this);

            if ((left_optimized instanceof IntConstant) &&
                (right_optimized instanceof IntConstant))
            {
                int left_int = ((IntConstant)left_optimized).value();
                int right_int = ((IntConstant)right_optimized).value();

                switch (intExpr.op()) {
                    case PLUS:
                        return IntConstant.constant(left_int + right_int);
                    case MINUS:
                        return IntConstant.constant(left_int - right_int);
                    case MULTIPLY:
                        return IntConstant.constant(left_int * right_int);
                    case DIVIDE:
                        return IntConstant.constant(left_int / right_int);
                    case MODULO:
                        return IntConstant.constant(left_int % right_int);
                    case AND:
                        return IntConstant.constant(left_int & right_int);
                    case OR:
                        return IntConstant.constant(left_int | right_int);
                    case XOR:
                        return IntConstant.constant(left_int ^ right_int);
                    case SHL:
                        return IntConstant.constant(left_int << right_int);
                    case SHR:
                        return IntConstant.constant(left_int >>> right_int);
                    case SHA:
                        return IntConstant.constant(left_int >> right_int);
                    default:
                        throw new RuntimeException("Unimplemented operator: " + intExpr.op());
                }
            } else if ((left_optimized != left) ||
                       (right_optimized != right))
            {
                return left_optimized.compose(intExpr.op(), right_optimized);
            }

            return intExpr;
        }

        public IntExpression visit(UnaryIntExpression intExpr) {
            return intExpr;
        }

        public IntExpression visit(SumExpression intExpr) {
            return intExpr;
        }

        public Formula visit(IntComparisonFormula intComp) {
            return intComp;
        }

        public Formula visit(QuantifiedFormula quantFormula) {
            return quantFormula;
        }

        public Formula visit(NaryFormula formula) {
            return formula;
        }

        public Formula visit(BinaryFormula binFormula) {
            return binFormula;
        }

        public Formula visit(NotFormula notFormula) {
            return notFormula;
        }

        public Formula visit(ConstantFormula constant) {
            return constant;
        }

        public Formula visit(ComparisonFormula compFormula) {
            Expression left = compFormula.left();
            Expression right = compFormula.right();
            Expression left_optimized = left.accept(this);
            Expression right_optimized = right.accept(this);

            if (left_optimized != left || right_optimized != right) {
                return left_optimized.compare(compFormula.op(), right_optimized);
            }

            return compFormula;
        }

        public Formula visit(MultiplicityFormula multFormula) {
            return multFormula;
        }

        public Formula visit(RelationPredicate predicate) {
            return predicate;
        }
    }
}