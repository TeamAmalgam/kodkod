package kodkod.optimizer;

import kodkod.ast.*;
import kodkod.ast.operator.*;
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
            Decls[] optimized_decls = new Decls[decls.size()];
            boolean decls_changed = false;

            for (int i = 0; i < decls.size(); i += 1) {
                optimized_decls[i] = decls.get(i).accept(this);
                if (optimized_decls[i] != decls.get(i)) {
                    decls_changed = true;
                }
            }

            if (decls_changed) {
                Decls decls_to_return = optimized_decls[0];
                for (int i = 1; i < decls.size(); i += 1) {
                    decls_to_return = decls_to_return.and(optimized_decls[i]);
                }
                return decls_to_return;
            }

            return decls;
        }

        public Decls visit(Decl decl) {
            Expression expr = decl.expression();
            Expression expr_optimized = expr.accept(this);

            if (expr_optimized != expr) {
                return decl.variable().declare(decl.multiplicity(), expr_optimized);
            } 

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
            Expression inner_expr = unaryExpr.expression();
            Expression inner_expr_optimized = inner_expr.accept(this);

            if (inner_expr_optimized != inner_expr) {
                return inner_expr_optimized.apply(unaryExpr.op());
            }

            return unaryExpr;
        }

        public Expression visit(BinaryExpression binaryExpr) {
            Expression left = binaryExpr.left();
            Expression right = binaryExpr.right();

            Expression left_optimized = left.accept(this);
            Expression right_optimized = right.accept(this);

            if (left != left_optimized || right != right_optimized) {
                return left_optimized.compose(binaryExpr.op(), right_optimized);
            }

            return binaryExpr;
        }

        public Expression visit(NaryExpression naryExpr) {
            Expression[] expressions_optimized = new Expression[naryExpr.size()];
            boolean expressions_changed = false;

            for (int i = 0; i < naryExpr.size(); i += 1) {
                expressions_optimized[i] = naryExpr.child(i).accept(this);
                if (expressions_optimized[i] != naryExpr.child(i)) {
                    expressions_changed = true;
                }
            }

            if (expressions_changed) {
                return Expression.compose(naryExpr.op(), expressions_optimized);
            }

            return naryExpr;
        }

        public Expression visit(Comprehension comprehension) {
            return comprehension;
        }

        public Expression visit(IfExpression ifExpr) {
            Formula condition = ifExpr.condition();
            Expression then_expression = ifExpr.thenExpr();
            Expression else_expression = ifExpr.elseExpr();

            Formula condition_optimized = condition.accept(this);
            Expression then_expression_optimized = then_expression.accept(this);
            Expression else_expression_optimized = else_expression.accept(this);

            if (condition != condition_optimized ||
                then_expression != then_expression_optimized ||
                else_expression != else_expression_optimized) 
            {
                return condition_optimized.thenElse(then_expression_optimized,
                                                    else_expression_optimized);
            }

            return ifExpr;
        }

        public Expression visit(ProjectExpression project) {
            Expression expr = project.expression();

            Expression expr_optimized = expr.accept(this);
            IntExpression[] optimized_columns = new IntExpression[project.arity()];
            boolean columns_changed = false;

            for (int i = 0; i < project.arity(); i += 1) {
                optimized_columns[i] = project.column(i).accept(this);
                if (optimized_columns[i] != project.column(i)) {
                    columns_changed = true;
                }
            }

            if (columns_changed || expr != expr_optimized) {
                return expr_optimized.project(optimized_columns);
            }

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
            Formula condition = intExpr.condition();
            IntExpression then_expr = intExpr.thenExpr();
            IntExpression else_expr = intExpr.elseExpr();

            Formula condition_optimized = condition.accept(this);
            IntExpression then_expr_optimized = then_expr.accept(this);
            IntExpression else_expr_optimized = else_expr.accept(this);

            if (condition_optimized != condition ||
                then_expr_optimized != then_expr ||
                else_expr_optimized != else_expr)
            {
                return condition_optimized.thenElse(then_expr_optimized, else_expr_optimized);
            }

            return intExpr;
        }

        public IntExpression visit(ExprToIntCast intExpr) {
            Expression inner_expr = intExpr.expression();
            Expression inner_expr_optimized = inner_expr.accept(this);

            if (inner_expr_optimized != inner_expr) {
                return inner_expr_optimized.apply(intExpr.op());
            }

            return intExpr;
        }

        public IntExpression visit(NaryIntExpression intExpr) {
            IntExpression[] children_optimized = new IntExpression[intExpr.size()];
            boolean child_changed = false;
            int int_constant_count = 0;
            int non_constant_count = 0;

            for (int i = 0; i < intExpr.size(); i += 1) {
                children_optimized[i] = intExpr.child(i).accept(this);   

                if (children_optimized[i] != intExpr.child(i)) {
                    child_changed = true;
                }

                if (children_optimized[i] instanceof IntConstant) {
                    int_constant_count += 1;
                } else {
                    non_constant_count += 1;
                }
            }

            if (int_constant_count > 0) {
                int int_value = 0;
                int i;

                // Find the first child that is an integer constant.
                for (i = 0; i < intExpr.size(); i += 1) {
                    if (children_optimized[i] instanceof IntConstant) {
                        int_value = ((IntConstant)children_optimized[i]).value();
                        break;
                    }
                }

                // Fold all the other integer constants.
                for (i = i + 1; i < intExpr.size(); i += 1) {
                    if (children_optimized[i] instanceof IntConstant) {
                        int child_int_value = ((IntConstant)children_optimized[i]).value();
                        switch(intExpr.op()) {
                            case PLUS:
                                int_value += child_int_value;
                                break;
                            case MULTIPLY:
                                int_value *= child_int_value;
                                break;
                            case AND:
                                int_value &= child_int_value;
                                break;
                            case OR:
                                int_value |= child_int_value;
                                break;
                            default:
                                throw new RuntimeException("Unimplemented operator: " + intExpr.op());
                        }
                    }
                }

                if (non_constant_count == 0) {
                    return IntConstant.constant(int_value);
                } else if ((int_value == 0 && intExpr.op() == IntOperator.PLUS) ||
                           (int_value == 1 && intExpr.op() == IntOperator.MULTIPLY) ||
                           (int_value == 0 && intExpr.op() == IntOperator.OR))
                {
                    IntExpression[] new_expressions = new IntExpression[non_constant_count];
                    int current_expr = 0;

                    for (i = 0; i < intExpr.size(); i += 1) {
                        if (!(children_optimized[i] instanceof IntConstant)) {
                            new_expressions[current_expr] = children_optimized[i];
                            current_expr += 1;
                        }
                    }

                    return IntExpression.compose(intExpr.op(), new_expressions);
                } else {
                    IntExpression[] new_expressions = new IntExpression[non_constant_count + 1];
                    new_expressions[0] = IntConstant.constant(int_value);
                    int current_expr = 1;

                    for (i = 0; i < intExpr.size(); i += 1) {
                        if (!(children_optimized[i] instanceof IntConstant)) {
                            new_expressions[current_expr] = children_optimized[i];
                            current_expr += 1;
                        }
                    }

                    return IntExpression.compose(intExpr.op(), new_expressions);
                }
            }

            if (child_changed) {
                return IntExpression.compose(intExpr.op(), children_optimized);
            }

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
                // Handle the case where the left and right have both
                // been optimized into constants.

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
            }

            if ((left_optimized instanceof IntConstant) ||
                (right_optimized instanceof IntConstant))
            {
                // Handle the case where one of the formulas is an integer constant.
               
                int int_value;
                IntExpression other_value;

                if (left_optimized instanceof IntConstant) {
                    int_value = ((IntConstant)left_optimized).value();
                    other_value = right_optimized;
                } else {
                    int_value = ((IntConstant)right_optimized).value();
                    other_value = left_optimized;
                }

                if (intExpr.op() == IntOperator.PLUS &&
                    int_value == 0)
                {
                    return other_value;
                }

                if (intExpr.op() == IntOperator.MULTIPLY &&
                    int_value == 1)
                {
                    return other_value;
                }

                if (intExpr.op() == IntOperator.MULTIPLY &&
                    int_value == 0)
                {
                    return IntConstant.constant(0);
                }

                if (intExpr.op() == IntOperator.MINUS &&
                    int_value == 0 &&
                    (right_optimized instanceof IntConstant))
                {
                    return other_value;
                }

                if (intExpr.op() == IntOperator.MINUS &&
                    int_value == 0 &&
                    (left_optimized instanceof IntConstant))
                {
                    return other_value.negate().accept(this);
                }

                if (intExpr.op() == IntOperator.DIVIDE &&
                    int_value == 1 &&
                    (right_optimized instanceof IntConstant))
                {
                    return other_value;
                }

                if (intExpr.op() == IntOperator.DIVIDE &&
                    int_value == 0 &&
                    (left_optimized instanceof IntConstant))
                {
                    return IntConstant.constant(0);
                }

            }

            if ((left_optimized != left) ||
                       (right_optimized != right))
            {
                // Handle the case where the formulas have changed but we can't
                // perform any further optimization.
                return left_optimized.compose(intExpr.op(), right_optimized);
            }

            return intExpr;
        }

        public IntExpression visit(UnaryIntExpression intExpr) {
            IntExpression expr = intExpr.intExpr();
            IntExpression expr_optimized = expr.accept(this);

            if (expr_optimized instanceof IntConstant) {
                int int_value = ((IntConstant)expr_optimized).value();

                switch (intExpr.op()) {
                    case NEG:
                        return IntConstant.constant(-int_value);
                    case NOT:
                        return IntConstant.constant(~int_value);
                    case SGN:
                        if (int_value > 0) {
                            return IntConstant.constant(1);
                        } else if (int_value < 0) {
                            return IntConstant.constant(-1);
                        } else {
                            return IntConstant.constant(0);
                        }
                    default:
                        throw new RuntimeException("Unimplemented operator: " + intExpr.op());

                }
            }

            if (intExpr.op() == IntOperator.NEG &&
                (expr_optimized instanceof UnaryIntExpression) &&
                ((UnaryIntExpression)expr_optimized).op() == IntOperator.NEG) 
            {
                return ((UnaryIntExpression)expr_optimized).intExpr();
            }

            if (intExpr.op() == IntOperator.NOT &&
                (expr_optimized instanceof UnaryIntExpression) &&
                ((UnaryIntExpression)expr_optimized).op() == IntOperator.NOT)
            {
                return ((UnaryIntExpression)expr_optimized).intExpr();
            }

            if (intExpr.op() == IntOperator.SGN &&
                (expr_optimized instanceof UnaryIntExpression) &&
                ((UnaryIntExpression)expr_optimized).op() == IntOperator.SGN)
            {
                return expr_optimized;
            }
            return intExpr;
        }

        public IntExpression visit(SumExpression intExpr) {
            Decls decls = intExpr.decls();
            IntExpression int_expr = intExpr.intExpr();

            Decls decls_optimized = decls.accept(this);
            IntExpression int_expr_optimized = int_expr.accept(this);

            if (decls_optimized != decls || int_expr_optimized != int_expr) {
                return int_expr_optimized.sum(decls_optimized);
            }

            return intExpr;
        }

        public Formula visit(IntComparisonFormula intComp) {
            IntExpression left = intComp.left();
            IntExpression right = intComp.right();

            IntExpression left_optimized = left.accept(this);
            IntExpression right_optimized = right.accept(this);

            if (left_optimized != left || right_optimized == right) {
                return left_optimized.compare(intComp.op(), right_optimized);
            }

            return intComp;
        }

        public Formula visit(QuantifiedFormula quantFormula) {
            Decls decls = quantFormula.decls();
            Formula formula = quantFormula.formula();

            Decls decls_optimized = decls.accept(this);
            Formula formula_optimized = formula.accept(this);

            if (decls_optimized != decls ||
                formula_optimized != formula)
            {
                return formula_optimized.quantify(quantFormula.quantifier(), decls_optimized);
            }

            return quantFormula;
        }

        public Formula visit(NaryFormula formula) {
            Formula[] formulas_optimized = new Formula[formula.size()];
            boolean formulas_changed = false;

            for (int i = 0; i < formula.size(); i += 1) {
                formulas_optimized[i] = formula.child(i).accept(this);

                if (formulas_optimized[i] != formula.child(i)) {
                    formulas_changed = true;
                }
            }

            if (formulas_changed) {
                return Formula.compose(formula.op(), formulas_optimized);
            }

            return formula;
        }

        public Formula visit(BinaryFormula binFormula) {
            Formula left = binFormula.left();
            Formula right = binFormula.right();
            Formula left_optimized = left.accept(this);
            Formula right_optimized = right.accept(this); 

            if (left_optimized != left || right_optimized != right) {
                return left_optimized.compose(binFormula.op(), right_optimized);
            }

            return binFormula;
        }

        public Formula visit(NotFormula notFormula) {
            Formula formula = notFormula.formula();
            Formula formula_optimized = formula.accept(this);

            if (formula_optimized != formula) {
                return formula_optimized.not();
            }

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
            Expression expr = multFormula.expression();
            Expression expr_optimized = expr.accept(this);

            if (expr_optimized != expr) {
                return expr_optimized.apply(multFormula.multiplicity());
            }

            return multFormula;
        }

        public Formula visit(RelationPredicate predicate) {
            return predicate;
        }
    }
}