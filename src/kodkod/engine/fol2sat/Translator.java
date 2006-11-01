/*
 * Translator.java
 * Created on Jul 5, 2005
 */
package kodkod.engine.fol2sat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Node;
import kodkod.engine.bool.BooleanConstant;
import kodkod.engine.bool.BooleanFactory;
import kodkod.engine.bool.BooleanFormula;
import kodkod.engine.bool.BooleanMatrix;
import kodkod.engine.bool.BooleanValue;
import kodkod.engine.bool.Operator;
import kodkod.engine.satlab.SATSolver;
import kodkod.engine.settings.Options;
import kodkod.engine.settings.Reporter;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.util.ints.IntSet;


/** 
 * Translates a formula in first order logic, represented as an
 * {@link kodkod.ast.Formula abstract syntax tree}, into a 
 * {@link kodkod.engine.satlab.SATSolver cnf formula}.
 * @author Emina Torlak 
 */
public final class Translator {
	
	/**
	 * Translates the given formula using the specified bounds and options.
	 * @return a Translation whose solver is a SATSolver instance initalized with the 
	 * CNF representation of the given formula, with respect to the given bounds.
	 * @throws TrivialFormulaException - the given formula is reduced to a constant during translation
	 * (i.e. the formula is trivially (un)satisfiable).
	 * @throws NullPointerException - any of the arguments are null
	 * @throws UnboundLeafException - the formula refers to an undeclared variable or a relation not mapped by the given bounds.
	 * @throws HigherOrderDeclException - the formula contains a higher order declaration that cannot
	 * be skolemized, or it can be skolemized but options.skolemize is false.
	 */
	@SuppressWarnings("unchecked")
	public static Translation translate(Formula formula, Bounds bounds, Options options) throws TrivialFormulaException {
		final Reporter reporter = options.reporter(); // grab the reporter
		
		// extract structural information about the formula (i.e. syntactically shared internal nodes)
		reporter.collectingStructuralInfo();
		AnnotatedNode<Formula> annotated = new AnnotatedNode<Formula>(formula);
				
		// copy the bounds and remove bindings for unused relations/ints
		bounds = bounds.clone();
		bounds.relations().retainAll(annotated.relations());
		if (!annotated.usesIntBounds()) bounds.ints().clear();
		
		// break symmetries on total orders and acyclic relations
		SymmetryBreaker breaker = new SymmetryBreaker(bounds, options);
		breaker.breakPredicateSymmetries(annotated.predicates());
			
		// skolemize
		if (options.skolemDepth()>=0) {
			annotated = Skolemizer.skolemize(annotated, bounds, options);
		} 
		
		// translate to boolean, checking for trivial (un)satisfiability
		LeafInterpreter interpreter = LeafInterpreter.exact(bounds, options);
		
		reporter.translatingToBoolean(formula, bounds);
		final Map<Node, IntSet> varUsage;
		BooleanValue circuit;
		if (options.trackVars()) {
			final TrackingCache c = new TrackingCache(annotated);
			circuit = FOL2BoolTranslator.translate(annotated,  interpreter, c, Environment.EMPTY);
			if (circuit.op()==Operator.CONST) {
				final Formula redux = TrivialFormulaReducer.reduce(annotated,breaker.broken(),c.trueFormulas(),c.falseFormulas());
				throw new TrivialFormulaException(redux, (BooleanConstant)circuit, bounds);
			}
			varUsage = c.varUsage();
		} else {
			final TranslationCache c = options.interruptible() ? new InterruptibleCache(annotated) : new TranslationCache(annotated);
			circuit = FOL2BoolTranslator.translate(annotated,  interpreter, c, Environment.EMPTY);
			if (circuit.op()==Operator.CONST) {
				throw new TrivialFormulaException(formula, (BooleanConstant)circuit, bounds);
			}
			varUsage = new LinkedHashMap<Node, IntSet>();
		}
		
		varUsage.putAll(interpreter.vars());
		annotated = null; // release structural information

		BooleanFactory factory = interpreter.factory();
		final int numPrimaryVariables = factory.numberOfVariables();
		
		// break lex symmetries
		reporter.breakingSymmetries();
		circuit = factory.and(circuit, breaker.breakLexSymmetries(interpreter));
	
		interpreter = null; breaker = null; // release the allocator and symmetry breaker

		// flatten
		if (options.flatten()) {
			reporter.flattening((BooleanFormula)circuit);
			factory.clear(); // remove everything but the variables from the factory
			circuit = BooleanFormulaFlattener.flatten((BooleanFormula)circuit, factory);
			factory = null; // release the factory itself
		}
		
		if (circuit.op()==Operator.CONST) {
			throw new TrivialFormulaException(formula, (BooleanConstant)circuit, bounds);
		}
		
		// translate to cnf and return the translation
		reporter.translatingToCNF((BooleanFormula)circuit);
		final SATSolver cnf = Bool2CNFTranslator.definitional((BooleanFormula)circuit, options.solver(), numPrimaryVariables);
		return new Translation(cnf, bounds, Collections.unmodifiableMap(varUsage), numPrimaryVariables);

	}
	
	
	/**
	 * Evaluates the given formula to a BooleanConstant using the provided instance and options.  
	 * 
	 * @return a BooleanConstant that represents the value of the formula.
	 * @throws NullPointerException - formula = null || instance = null || options = null
	 * @throws UnboundLeafException - the formula refers to an undeclared variable or a relation not mapped by the instance
	 * @throws HigherOrderDeclException - the formula contains a higher order declaration
	 */
	public static BooleanConstant evaluate(Formula formula, Instance instance, Options options) {
		return (BooleanConstant) 
		 FOL2BoolTranslator.translate(new AnnotatedNode<Formula>(formula), 
				 LeafInterpreter.exact(instance, options));
	}
	
	/**
	 * Evaluates the given expression to a BooleanMatrix using the provided instance and options.
	 * 
	 * @return a BooleanMatrix whose TRUE entries represent the tuples contained by the expression.
	 * @throws NullPointerException - formula = null || instance = null || options = null
	 * @throws UnboundLeafException - the expression refers to an undeclared variable or a relation not mapped by the instance
	 * @throws HigherOrderDeclException - the expression contains a higher order declaration
	 */
	public static BooleanMatrix evaluate(Expression expression,Instance instance, Options options) {
		return (BooleanMatrix) 
		 FOL2BoolTranslator.translate(new AnnotatedNode<Expression>(expression),
				 LeafInterpreter.exact(instance, options));
	}

}


