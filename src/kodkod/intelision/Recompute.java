package kodkod.intelision;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import kodkod.ast.BinaryExpression;
import kodkod.ast.BinaryIntExpression;
import kodkod.ast.ComparisonFormula;
import kodkod.ast.ExprToIntCast;
import kodkod.ast.Expression;
import kodkod.ast.IntConstant;
import kodkod.ast.IntToExprCast;
import kodkod.ast.Node;
import kodkod.ast.Relation;
import kodkod.ast.SumExpression;
import kodkod.ast.UnaryExpression;
import kodkod.ast.Variable;
import kodkod.engine.Solution;
import kodkod.engine.Evaluator;
import kodkod.engine.config.Options;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.Tuple;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;
import kodkod.util.ints.IndexedEntry;
import kodkod.util.ints.IntIterator;
import kodkod.util.ints.IntSet;


class Recompute {

	Tuple mostRecentRight;
	IntExprReduction ier;
	HashSet<String> bogusVariables = new HashSet<String>();
	Map<Relation, TupleSet> relationTuples;
	HashSet<ComparisonFormula> formulas;
	TupleFactory factory;
	int bitwidth;
	ArrayList<Relation> bogusRelations = new ArrayList<Relation>();
	int numberOfType = 0;
	Evaluator evaluator;
	Options options;

	public Recompute(IntExprReduction ier) {
		this.ier = ier;
		bogusVariables = new HashSet<String>();
		relationTuples = null;
		formulas = null;
		factory = null;
		bitwidth = 0;
		bogusRelations = new ArrayList<Relation>();
		numberOfType = 0;
		evaluator = null;
	}
	
	public Solution recompute(Solution sol, TupleFactory factory, HashSet<ComparisonFormula> formulas, HashSet<String> bogusVariables, Options options){
		this.formulas = formulas;
		this.bogusVariables = bogusVariables;
		this.factory = factory;
		this.bitwidth = options.bitwidth();
		this.options = options;

		ArrayList<ArrayList<TemporaryTuple>> tempTuples = new ArrayList<ArrayList<TemporaryTuple>>();
		if(sol.outcome() == Solution.Outcome.UNSATISFIABLE)
			return sol;
		

		Instance in = sol.instance();
		this.evaluator = new Evaluator(in, options);
		relationTuples =  in.relationTuples();
		
		for(ComparisonFormula cf: formulas)
		{
			Expression expr;
			
			if(!ier.reductions_delete.contains(cf))
				continue;
			TupleSet ts = relationTuples.get(ier.variables.get(cf));
			ArrayList<TemporaryTuple> temps = new ArrayList<TemporaryTuple>();
			if(ts != null){
				Iterator<Tuple> itr = ts.iterator();
				while(itr.hasNext()){
					Tuple tuple = itr.next();
					Relation rightMostRelation = null;
					Tuple rightMostTuple = null;
					if(cf.right() instanceof BinaryExpression || cf.right() instanceof Relation){
						rightMostRelation = getRightMostRelation((BinaryExpression)cf.right());
						rightMostTuple = getRightMostTuple(cf.right(), tuple);
						expr = cf.left();
					}
					else{
						rightMostRelation = getRightMostRelation((BinaryExpression)cf.left());
						rightMostTuple = getRightMostTuple(cf.left(), tuple);
						expr = cf.right();
					}
					if(rightMostTuple == null){
						temps.add(new TemporaryTuple("",0));
						continue;
					}
						
					bogusRelations.add(rightMostRelation);
					TemporaryTuple temp = computeByType(expr,tuple);
					temps.add(new TemporaryTuple(rightMostTuple.atom(0), temp.right()));
				}
			}
			else
			{
				Relation rightMostRelation = null;
				Tuple rightMostTuple = null;
				if(cf.right() instanceof BinaryExpression || cf.right() instanceof Relation){
					if(cf.right() instanceof BinaryExpression){
						rightMostRelation = getRightMostRelation((BinaryExpression)cf.right());
						rightMostTuple = getRightMostTuple(cf.right(), null);
						expr = cf.left();
					}
					else
					{
						rightMostRelation = (Relation)cf.right();
						rightMostTuple = getRightMostTuple(cf.right(), null);
						expr = cf.left();
					}
				}
				else{
					if(cf.left() instanceof BinaryExpression){
						rightMostRelation = getRightMostRelation((BinaryExpression)cf.left());
						rightMostTuple = getRightMostTuple(cf.left(), null);
						expr = cf.right();
					}
					else
					{
						rightMostRelation = (Relation)cf.left();
						rightMostTuple = getRightMostTuple(cf.left(), null);
						expr = cf.right();
					}
				}
				
				if(rightMostTuple == null){
					temps.add(new TemporaryTuple("",computeByType(expr,null).right));//XXX newly added test
					bogusRelations.add(rightMostRelation);//XXX newly added test
				}
				else{
					bogusRelations.add(rightMostRelation);
					TemporaryTuple temp = computeByType(expr,null);
					temps.add(new TemporaryTuple(rightMostTuple.atom(0), temp.right()));
				}
			
			}
			tempTuples.add(temps);
		}
		boundTemporaryTuplesToBitwidth(tempTuples);
		return computeNewSolution(sol, tempTuples);
	}

