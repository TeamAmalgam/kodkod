package kodkod.intelision;

import java.util.Collections;
import java.util.Map;

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
import kodkod.ast.operator.ExprCastOperator;
import kodkod.ast.visitor.ReturnVisitor;

public class ArithmeticStorageElider implements ReturnVisitor<Node,Node,Node,Node>{
	private enum Replace{
		FALSE,
		COMPARISON,
		INTCOMPARISON,
		VARIABLES
	};
	private Replace replace = Replace.FALSE;
	private final Map<String, Expression> swapAnswerPairs;
	
	public Variable quantVariable=null;
	public String quantExpression="";
	public String multiplicity=""; 
	
	public ArithmeticStorageElider(Map<String, Expression> swapAnswerPairs)
	{
		this.swapAnswerPairs = Collections.unmodifiableMap(swapAnswerPairs);
	}
	
	//*************************************************
		//
		//Build Tree Section
		//Traverses the original tree, building a copy of
		//it that contains modified nodes to avoid 
		//arithmetic storage where possible
		//
		//*************************************************
		
		public Decls visit(Decls decls) {
			return null;
			
		}

		
		public   Decl visit(Decl decl) {
			return null;
			
		}

		
		public Expression visit(final Relation relation) {
			final String answer =relation.toString();
			if(swapAnswerPairs.containsKey(answer))
			{
				final Expression e  = swapAnswerPairs.get(answer);//((Relation)binExpr.right()).name());
				if(e instanceof IntToExprCast){
					if(replace == Replace.INTCOMPARISON){
						
						IntExpression i =(IntExpression) ((IntToExprCast)e).intExpr();
						return (Expression)i.accept(new VariableReplacer(quantVariable));// binExpr.left());
					}
					else if(replace == Replace.COMPARISON){
						System.out.println("CHECK THIS WHEN IT COMES UP");
						return (Expression)e.accept(new VariableReplacer(quantVariable));
					}
				}
			}
			return relation;
			
		}

		
		public   Variable visit(Variable variable) {
			return null;
			
		}

		
		public   ConstantExpression visit(ConstantExpression constExpr) {
			return null;
			
		}

		
		public   UnaryExpression visit(UnaryExpression unaryExpr) {
			return null;
			
		}

		
		public Node visit(final BinaryExpression binExpr) {
			final String answer = EqualityFinder.myToString(binExpr, multiplicity, quantExpression);
			if(swapAnswerPairs.containsKey(answer))
			{
				final Expression e  = swapAnswerPairs.get(answer);//((Relation)binExpr.right()).name());
				if(e instanceof IntToExprCast){
					if(replace == Replace.INTCOMPARISON){
						
						final IntExpression i =(IntExpression) ((IntToExprCast)e).intExpr();
						if(quantVariable != null)
							return (IntExpression)i.accept(new VariableReplacer(quantVariable));// binExpr.left());
					}
					else if(replace == Replace.COMPARISON){
						System.out.println("CHECK THIS WHEN IT COMES UP");
						if(quantVariable != null)
							return (Expression)e.accept(new VariableReplacer((Variable)binExpr.left()));
							//return (Expression)VariableReplacer.build(e, ((Variable)binExpr.left()));
					}
				}
			}
			return binExpr;
			
		}

		
		public NaryExpression visit(NaryExpression expr) {
			return null;
			
		}

		
		public Comprehension visit(Comprehension comprehension) {
			return null;
			
		}

		
		public IfExpression visit(IfExpression ifExpr) {
			return null;
			
		}

		
		public   ProjectExpression visit(ProjectExpression project) {
			return null;
			
		}

		
		public   IntToExprCast visit(IntToExprCast castExpr) {
			return null;
			
		}

		
		public   IntConstant visit(IntConstant intConst) {
			return intConst;
			
		}

		
		public   IfIntExpression visit(IfIntExpression intExpr) {
			return null;
			
		}

		
		public IntExpression visit(final ExprToIntCast intExpr) {
			if(replace == Replace.FALSE || intExpr.op() == ExprCastOperator.CARDINALITY)
				return intExpr;
			else
			{
				final Node n = intExpr.expression().accept(this);
				if(n instanceof IntExpression)
					return (IntExpression)n;
				else
					return intExpr;
							
			}	
		}

		
		public   NaryIntExpression visit(NaryIntExpression intExpr) {
			return null;
			
		}

		
		public BinaryIntExpression visit(BinaryIntExpression intExpr) {
			return new BinaryIntExpression((IntExpression)intExpr.left().accept(this), intExpr.op(), (IntExpression)intExpr.right().accept(this));
			
		}

		
		public   UnaryIntExpression visit(UnaryIntExpression intExpr) {
			return null;
			
		}

		
		public   SumExpression visit(SumExpression intExpr) {
			return null;
			
		}

		
		public Formula visit(IntComparisonFormula n) {
			//if(n.reduction == Reduction.INTCOMPARISON ){
			if(IntExprReduction.reductions_intComparison.contains(n)){
				replace = Replace.INTCOMPARISON;
				final IntExpression newIE = (IntExpression)((IntExpression)n.left().accept(this));
				final Formula newFormula = newIE.compare(n.op(), (IntExpression)n.right().accept(this));
				replace = Replace.FALSE;
				return newFormula;
			} else {
				return null;
			}
		}

