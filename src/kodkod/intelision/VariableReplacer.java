package kodkod.intelision;

import kodkod.ast.BinaryExpression;
import kodkod.ast.BinaryFormula;
import kodkod.ast.BinaryIntExpression;
import kodkod.ast.ComparisonFormula;
import kodkod.ast.Comprehension;
import kodkod.ast.ConstantExpression;
import kodkod.ast.ConstantFormula;
import kodkod.ast.Decl;
import kodkod.ast.Decls;
import kodkod.ast.ExprToIntCast;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IfExpression;
import kodkod.ast.IfIntExpression;
import kodkod.ast.IntComparisonFormula;
import kodkod.ast.IntConstant;
import kodkod.ast.IntExpression;
import kodkod.ast.IntToExprCast;
import kodkod.ast.MultiplicityFormula;
import kodkod.ast.NaryExpression;
import kodkod.ast.NaryFormula;
import kodkod.ast.NaryIntExpression;
import kodkod.ast.Node;
import kodkod.ast.NotFormula;
import kodkod.ast.ProjectExpression;
import kodkod.ast.QuantifiedFormula;
import kodkod.ast.Relation;
import kodkod.ast.RelationPredicate;
import kodkod.ast.SumExpression;
import kodkod.ast.UnaryExpression;
import kodkod.ast.UnaryIntExpression;
import kodkod.ast.Variable;
import kodkod.ast.visitor.ReturnVisitor;

class VariableReplacer implements ReturnVisitor<Node,Node,Node,Node>{

	private final Expression variable;

	public VariableReplacer(Expression v){
		variable = v;
	}
	
	public Node visit(Decls f)
	{
		throw new RuntimeException("Not Implemented Yet.");
	}
	
	public  Node visit(Decl f) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	public  Node visit(BinaryIntExpression f) {
		return new BinaryIntExpression((IntExpression)f.left().accept(this), 
				f.op(), 
				(IntExpression) f.right().accept(this));
	}

	public  Node visit(IntConstant f) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	public  Node visit(IntToExprCast f) {
		//return new IntToExprCast((IntExpression)f.intExpr().accept(this),f.op());
		return ((IntExpression)f.intExpr().accept(this)).cast(f.op());
	}

	public  Node visit(ExprToIntCast f) {
		//return new ExprToIntCast((Expression) f.expression().accept(this), f.op());
		return ((Expression) f.expression().accept(this)).apply(f.op());
	}

	public  Node visit(Variable f) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	public  Node visit(ConstantExpression f) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	public  Node visit(Relation f) {
		return f;
	}

	public  Node visit(NaryFormula formula) {
		if(formula.size() == 0)
			throw new RuntimeException("Not Implemented Yet.");
		Formula builtFormula = (Formula)formula.child(0).accept(this);
		for(int i = 1; i < formula.size(); i++)
			builtFormula = builtFormula.compose(formula.op(), (Formula)formula.child(i).accept(this));
		return builtFormula;
	}

	public  Node visit(NotFormula f) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	public  Node visit(final BinaryExpression f) {
		if(f.left() instanceof Variable)
		{
			//return new BinaryExpression(variable, f.op(), f.right());
			return variable.compose(f.op(), f.right());
		}
		else
		{
			//return new BinaryExpression((Expression)f.left().accept(this)
			//		, f.op(), 
			//		(Expression)f.right().accept(this));
			return ((Expression)f.left().accept(this)).compose(
					f.op(), 
					(Expression)f.right().accept(this));
		}
	}

	public  Node visit(ConstantFormula f) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	public  Node visit(MultiplicityFormula f) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	public  Node visit(IntComparisonFormula f) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	public  Node visit(QuantifiedFormula f) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	public  Node visit(ComparisonFormula f) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	public  Node visit(BinaryFormula f) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}
	
	public Node visit(UnaryExpression f){
		throw new RuntimeException("Not Implemented Yet.");
	}

	@Override
	public Node visit(NaryExpression expr) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	@Override
	public Node visit(Comprehension comprehension) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	@Override
	public Node visit(IfExpression ifExpr) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	@Override
	public Node visit(ProjectExpression project) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	@Override
	public Node visit(IfIntExpression intExpr) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	@Override
	public Node visit(NaryIntExpression intExpr) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	@Override
	public Node visit(UnaryIntExpression intExpr) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}

	@Override
	public Node visit(SumExpression intExpr) {
		// TODO Auto-generated method stub
		return intExpr;
	}

	@Override
	public Node visit(RelationPredicate predicate) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not Implemented Yet.");
	}
	
	
}