	//Creates the new solution with the corrected values for "dependent variables"
	//Essentially a copy of the old Solution, but with a new Instance
	//This method can probably simplified a lot, but I had trouble creating tuples, etc. due to Universe errors.. 
	public Solution computeNewSolution(Solution oldSolution, ArrayList<ArrayList<TemporaryTuple>> tempTuples)
	{
		Instance oldInstance = oldSolution.instance();
		Universe oldUniverse = oldInstance.universe();
		Iterator<Object> itr = oldUniverse.iterator();
		ArrayList<Object> newUniverseList = new ArrayList<Object>();
		ArrayList<String> newInts = new ArrayList<String>();
		//XXX this function changed for moolloy, should be ok
		
		
		
		//////////
		
		while(itr.hasNext())
			newUniverseList.add(itr.next());
		for(ArrayList<TemporaryTuple> a: tempTuples)
			for(TemporaryTuple t: a)
				if(!newUniverseList.contains(t.right()+""))
					newUniverseList.add(t.right()+"");
		
		Universe newUniverse = new Universe(newUniverseList);
		Instance newInstance = new Instance(newUniverse);
		Set<Relation> keys =  relationTuples.keySet();
		Iterator<Relation> itr2 = keys.iterator();
		
		while(itr2.hasNext()){
			Relation r = itr2.next();
			TupleSet ts = relationTuples.get(r);
			if(!bogusVariables.contains(r))
			{
				Iterator<Tuple> itr3 = ts.iterator();
				TupleSet newTupleSet = newUniverse.factory().noneOf(ts.arity());
				while(itr3.hasNext())
				{
					Tuple next = itr3.next();
					Tuple newTuple;
					if(ts.arity() == 1)
						newTuple = newUniverse.factory().tuple(next.atom(0));
          else if (ts.arity() == 2)
						newTuple = newUniverse.factory().tuple(next.atom(0), next.atom(1));
          else if (ts.arity() == 3)
            newTuple = newUniverse.factory().tuple(next.atom(0), next.atom(1), next.atom(2));
          else
            throw new RuntimeException("Not implemented yet for arity: " + ts.arity());

					newTupleSet.add(newTuple);
					
					
				}
				newInstance.add(r, newTupleSet);
			}
		}
		for(int i = 0; i < tempTuples.size(); i++)
		{
			TupleSet newTupleSet = newUniverse.factory().noneOf(bogusRelations.get(i).arity());
			for(int j = 0; j < tempTuples.get(i).size(); j++){
				if(bogusRelations.get(i).arity() == 2){
					newTupleSet.add(newUniverse.factory().tuple(tempTuples.get(i).get(j).left(), tempTuples.get(i).get(j).right()+""));
				}
				else if(bogusRelations.get(i).arity() == 1){
					newTupleSet.add(newUniverse.factory().tuple(tempTuples.get(i).get(j).right()+""));
				}
				else{
					System.out.println("BAD ARITY");
					System.exit(1);
				}
				newInstance.add(bogusRelations.get(i), newTupleSet);
				String newInt = (String)newTupleSet.iterator().next().atom(newTupleSet.arity()-1);
				newInts.add(newInt);
			}
		}
		
		//XXX hack this can probably be done differently
		for(String s: newInts)
			if(!newUniverseList.contains(s))
				newUniverseList.add(s);
		Universe lastUniverse = new Universe(newUniverseList);
		Bounds b = new Bounds(newUniverse);
		IntSet set = oldInstance.ints();
		IntIterator intitr = set.iterator();
		while(intitr.hasNext()){
			int i = intitr.next();
			b.boundExactly(i, newUniverse.factory().range(newUniverse.factory().tuple(i+"" ),newUniverse.factory().tuple( i+"" )));
		}	
		
		for(String s: newInts){
			b.boundExactly(Integer.parseInt(s), newUniverse.factory().range(newUniverse.factory().tuple(s),newUniverse.factory().tuple( s)));
		}

		for(IndexedEntry<TupleSet> t : b.intBounds()){
			newInstance.add(t.index(), t.value());
		}
		
		return new Solution(oldSolution.outcome(), oldSolution.stats(), newInstance, oldSolution.proof());
	}
	
	
	public static ArrayList<TemporaryTuple> concat(TupleSet ts, ArrayList<TemporaryTuple> tempInts)
	{
		if(tempInts.size() != ts.size())
		{
			System.out.println("error");
			System.exit(1);
		}
		Iterator<Tuple> itr = ts.iterator();
		Iterator<TemporaryTuple> itr2 = tempInts.iterator();
		ArrayList<TemporaryTuple> list = new ArrayList<TemporaryTuple>();
		while(itr.hasNext())
		{
			Tuple tuple = itr.next();
			TemporaryTuple temp = itr2.next();
			list.add(new TemporaryTuple(tuple.atom(0), temp.right()));
		}
		return list;
	}
	
