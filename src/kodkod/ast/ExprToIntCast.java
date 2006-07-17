/**
 * 
 */
package kodkod.ast;


import kodkod.ast.visitor.ReturnVisitor;
import kodkod.ast.visitor.VoidVisitor;

/**
 * An {@link kodkod.ast.IntExpression } representing the 
 * cardinality of an {@link kodkod.ast.Expression} or the 
 * sum of all the integer atoms contained in the expression.
 * @specfield expression: Expression
 * @specfield op: Op
 * @invariant children = expression
 * @author Emina Torlak
 */
public final class ExprToIntCast extends IntExpression {
	private final Expression expression;
	private final Operator op; 
	private final int hashcode;
	/**  
	 * Constructs a new cardinality expression.
	 * 
	 * @effects this.expression' = expression && this.op' = op
	 * @throws NullPointerException - expression = null || op = null
	 * @throws IllegalArgumentException - op = SUM && child.arity != 1
	 */
	ExprToIntCast(Expression child, Operator op) {
		if (!op.applicable(child.arity())) 
			throw new IllegalArgumentException("cannot apply " + op + " to " + child);
		this.expression = child;
		this.op = op;
		this.hashcode = op.hashCode() + child.hashCode();
	}
	
	/**
	 * Returns this.expression.
	 * @return this.expression
	 */
	public Expression expression() {return expression;}

	/**
	 * Returns this.op.
	 * @return this.op
	 */
	public Operator op() { return op; } 
	
	/**
	 * Returns true of o is a ExprToIntCast with the
	 * same tree structure as this.
	 * @return o in ExprToIntCast && o.op.equals(this.op) && o.expression.equals(this.expression)
	 */
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ExprToIntCast)) return false;
		ExprToIntCast that = (ExprToIntCast)o;
		return op.equals(that.op) && expression.equals(that.expression);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return op + "("+expression.toString()+")";
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() { 
		return  hashcode;
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.ast.IntExpression#accept(kodkod.ast.visitor.ReturnVisitor)
	 */
	@Override
	public <E, F, D, I> I accept(ReturnVisitor<E, F, D, I> visitor) {
		return visitor.visit(this);
	}
	
    
	/**
	 * {@inheritDoc}
	 * @see kodkod.ast.IntExpression#accept(kodkod.ast.visitor.VoidVisitor)
	 */
	@Override
	public void accept(VoidVisitor visitor) {
		visitor.visit(this);
	}
	
	/**
	 * Represents an expression 'cast' operator.
	 */
	public static enum Operator {
		/** the cardinality operator */
		CARDINALITY {
			/**
			 * {@inheritDoc}
			 * @see java.lang.Object#toString()
			 */
			public String toString() { 
				return "#";
			}
		}, 
		/** the sum operator */
		SUM {
			/**
			 * {@inheritDoc}
			 * @see java.lang.Object#toString()
			 */
			public String toString() { 
				return "sum";
			}
			
			/** 
			 * Returns true if arity = 1.
			 * @return arity = 1
			 */
			boolean applicable(int arity) { return arity==1; }
		};
		
		/**
		 * Returns true if this operator is applicable to an
		 * expression of the given arity.
		 * @return true if this operator is applicable to an
		 * expression of the given arity.
		 */
		boolean applicable(int arity) { return true; }
	}
	
}