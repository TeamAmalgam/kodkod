package kodkod.intelision;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;

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
import kodkod.instance.Tuple;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;
import kodkod.util.collections.IdentityHashSet;
import kodkod.util.ints.IntSet;
import kodkod.util.ints.IntTreeSet;
import kodkod.util.ints.IntIterator;

public final class IntExprReduction {
	//NOTES for changing between experiments:
	//  get rid of int/next relation around line 275
	//  change the intconsts around line 250
	//  comment lines where f is modified in incremental solver
	//  uncomment powerset stuff
	
	private HashSet<ComparisonFormula> comparisonNodes = new HashSet<ComparisonFormula>();
	private HashSet<IntComparisonFormula> intComparisonNodes = new HashSet<IntComparisonFormula>();
	private HashMap<String, Expression> swapAnswerPairs= new HashMap<String, Expression>();
	private HashSet<String> bogusVariables = new HashSet<String>();//Relation>(); 
	
	public ArrayList<String> equalityIntConstants = new ArrayList<String>();
	public ArrayList<String> claferMOOconstants = new ArrayList<String>();
	public ArrayList<String> uniqueEqualityIntConstants = new ArrayList<String>();
	
	private Formula modifiedTree;
	private boolean[] createNewTree;
	
	public Universe recreatedUniverse = null;
	public Bounds recreatedBounds = null;
	
	// TODO: make these not static
	IdentityHashSet<Node> reductions_delete;
	private IdentityHashSet<Node> reductions_replace;
	private IdentityHashSet<Node> reductions_swapVariables;
	IdentityHashSet<Node> reductions_comparison;
	IdentityHashSet<Node> reductions_equalExpressions;
	IdentityHashSet<Node> reductions_intComparison;
	IdentityHashSet<Node> reductions_intConstant;
	
	//***** is there any problem with using a regular hashmap?
	IdentityHashMap<Node, String> answers;
	//can the second type param be changed to Expression?
	IdentityHashMap<Node, Node> variables;
	IdentityHashMap<Node, Expression> equalExpressions;
	