	public TemporaryTuple computeByType(Node f, Tuple tuple){
		if(f instanceof IntToExprCast)
			return compute((IntToExprCast) f,  tuple);
		else if(f instanceof BinaryIntExpression)
			return compute((BinaryIntExpression) f,  tuple);
		else if(f instanceof BinaryExpression)
			return compute((BinaryExpression) f,  tuple);
		else if(f instanceof ExprToIntCast)
			return compute((ExprToIntCast) f,  tuple);
		else if(f instanceof IntConstant)
			return compute((IntConstant) f,  tuple);
		else if(f instanceof SumExpression)
			return compute((SumExpression) f, tuple);
		else{
			System.out.println("Error in recompute5");
			System.out.println(f);
			return null;
		}
	}
	
	public TemporaryTuple compute(IntToExprCast f, Tuple tuple)
	{
		return computeByType(f.intExpr(),  tuple);
	}
	
	public TemporaryTuple compute(BinaryExpression f, Tuple tuple)
	{
		Tuple rightMostTuple = getRightMostTuple(f, tuple);
		if(rightMostTuple == null)
			return new TemporaryTuple("",0);
		return new TemporaryTuple("", Integer.parseInt(rightMostTuple.atom(1).toString()));
	}
	
	public TemporaryTuple compute(ExprToIntCast f, Tuple tuple)
	{
		switch(f.op()){
		case CARDINALITY:
			System.out.println("Error in recompute");
			break;
		case SUM:
			return computeByType(f.expression(),  tuple);
		default:
			break;
		}
		return null;
	}
	
	public TemporaryTuple compute(IntConstant f, Tuple tuple)
 	{
		return new TemporaryTuple("",f.value());
	}
	
