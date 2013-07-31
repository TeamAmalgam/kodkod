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

public class VariableReplacer implements ReturnVisitor<Node,Node,Node,Node>{

	 Expression variable;

	public VariableReplacer(Expression v){
		variable = v;
	}
	
	public Node visit(Decls f)
	{
		return null;
	}
	
	public  Node visit(Decl f) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Node visit(BinaryIntExpression f) {
		return new BinaryIntExpression((IntExpression)f.left().accept(this), 
				f.op(), 
				(IntExpression) f.right().accept(this));
	}

	public  Node visit(IntConstant f) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Node visit(IntToExprCast f) {
		return new IntToExprCast((IntExpression)f.intExpr().accept(this),f.op());
	}

	public  Node visit(ExprToIntCast f) {
		return new ExprToIntCast((Expression) f.expression().accept(this), f.op());
	}

	public  Node visit(Variable f) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Node visit(ConstantExpression f) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Node visit(Relation f) {
		return f;
	}

	public  Node visit(NaryFormula f) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Node visit(NotFormula f) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Node visit(BinaryExpression f) {
		if(f.left() instanceof Variable)
		{
			return new BinaryExpression(variable, f.op(), f.right());
		}
		else
		{
			return new BinaryExpression((Expression)f.left().accept(this)
					, f.op(), 
					(Expression)f.right().accept(this));
		}
	}

	public  Node visit(ConstantFormula f) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Node visit(MultiplicityFormula f) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Node visit(IntComparisonFormula f) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Node visit(QuantifiedFormula f) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Node visit(ComparisonFormula f) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Node visit(BinaryFormula f) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Node visit(UnaryExpression f){
		return null;
	}

	@Override
	public Node visit(NaryExpression expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visit(Comprehension comprehension) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visit(IfExpression ifExpr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visit(ProjectExpression project) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visit(IfIntExpression intExpr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visit(NaryIntExpression intExpr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visit(UnaryIntExpression intExpr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visit(SumExpression intExpr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visit(RelationPredicate predicate) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
