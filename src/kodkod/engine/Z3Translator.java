package kodkod.engine;

import kodkod.ast.*;
import kodkod.ast.visitor.ReturnVisitor;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import com.microsoft.z3.*;

public final class Z3Translator 
    implements ReturnVisitor<Expr, BoolExpr, Expr, IntExpr> {
    
    private final Context context;
    private final Formula formula;
    private final Bounds bounds;
    private BoolExpr expression;

    public Z3Translator(Context context, Formula f, Bounds b) {
        this.context = context;
        this.formula = f;
        this.bounds = b;
    }

    public void generateTranslation() {
        expression = formula.accept(this);
    }

    public BoolExpr getTranslation() {
        return this.expression;
    }

    public Instance getInstance(Model model) {
        return null;
    }

    public Expr visit(Decls decls) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public Expr visit(Decl decl) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public Expr visit(Relation relation) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public Expr visit(Variable variable) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public Expr visit(ConstantExpression constExpr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public Expr visit(UnaryExpression unaryExpr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public Expr visit(BinaryExpression binExpr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public Expr visit(NaryExpression expr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public Expr visit(Comprehension comprehension) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public Expr visit(IfExpression ifExpr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public Expr visit(ProjectExpression project) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public Expr visit(IntToExprCast castExpr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public IntExpr visit(IntConstant intConst) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public IntExpr visit(IfIntExpression intExpr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public IntExpr visit(ExprToIntCast intExpr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public IntExpr visit(NaryIntExpression intExpr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public IntExpr visit(BinaryIntExpression intExpr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public IntExpr visit(UnaryIntExpression intExpr) {
        throw new RuntimeException("Not Implemneted Yet.");
    }

    public IntExpr visit(SumExpression intExpr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public BoolExpr visit(IntComparisonFormula intComp) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public BoolExpr visit(QuantifiedFormula quantFormula) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public BoolExpr visit(NaryFormula formula) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public BoolExpr visit(BinaryFormula binFormula) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public BoolExpr visit(NotFormula not) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public BoolExpr visit(ConstantFormula constant) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public BoolExpr visit(ComparisonFormula compFormula) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public BoolExpr visit(MultiplicityFormula multFormula) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public BoolExpr visit(RelationPredicate predicate) {
        throw new RuntimeException("Not Implemented Yet.");
    }
}