	public TemporaryTuple compute(BinaryIntExpression f, Tuple tuple)
	{
		TemporaryTuple l, r;
		switch(f.op()){
		case ABS:
			break;
		case AND:
			break;
		case DIVIDE:
			l = computeByType(f.left(),  tuple);
			r= computeByType(f.right(),  tuple);
			l.setRight(l.right() / r.right());
			return l;
		case MINUS:
			l = computeByType(f.left(),  tuple);
			r= computeByType(f.right(),  tuple);
			l.setRight(l.right() - r.right());
			return l;
		case MODULO:
			break;
		case MULTIPLY:
			l = computeByType(f.left(),  tuple);
			r= computeByType(f.right(),  tuple);
			l.setRight(l.right() * r.right());
			return l;
		case NEG:
			break;
		case NOT:
			break;
		case OR:
			break;
		case PLUS:
			l = computeByType(f.left(), tuple);
			r= computeByType(f.right(),  tuple);
			l.setRight(l.right() +r.right());
			return l;
		case SGN:
			break;
		case SHA:
			break;
		case SHL:
			break;
		case SHR:
			break;
		case XOR:
			break;
		default:
			break;
		
		}
		System.out.println("error in recompute2");
		return null;
	}

	public TemporaryTuple compute(SumExpression s, Tuple t) {
		//System.out.println("Evaluating " + s);
		//System.out.println("With instance: " + evaluator.instance());
		int result = evaluator.evaluate(s);
		//System.out.println("Evaluated " + s);
		//System.out.println("Got " + result);

		return new TemporaryTuple("", result);
	}
	
	public ArrayList<TemporaryTuple> composeArrayLists(ArrayList<TemporaryTuple> left, char op, ArrayList<TemporaryTuple> right)
	{
		ArrayList<TemporaryTuple> vals = new ArrayList<TemporaryTuple>();
		Iterator<TemporaryTuple> litr = left.iterator();
		Iterator<TemporaryTuple> ritr = right.iterator();
		int lor = 0;
		TemporaryTuple l=null,r=null;
		while(litr.hasNext() && ritr.hasNext())
		{
			if(lor == 0){
				l = litr.next();
				r = ritr.next();
			}
			else if(lor == 1)
				l = litr.next();
			else
				r = ritr.next();
			
			String[] ltemp = l.left.toString().split("\\$");
			String[] rtemp = r.left.toString().split("\\$");
			int lindex = Integer.parseInt(ltemp[ltemp.length-1]);
			int rindex = Integer.parseInt(rtemp[rtemp.length-1]);
			
			if(lindex == rindex){
				lor = 0;
				switch(op){
				case '+':
					vals.add(new TemporaryTuple("$" + lindex,l.right() + r.right()));
					break;
				case '-':
					vals.add(new TemporaryTuple("$" + lindex,l.right() - r.right()));
					break;
				case '*':
					vals.add(new TemporaryTuple("$" + lindex,l.right() * r.right()));
					break;
				case '/':
					vals.add(new TemporaryTuple("$" + lindex,l.right() / r.right()));
					break;
				default:
					System.out.println("Error in recompute");
				}
			}
			else if(lindex < rindex){
				lor = 1;
				vals.add(new TemporaryTuple("$"+ lindex, l.right()));
			}
			else{
				lor = 2;
				vals.add(new TemporaryTuple("$"+ rindex, r.right()));
			}
			
		}
		if(lor == 1)
			vals.add(r);
		if(lor == 2)
			vals.add(l);
		while(litr.hasNext())
			vals.add(litr.next());
		while(ritr.hasNext())
			vals.add(ritr.next());
		return vals;
	}
	
	public void boundTemporaryTuplesToBitwidth(ArrayList<ArrayList<TemporaryTuple>> tempTuples)
	{
		if(bitwidth == 32)
			return;
		for(ArrayList<TemporaryTuple> a : tempTuples)
			for(TemporaryTuple t: a)
				t.rebound(bitwidth);
	}
	