		public QuantifiedFormula visit(final QuantifiedFormula qf) {	
			final Decls decls = qf.decls();
			final Decl d = decls.get(0);
			multiplicity = d.multiplicity().toString();
			quantVariable = d.variable();
			quantExpression = d.expression().toString();
			final Formula f2 = (Formula)qf.formula().accept(this);
			final QuantifiedFormula qf2 = (QuantifiedFormula) f2.quantify(qf.quantifier(), decls);
//			QuantifiedFormula q = new QuantifiedFormula(quantFormula.quantifier(), 
//					quantFormula.decls(), (Formula)quantFormula.formula().accept(this));
			multiplicity = null;
			quantVariable = null;
			quantExpression = null;
			return qf2;
		}
		
		public NaryFormula visit(NaryFormula formula) {
			return null;
			
		}
		
		public BinaryFormula visit(final BinaryFormula bf) {
			final Formula left = (Formula)bf.left().accept(this);
			final Formula right = (Formula)bf.right().accept(this);
			return (BinaryFormula) left.compose(bf.op(), right);
//			return new BinaryFormula(left, binFormula.op(), right);
		}

		
		public NotFormula visit(NotFormula not) {
			return null;
			
		}

		
		public ConstantFormula visit(ConstantFormula constant) {
			return null;
			
		}

		
		public Formula visit(final ComparisonFormula n) {
			//if(n.reduction == Reduction.DELETE){ //|| n.reduction == Reduction.INTCONSTANT){
			if(IntExprReduction.reductions_delete.contains(n)){
				return Formula.constant(true);
			}
			//else if(n.reduction == Reduction.INTCONSTANT)
			else if(IntExprReduction.reductions_intConstant.contains(n)){
				return n;
			}
			//else if(n.reduction == Reduction.EQUALEXPRESSIONS){
			else if(IntExprReduction.reductions_equalExpressions.contains(n)){
				final ComparisonFormula tempForm = (ComparisonFormula)n;
				//return new ComparisonFormula(tempForm.right(), tempForm.op(), tempForm.equalExpression);
				return tempForm.right().compare(tempForm.op(), IntExprReduction.equalExpressions.get(tempForm));  
			}
			//else if( n.reduction == Reduction.COMPARISON)
			else if(IntExprReduction.reductions_comparison.contains(n)){
				replace = Replace.COMPARISON;
				//newFormula = new ComparisonFormula((Expression)n.left().accept(this), n.op(), (Expression)n.right());
				final Formula newFormula = ((Expression)n.left().accept(this)).compare(
						n.op(),
						(Expression)n.right());
				replace = Replace.FALSE;
				return newFormula;
			}
			else {
				return n;
			}
		}

		
		public   MultiplicityFormula visit(MultiplicityFormula multFormula) {
			return multFormula;
		}

		public RelationPredicate visit(RelationPredicate predicate) {
			return null;
		}

}