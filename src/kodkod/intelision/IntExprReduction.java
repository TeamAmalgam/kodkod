package kodkod.intelision;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;

import kodkod.ast.BinaryExpression;
import kodkod.ast.ComparisonFormula;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntComparisonFormula;
import kodkod.ast.IntConstant;
import kodkod.ast.IntToExprCast;
import kodkod.ast.Node;
import kodkod.ast.Relation;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.config.Options;
import kodkod.engine.satlab.SATFactory;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.Universe;
import kodkod.util.collections.IdentityHashSet;

public final class IntExprReduction {
	private HashSet<ComparisonFormula> comparisonNodes = new HashSet<ComparisonFormula>();
	private HashSet<IntComparisonFormula> intComparisonNodes = new HashSet<IntComparisonFormula>();
	private HashMap<String, Expression> swapAnswerPairs= new HashMap<String, Expression>();
	private HashSet<String> bogusVariables = new HashSet<String>();//Relation>(); 
	
	private Formula modifiedTree;
	private boolean[] createNewTree;
	
	// TODO: make these not static
	static IdentityHashSet<Node> reductions_delete = new IdentityHashSet<Node>();
	private static IdentityHashSet<Node> reductions_replace = new IdentityHashSet<Node>();
	private static IdentityHashSet<Node> reductions_swapVariables = new IdentityHashSet<Node>();
	static IdentityHashSet<Node> reductions_comparison = new IdentityHashSet<Node>();
	static IdentityHashSet<Node> reductions_equalExpressions = new IdentityHashSet<Node>();
	static IdentityHashSet<Node> reductions_intComparison = new IdentityHashSet<Node>();
	static IdentityHashSet<Node> reductions_intConstant = new IdentityHashSet<Node>();
	
	//***** is there any problem with using a regular hashmap?
	static IdentityHashMap<Node, String> answers = new IdentityHashMap<Node, String>();
	//can the second type param be changed to Expression?
	static IdentityHashMap<Node, Node> variables = new IdentityHashMap<Node, Node>();
	static IdentityHashMap<Node, Expression> equalExpressions = new IdentityHashMap<Node, Expression>();
	
	public static void clearStatics() {
		reductions_delete = new IdentityHashSet<Node>();
		reductions_replace = new IdentityHashSet<Node>();
		reductions_swapVariables = new IdentityHashSet<Node>();
		reductions_comparison = new IdentityHashSet<Node>();
		reductions_equalExpressions = new IdentityHashSet<Node>();
		reductions_intComparison = new IdentityHashSet<Node>();
		reductions_intConstant = new IdentityHashSet<Node>();

		answers = new IdentityHashMap<Node, String>();
		variables = new IdentityHashMap<Node, Node>();
		equalExpressions = new IdentityHashMap<Node, Expression>();
	}

	//adds AST node to proper reductions set, making sure to remove it from any others first
	//the removal checks can be deleted eventually
	private void addReduction(Node n, IdentityHashSet<Node> set){
		reductions_delete.remove(n);
		reductions_replace.remove(n);
		reductions_swapVariables.remove(n);
		reductions_comparison.remove(n);
		reductions_equalExpressions.remove(n);
		reductions_intComparison.remove(n);
		reductions_intConstant.remove(n);
		set.add(n);
	}
	
	public Formula[] reduceIntExpressions(Formula...formulas)
	{
		createNewTree = new boolean[formulas.length];
		for(int i = 0; i < formulas.length; i++) 
		{
			final Formula f = formulas[i];
			final EqualityFinder equalityFinder = new EqualityFinder();
			f.accept(equalityFinder);
			final IdentityHashSet<ComparisonFormula> currentComparisonNodes = equalityFinder.comparisonNodes;
			final IdentityHashSet<IntComparisonFormula> currentInequalityNodes = equalityFinder.intComparisonNodes;
			createNewTree[i] = !(currentComparisonNodes.isEmpty() && currentInequalityNodes.isEmpty());
			comparisonNodes.addAll(currentComparisonNodes);
			intComparisonNodes.addAll(currentInequalityNodes);
		}
		for(final ComparisonFormula cf : comparisonNodes){
			//the "independent side" of the comparison formula
			final Expression arithmetic_expression;
			if(cf.right() instanceof BinaryExpression || cf.right() instanceof Relation){
				arithmetic_expression = cf.left();
			}
			else{
				arithmetic_expression = cf.right();
			}
			addReduction(cf, reductions_delete);
			//check if arithmetic_expression is a constant
			if(arithmetic_expression instanceof IntToExprCast)
				if(((IntToExprCast)arithmetic_expression).intExpr() instanceof IntConstant)
				{
					addReduction(cf, reductions_intConstant);
					swapAnswerPairs.put(answers.get(cf), arithmetic_expression);
					continue;
				}
			
			if(swapAnswerPairs.containsKey(answers.get(cf))){
				equalExpressions.put(cf, (Expression) swapAnswerPairs.get(answers.get(cf)));
				addReduction(cf, reductions_equalExpressions);
			}
			swapAnswerPairs.put(answers.get(cf), arithmetic_expression);
			bogusVariables.add(answers.get(cf));
			
		}
		for(final IntComparisonFormula icf : intComparisonNodes){
			addReduction(icf, reductions_intComparison);
		}

		
		
		for(int i = 0; i < formulas.length; i++)
			if(createNewTree[i]){
				final Formula f = formulas[i];

				modifiedTree = reduceFormula(f);
				formulas[i] = modifiedTree;
			}
		
		return formulas;
	}
	
	public Formula reduceFormula(Formula f) {
		ArithmeticStorageElider elider = new ArithmeticStorageElider(swapAnswerPairs);
		return (Formula)f.accept(elider);
	}

	public Solution recompute(Solution soln, Universe universe, Options options) {
		Recompute.clearStatics();
		return Recompute.recompute(soln, universe.factory(), comparisonNodes, bogusVariables, options);
	}

	public void solve(Formula formula, Bounds bounds, TupleFactory factory, Universe universe, int bitwidth)
	{
		Solver solver = new Solver();
		solver.options().setSolver(SATFactory.DefaultSAT4J);
		solver.options().setBitwidth(bitwidth);
		solver.options().setIntEncoding(Options.IntEncoding.TWOSCOMPLEMENT);
		solver.options().setSymmetryBreaking(20);
		solver.options().setSkolemDepth(0);
		System.out.println("Solving...");
		System.out.flush();
		Solution sol = solver.solve(formula,bounds);

		System.out.println(sol.toString());
		
		System.out.println(Recompute.recompute(sol, factory, comparisonNodes, bogusVariables, solver.options()).toString());
	}
	
	
	
}