	public Relation getRightMostRelation(BinaryExpression b)
	{
		BinaryExpression b2 = b;
		while(!(b2.right() instanceof Relation))
			b2 = (BinaryExpression) b2.right();
		return (Relation)b2.right();
	}
	
	
	
	

	public Tuple getRightMostTuple(Expression b, Tuple s)
	{
		mostRecentRight = null;
		ArrayList<Tuple> t = getRightMostTuple_h(b,s);
		if(t == null)
			return null;
		return mostRecentRight;
	}
	
	
	public ArrayList<Tuple> getRightMostTuple_h(Expression b, Tuple s)
	{
		ArrayList<Tuple> tuples = new ArrayList<Tuple>();
		if(b == null)
			return null;
		else if(b instanceof BinaryExpression)
		{
			
			switch(((BinaryExpression)b).op()){
			case JOIN:
				return myJoin(getRightMostTuple_h(((BinaryExpression) b).left(), s), getRightMostTuple_h(((BinaryExpression) b).right(),s));
			case PRODUCT:
				return myProduct(getRightMostTuple_h(((BinaryExpression) b).left(), s), getRightMostTuple_h(((BinaryExpression) b).right(),s));
			default:
				System.out.println("Unsupported binaryexpression op");
				System.exit(1);
				break;
			}
			
		}
		else if(b instanceof Variable){
			tuples.add(s);
			return tuples;
		}
		else if(b instanceof Relation){
			TupleSet ts = relationTuples.get((Relation)b);
			Iterator<Tuple> itr = ts.iterator();
			
			while(itr.hasNext())
				tuples.add(itr.next());
			return tuples;
		}
		else if(b instanceof UnaryExpression)
		{
			switch(((UnaryExpression)b).op()){
			case CLOSURE:
				System.exit(1);
				break;
			case DIFFERENCE:
				System.exit(1);
				break;		
			case INTERSECTION: 
				System.exit(1);
				break;
			case JOIN:			
				System.exit(1);
				break;
			case OVERRIDE:		
				System.exit(1);
				break;
			case PRODUCT:			
				System.exit(1);
				break;
			case REFLEXIVE_CLOSURE:	
				System.exit(1);
				break;
			case TRANSPOSE:		//XXX might need to be elaborated
				Expression untrans = ((UnaryExpression)b).expression();
				TupleSet ts = relationTuples.get(untrans);
				Iterator<Tuple> itr = ts.iterator();	
				while(itr.hasNext())
				{
					Tuple t = itr.next();
					Object[] o = new Object[t.arity()];
					for(int i = 0; i < t.arity(); i++)
						o[t.arity()-1-i] = t.atom(i);
					Tuple t2 = factory.tuple(o); 
					tuples.add(t2);
				}
				return tuples;
			case UNION:
				System.exit(1);
				break;
			default:
				break;
			}
		}
		else
		{
			System.out.println("error in recompute3");
			System.exit(1);
		}
		return null;
	}
	
	public ArrayList<Tuple> myJoin(ArrayList<Tuple> l, ArrayList<Tuple> r)
	{
		if(l == null || r == null)
			return null;
		ArrayList<Tuple> tuples = new ArrayList<Tuple>();
		for(Tuple t: l)
			for(Tuple t2: r)
			{
				if(t.atom(t.arity()-1) == t2.atom(0)){
					Object[] atoms = new Object[t.arity() + t2.arity()-2];
					for(int i =0; i< t.arity()-1; i++)
						atoms[i] = t.atom(i);
					for(int i =1; i< t2.arity(); i++)
						atoms[i+t.arity()-2] = t2.atom(i);
					mostRecentRight = t2;
					tuples.add(factory.tuple(atoms));
					return tuples;
				}
			}
		return null;
	}
	
	public static ArrayList<Tuple> myProduct(ArrayList<Tuple> l, ArrayList<Tuple> r)
	{
		ArrayList<Tuple> tuples = new ArrayList<Tuple>();
		for(Tuple t: l)
			for(Tuple t2: r)
			{
				tuples.add(t.product(t2));
			}
		return tuples;
	}
	
}
	
