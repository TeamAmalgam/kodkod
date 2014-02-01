package kodkod.intelision;

import kodkod.ast.BinaryExpression;
import kodkod.ast.ComparisonFormula;
import kodkod.ast.Decl;
import kodkod.ast.Decls;
import kodkod.ast.ExprToIntCast;
import kodkod.ast.Expression;
import kodkod.ast.IntComparisonFormula;
import kodkod.ast.IntToExprCast;
import kodkod.ast.NaryFormula;
import kodkod.ast.Node;
import kodkod.ast.QuantifiedFormula;
import kodkod.ast.Relation;
import kodkod.ast.operator.ExprCastOperator;
import kodkod.ast.operator.ExprCompOperator;
import kodkod.ast.operator.FormulaOperator;
import kodkod.ast.visitor.AbstractVoidVisitor;
import kodkod.util.collections.IdentityHashSet;

//Traverses the tree marking equality and inequality nodes 
class EqualityFinder extends AbstractVoidVisitor {

	IntExprReduction ier;
	final IdentityHashSet<ComparisonFormula> comparisonNodes = new IdentityHashSet<ComparisonFormula>();
	final IdentityHashSet<IntComparisonFormula> intComparisonNodes = new IdentityHashSet<IntComparisonFormula>();
	
	
	private Expression quantExpression;
	private String multiplicity; 
	
	
	
	public EqualityFinder(IntExprReduction ier) { 
		super();
		this.ier = ier;
	}
	/*
	public static void callByType(Object f)
	{
		if (f instanceof BinaryFormula)
			checkForInts((BinaryFormula) f);
		else if (f instanceof ComparisonFormula)
			checkForInts((ComparisonFormula) f);
		else if (f instanceof ConstantFormula)
			checkForInts((ConstantFormula) f);
		else if (f instanceof MultiplicityFormula)
			checkForInts((MultiplicityFormula) f);
		else if (f instanceof IntComparisonFormula)
			checkForInts((IntComparisonFormula) f);
		else if (f instanceof QuantifiedFormula)
			checkForInts((QuantifiedFormula) f);
		else if (f instanceof NaryFormula)
			checkForInts((NaryFormula) f);
		else if (f instanceof NotFormula)
			checkForInts((NotFormula) f);
		else if (f instanceof BinaryExpression)
			checkForInts((BinaryExpression) f);
		else if (f instanceof Relation)
			checkForInts((Relation) f);
		else if (f instanceof ConstantExpression)
			checkForInts((ConstantExpression) f);
		else if (f instanceof Variable)
			checkForInts((Variable) f);
		else if (f instanceof ExprToIntCast)
			checkForInts((ExprToIntCast) f);
		else if (f instanceof IntToExprCast)
			checkForInts((IntToExprCast) f);
		else if (f instanceof IntConstant)
			checkForInts((IntConstant) f);
		else if (f instanceof BinaryIntExpression)
			checkForInts((BinaryIntExpression) f);
		else if (f instanceof UnaryExpression)
			checkForInts((UnaryExpression) f);
		else
			System.out.println("ERROR" + f);
	}
	*/
	
	public void visit(final ComparisonFormula f)
	{
		f.left().accept(this);
		f.right().accept(this);
		if(f.right().toString().equals(f.left().toString()) && f.right().toString().contains("Int/next")){
			ier.addReduction(f,ier.reductions_delete );
			System.out.println("EXACT: " + f.right());
		}
		if(f.op() == ExprCompOperator.EQUALS && (f.left() instanceof IntToExprCast || f.right() instanceof IntToExprCast)){
			
			
			if(f.left() instanceof BinaryExpression || f.left() instanceof Relation){ // && ((BinaryExpression)f.left()).right() instanceof Relation)
				comparisonNodes.add(f);
				//System.out.println("Found comparison node: " + f);
				//f.variable = (Relation)quantExpression;
				ier.variables.put(f, quantExpression);
				//System.out.println(f.variable);
				if(f.left() instanceof Relation)
					//f.answer = f.left().toString();
					ier.answers.put(f, f.left().toString());
				else if(multiplicity != null)
					//f.answer = myToString((BinaryExpression)f.left(), multiplicity.toString(), quantExpression.toString());
					ier.answers.put(f, 
							myToString((BinaryExpression)f.left(), multiplicity.toString(), quantExpression.toString()));
				else 
					//f.answer = myToString((BinaryExpression)f.left(), "","");
					ier.answers.put(f, myToString((BinaryExpression)f.left(), "",""));
			}
			else if(f.right() instanceof BinaryExpression  || f.right() instanceof Relation){
				//f.variable = (Relation)quantExpression;
				ier.variables.put(f, quantExpression);
				comparisonNodes.add(f);
				//System.out.println("Found comparison node: " + f);
				if(f.right() instanceof Relation)
					//f.answer = f.right().toString();
					ier.answers.put(f, f.right().toString());
				else if(multiplicity != null)
					//f.answer = myToString((BinaryExpression)f.right(), multiplicity.toString(), quantExpression.toString());
					ier.answers.put(f, myToString((BinaryExpression)f.right(), multiplicity.toString(), quantExpression.toString()));
				else 
					//f.answer = myToString((BinaryExpression)f.right(), "","");
					ier.answers.put(f, myToString((BinaryExpression)f.right(), "",""));
			}
				
		}
	}
	
	public void visit(NaryFormula formula){
		if(formula.op() != FormulaOperator.AND){
			System.out.println("NaryFormula only works with AND currently.");
			System.exit(1);
		}
		for(int i = 0; i < formula.size(); i++)
			formula.child(i).accept(this);
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
		System.out.println("Found int comparison: " + f);
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