	public IntExprReduction() {
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
	public void addReduction(Node n, IdentityHashSet<Node> set){
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
			final EqualityFinder equalityFinder = new EqualityFinder(this);
			f.accept(equalityFinder);
			final IdentityHashSet<ComparisonFormula> currentComparisonNodes = equalityFinder.comparisonNodes;
			final IdentityHashSet<IntComparisonFormula> currentInequalityNodes = equalityFinder.intComparisonNodes;
			createNewTree[i] = !(currentComparisonNodes.isEmpty() && currentInequalityNodes.isEmpty());
			comparisonNodes.addAll(currentComparisonNodes);
			intComparisonNodes.addAll(currentInequalityNodes);
		}
		//System.out.println("Comp Nodes: " + comparisonNodes + " " + intComparisonNodes);
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
	

	public List<String> allSums(List<String> ints) {
		System.out.println("Finding all sums of: ");
		IntSet masterSet = new IntTreeSet();
		IntSet[] sets = new IntSet[ints.size()]; // Each set contains the sums with the index + 1 number of terms.

		sets[0] = new IntTreeSet();
		for (String i : ints) {
			System.out.println("\t" + i);
			sets[0].add(Integer.parseInt(i));
			masterSet.add(Integer.parseInt(i));
		}

		for (int i = 1; i < sets[0].size(); i += 1) {
			System.out.println("Iteration " + i);
			IntSet currentSet = new IntTreeSet();

			IntIterator beginningIterator = sets[0].iterator();

			while (beginningIterator.hasNext()) {
				int j = beginningIterator.next();
				IntIterator lastIterator = sets[i - 1].iterator();
				while (lastIterator.hasNext()) {
					int k = lastIterator.next();

					currentSet.add(j + k);
					masterSet.add(j + k);
				}
			}

			sets[i] = currentSet;
		}

		ArrayList<String> toReturn = new ArrayList<String>(masterSet.size());
		IntIterator masterIterator = masterSet.iterator();
		while (masterIterator.hasNext()) {
			toReturn.add(masterIterator.next() + "");
		}

		return toReturn;
	}

	public void getEqualityConstants(){
		//System.out.println("Get Equality Ints");
		for(ComparisonFormula f : comparisonNodes){
			if(f.left() instanceof IntToExprCast && ((IntToExprCast)f.left()).intExpr() instanceof IntConstant){
				IntConstant c = (IntConstant)((IntToExprCast)f.left()).intExpr();
				equalityIntConstants.add(c.toString());
			}
			else if(f.right() instanceof IntToExprCast && ((IntToExprCast)f.right()).intExpr() instanceof IntConstant){
				IntConstant c = (IntConstant)((IntToExprCast)f.right()).intExpr();
				equalityIntConstants.add(c.toString());
			}	
		}
		if(!equalityIntConstants.contains("0"))
			equalityIntConstants.add("0");
		HashSet<String> hs = new HashSet<String>();
		hs.addAll(equalityIntConstants);
		uniqueEqualityIntConstants.addAll(hs);
		ArrayList<Integer> intList = new ArrayList<Integer>();
		for(String s : equalityIntConstants)
			intList.add(Integer.parseInt(s));

		System.out.println("Calculating moo ints.");
		claferMOOconstants = (ArrayList<String>)allSums(equalityIntConstants);
		System.out.println("Found " + claferMOOconstants.size() + " ints");
		/*
		Iterator<BitSet> itr = PowerSet.iteratePowerSet(equalityIntConstants.size());
		
		while(itr.hasNext()){
			BitSet b = itr.next();
			int sum = 0;
			for(int i = 0; i < b.length(); i++){
				if(b.get(i))
					sum += Integer.parseInt(equalityIntConstants.get(i));
			}
			claferMOOconstants.add(sum + "");
			
		}
		*/
		
		
		//System.out.println("PowerSet");
		/*
		List<List<Integer>> powerList = powerSet(intList);
		hs = new HashSet<String>();
		for(List<Integer> i : powerList){
			int sum = 0;
			for(Integer j : i)
				sum += j;
			hs.add(sum+"");
		}
		claferMOOconstants.addAll(hs);
		//System.out.println(claferMOOconstants);
		 
		 */
	}
	
	
	
	public static boolean isInteger(String s) {
	    try { 
	        Integer.parseInt(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    }
	    // only got here if we didn't return false
	    return true;
	}
	
	
	
	public TupleSet createTupleSet(TupleSet old, TupleFactory factory, Relation r, ArrayList<String> experimentInts){
		TupleSet tupleSet = factory.noneOf(r.arity());
		Iterator<Tuple> tupleitr = old.iterator();
		while(tupleitr.hasNext()){
			Tuple t = tupleitr.next();
			boolean elidedTuple = false;
			List<String> atomList = new ArrayList<String>();
			for(int i = 0; i < t.arity(); i++){
				String atomStr = t.atom(i).toString();
				atomList.add(atomStr);
				if(isInteger(atomStr)){
					if(!experimentInts.contains(atomStr)){
						//System.out.println("Found elided");
						elidedTuple = true;
						break;
					}
				}
			}
			//create tuple with new universe factory
			if(!elidedTuple){
				Tuple tuple = factory.tuple(atomList);
				tupleSet.add(tuple);
			}
		}
		return tupleSet;
	}
	
	public void recreateUniverseAndBounds(Bounds oldBounds){
		//System.out.println("IN UTILITY");
		//System.out.println(equalityIntConstants);
		//System.out.println("BOUNDS");
		//System.out.println(b);
		//System.out.println("UNIVERSE");
		//System.out.println(b.universe());
		
		ArrayList<String> experimentInts = this.claferMOOconstants;
		//System.out.println(this.claferMOOconstants);
		//ArrayList<String> experimentInts = this.equalityIntConstants;
		
		Universe oldUniverse = oldBounds.universe();
		Iterator<Object> itr = oldUniverse.iterator();
		ArrayList<Object> newUniverseList = new ArrayList<Object>();
		while(itr.hasNext()){
			Object atom = itr.next();
			//System.out.println(atom);
			if(isInteger(atom.toString())){
				if(experimentInts.contains(atom.toString()) && !newUniverseList.contains(atom.toString())){
					newUniverseList.add(atom);
				}
			}
			else
				newUniverseList.add(atom);
		}
		//System.out.println("New Universe: " + newUniverseList);
		Universe newUniverse = new Universe(newUniverseList);
		Bounds newBounds = new Bounds(newUniverse);
		TupleFactory factory = newUniverse.factory();
		//System.out.println("Relations");
		for(Relation r : oldBounds.relations()){
			//System.out.println(r);
			if(r.toString().contains("Int/next")){
				//System.out.println("Eliding Int/next");
				continue;
			}
			//System.out.println("relation");
			//System.out.println(r);
			//System.out.println(oldBounds.lowerBound(r));
			TupleSet oldUpper = oldBounds.upperBound(r);
			TupleSet upperTupleSet = createTupleSet(oldUpper, factory, r, experimentInts);
			TupleSet oldLower = oldBounds.lowerBound(r);
			TupleSet lowerTupleSet = createTupleSet(oldLower, factory, r, experimentInts);
			newBounds.bound(r, lowerTupleSet, upperTupleSet);
			//if(r.toString().contains("Int/next")){
			//	System.out.println("Eliding Int/next");
			//	System.out.println(upperTupleSet);
			//	System.out.println(lowerTupleSet);
			//}
		}
		
		for(String atom : experimentInts){
			int i = Integer.parseInt(atom);
			newBounds.boundExactly(i, factory.range(factory.tuple(atom),factory.tuple(atom)));
		}
		//System.out.println("NEW BOUNDS");
		//System.out.println(newBounds);
		this.recreatedBounds = newBounds;
		this.recreatedUniverse = newUniverse;
		//System.out.println(newBounds);
		//System.out.println(newUniverse);
		
		
	}
	
	
	public Formula reduceFormula(Formula f) {
		ArithmeticStorageElider elider = new ArithmeticStorageElider(this, swapAnswerPairs);
		return (Formula)f.accept(elider);
	}

	public Solution recompute(Solution soln, Universe universe, Options options) {
		return new Recompute(this).recompute(soln, universe.factory(), comparisonNodes, bogusVariables, options);
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
		
		System.out.println(new Recompute(this).recompute(sol, factory, comparisonNodes, bogusVariables, solver.options()).toString());
	}
	
	
	
}
