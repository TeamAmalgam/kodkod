package kodkod.intelision;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import kodkod.ast.ComparisonFormula;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntComparisonFormula;
import kodkod.ast.IntConstant;
import kodkod.ast.IntToExprCast;
import kodkod.ast.Node.Reduction;
import kodkod.ast.Relation;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.config.Options;
import kodkod.engine.satlab.SATFactory;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.Universe;

public final class IntExprReduction {
	HashSet<String> vars = new HashSet<String>();
	public HashSet<ComparisonFormula> comparisonNodes = new HashSet<ComparisonFormula>();
	public HashSet<IntComparisonFormula> intComparisonNodes = new HashSet<IntComparisonFormula>();
	public HashMap<String, Expression> swapAnswerPairs= new HashMap<String, Expression>();
	public HashSet<String> bogusVariables = new HashSet<String>();//Relation>(); 
	
	Formula modifiedTree;
	public Formula[] oldFormulas;
	public boolean[] createNewTree;
	
	public Formula[] reduceIntExpressions(Formula...formulas)
	{
		createNewTree = new boolean[formulas.length];
		for(int i = 0; i < formulas.length; i++) 
		{
			Formula f = formulas[i];
			EqualityFinder equalityFinder = new EqualityFinder();
			f.accept(equalityFinder);
			HashSet<ComparisonFormula> currentComparisonNodes = equalityFinder.comparisonNodes;
			HashSet<IntComparisonFormula> currentInequalityNodes = equalityFinder.intComparisonNodes;
			createNewTree[i] = !(currentComparisonNodes.isEmpty() && currentInequalityNodes.isEmpty());
			comparisonNodes.addAll(currentComparisonNodes);
			intComparisonNodes.addAll(currentInequalityNodes);
		}
		for(ComparisonFormula cf : comparisonNodes){
			//the "independent side" of the comparison formula
			Expression arithmetic_expression;
			if(cf.assignmentOnLeft){
				arithmetic_expression = cf.right();
			}
			else{
				arithmetic_expression = cf.left();
			}
			cf.reduction = Reduction.DELETE;
			//check if arithmetic_expression is a constant
			if(arithmetic_expression instanceof IntToExprCast)
				if(((IntToExprCast)arithmetic_expression).intExpr() instanceof IntConstant)
				{
					cf.reduction = Reduction.INTCONSTANT;
					swapAnswerPairs.put(cf.answer, arithmetic_expression);
					continue;
				}
			
			if(swapAnswerPairs.containsKey(cf.answer)){
				cf.equalExpression = swapAnswerPairs.get(cf.answer);
				cf.reduction=Reduction.EQUALEXPRESSIONS;
			}
			swapAnswerPairs.put(cf.answer, arithmetic_expression);
			bogusVariables.add(cf.answer);
		}
		for(IntComparisonFormula icf : intComparisonNodes){
			icf.reduction = Reduction.INTCOMPARISON;
		}
		int count = 0;

		
		
		for(int i = 0; i < formulas.length; i++)
			if(createNewTree[i]){
				Formula f = formulas[i];
				
				//ArithmeticStorageElider bt = new ArithmeticStorageElider(f, swapAnswerPairs);
				ArithmeticStorageElider elider = new ArithmeticStorageElider(swapAnswerPairs);
				modifiedTree = (Formula)f.accept(elider);
				//if(!f.toString().equals(newTree.toString()))
				//	System.out.print("count"+count);
				formulas[i] = modifiedTree;
			}
		
		return formulas;
	}
	//XXX this was added in moolloy
	public Formula[] resume(Formula...formulas){
		return reduceIntExpressions(formulas);
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
		/*Iterator<Relation> itr = sol.instance().relationTuples().keySet().iterator();
		while(itr.hasNext()){
			Relation r = itr.next();
			System.out.println(r + ":: " +sol.instance().relationTuples().get(r));
		}*/
		System.out.println(sol.toString());
		
		System.out.println(Recompute.recompute(sol, factory, comparisonNodes, bogusVariables, bitwidth).toString());
	}
	
	
	
}
