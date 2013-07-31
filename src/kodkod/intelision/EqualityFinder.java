package kodkod.intelision;

import kodkod.ast.BinaryExpression;
import kodkod.ast.ComparisonFormula;
import kodkod.ast.Decl;
import kodkod.ast.Decls;
import kodkod.ast.ExprToIntCast;
import kodkod.ast.Expression;
import kodkod.ast.IntComparisonFormula;
import kodkod.ast.IntToExprCast;
import kodkod.ast.Node;
import kodkod.ast.QuantifiedFormula;
import kodkod.ast.Relation;
import kodkod.ast.operator.ExprCastOperator;
import kodkod.ast.operator.ExprCompOperator;
import kodkod.ast.visitor.AbstractVoidVisitor;
import kodkod.util.collections.IdentityHashSet;

//Traverses the tree marking equality and inequality nodes 
class EqualityFinder extends AbstractVoidVisitor {

	final IdentityHashSet<ComparisonFormula> comparisonNodes = new IdentityHashSet<ComparisonFormula>();
	final IdentityHashSet<IntComparisonFormula> intComparisonNodes = new IdentityHashSet<IntComparisonFormula>();
	
	
	private Expression quantExpression;
	private String multiplicity; 
	
	
	
	public EqualityFinder() { super(); }
	
	public void visit(final ComparisonFormula f)
	{
		f.left().accept(this);
		f.right().accept(this);
		if(f.op() == ExprCompOperator.EQUALS && (f.left() instanceof IntToExprCast || f.right() instanceof IntToExprCast)){
			
			
			if(f.left() instanceof BinaryExpression || f.left() instanceof Relation){
				comparisonNodes.add(f);
				IntExprReduction.variables.put(f, quantExpression);
				if(f.left() instanceof Relation)
					IntExprReduction.answers.put(f, f.left().toString());
				else if(multiplicity != null)
					IntExprReduction.answers.put(f, 
							myToString((BinaryExpression)f.left(), multiplicity.toString(), quantExpression.toString()));
				else 
					IntExprReduction.answers.put(f, myToString((BinaryExpression)f.left(), "",""));
			}
			else if(f.right() instanceof BinaryExpression  || f.right() instanceof Relation){
				IntExprReduction.variables.put(f, quantExpression);
				comparisonNodes.add(f);
				if(f.right() instanceof Relation)
					IntExprReduction.answers.put(f, f.right().toString());
				else if(multiplicity != null)
					IntExprReduction.answers.put(f, myToString((BinaryExpression)f.right(), multiplicity.toString(), quantExpression.toString()));
				else 
					IntExprReduction.answers.put(f, myToString((BinaryExpression)f.right(), "",""));
			}
				
		}
	}

	// TODO: rewrite this myToString() method
	static String myToString(final BinaryExpression be, final String mult, final String expression){
		if(be.right() instanceof BinaryExpression)
			return myToString(((BinaryExpression)be.right()), mult,expression);
		else if(be.right() instanceof Relation)
			return be.right().toString();
		else 
			System.exit(1); // TODO: remove this call
		return "";
	}
	

	
	public void visit(IntComparisonFormula f)
	{
		f.left().accept(this);
		f.right().accept(this);
		intComparisonNodes.add(f);
	}
	
	public void visit(final QuantifiedFormula f)
	{
		final Decls decls = f.decls();
		final Decl d = decls.get(0);
		multiplicity = d.multiplicity().toString();
		quantExpression = d.expression();
		f.formula().accept(this);
		multiplicity = null;
		quantExpression = null;
	}
	
	public void visit(IntToExprCast castExpr) {
		castExpr.intExpr().accept(this);
	}
	
	
	public void visit(ExprToIntCast intExpr) {
		if(intExpr.op() == ExprCastOperator.SUM){
			intExpr.expression().accept(this);
		}
	}


	protected boolean visited(Node n) {
		return false;
	}
	
}
