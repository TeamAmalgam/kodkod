package kodkod.intelision;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.List;

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

  IntExprReduction ier;

	public Recompute(IntExprReduction ier) {
		this.ier = ier;
	}
	
	public Solution recompute(Solution sol, Bounds fromBounds, Bounds toBounds, Options options){
    if (sol.unsat()) {
      return sol;
    }

    Evaluator eval = new Evaluator(sol.instance(), options);
    Instance newInstance = new Instance(toBounds.universe());
    TupleFactory tupleFactory = toBounds.universe().factory();

    // Add the relations in the from solution into the new instance.
    for (Relation rel : sol.instance().relations()) {
      TupleSet oldTuples = sol.instance().tuples(rel);
      TupleSet newTuples = tupleFactory.noneOf(rel.arity());

      for (Tuple t : oldTuples) {
        List<Object> atoms = new ArrayList<Object>();
        for (int i = 0; i < t.arity(); i += 1) {
          atoms.add(t.atom(i));
        }
        newTuples.add(tupleFactory.tuple(atoms));
      }

      newInstance.add(rel, newTuples);
    }

    // Add the ints in the solution into the new instance.
    for (int i : sol.instance().ints().toArray()) {
      TupleSet oldTuples = sol.instance().tuples(i);
      TupleSet newTuples = tupleFactory.noneOf(oldTuples.arity());

      for (Tuple t : oldTuples) {
        List<Object> atoms = new ArrayList<Object>();
        for (int j = 0; j < t.arity(); j += 1) {
          atoms.add(t.atom(j));
        }
        newTuples.add(tupleFactory.tuple(atoms));
      }

      newInstance.add(i, newTuples);
    }

    // At this point the solutions should be the same, except the new one is
    // in the new universe.

    // Add the elided ints back into the solution.
    for (int i : toBounds.ints().toArray()) {
      TupleSet possibleIntTuples = toBounds.exactBound(i);
      Tuple toAdd = possibleIntTuples.iterator().next();
      
      newInstance.add(i, tupleFactory.setOf(toAdd));
    }

    // Add back relations that were elided.
    for (Relation rel : ier.elidedRelations) {
      TupleSet tuples = tupleFactory.noneOf(rel.arity());

      // We need to evaluate the expression that elided this relation to compute the expression.

      // Int/next is special.
      if (rel.toString().equals("Int/next")) {
        System.out.println("This is Int/next");
        
        int previous_int = 0;
        boolean first = true;

        for (int i : toBounds.ints().toArray()) {
          if (first) {
            first = false;
            previous_int = i;
            continue;
          }

          Tuple previousTuple = toBounds.exactBound(previous_int).iterator().next();
          Tuple currentTuple = toBounds.exactBound(i).iterator().next();

          tuples.add(tupleFactory.tuple(previousTuple.atom(0), currentTuple.atom(0)));
        }

        newInstance.add(rel, tuples);

        continue;
      }

      // Find the expression.
      for (Node n : ier.reductions_delete) {
        ComparisonFormula cf = (ComparisonFormula)n;

        if (cf.left() == rel ||
            cf.right() == rel) {
          System.out.println("Relation: " + rel + " fixed by " + cf);

          IntToExprCast cast;
          if (cf.left() == rel) {
            cast = (IntToExprCast)cf.right();
          } else {
            cast = (IntToExprCast)cf.left();
          }

          System.out.println("Evaluating: " + cast.intExpr().toString());

          int arithmeticResult = eval.evaluate(cast.intExpr());
          System.out.println("Arithmetic evaluates to: " + arithmeticResult);

          TupleSet possibleTuples = toBounds.exactBound(arithmeticResult);
          
          if (possibleTuples == null) {
            throw new RuntimeException("Integer " + arithmeticResult + " is not bound.");
          }

          Tuple tupleToAdd = possibleTuples.iterator().next();
          
          tuples.add(tupleToAdd);
          
          break;
        }
      }

      newInstance.add(rel, tuples);
    }

    Solution newSolution = new Solution(sol.outcome(), sol.stats(), newInstance, sol.proof());

    System.out.println("Old Solution: ");
    System.out.println(sol);
    System.out.println();
    System.out.println("New Solution: ");
    System.out.println(newSolution);


    return newSolution;
  }
}
	
