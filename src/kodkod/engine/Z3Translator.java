package kodkod.engine;

import com.microsoft.z3.*;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import kodkod.ast.*;
import kodkod.ast.operator.*;
import kodkod.ast.visitor.ReturnVisitor;
import kodkod.instance.Bounds;
import kodkod.instance.Instance;
import kodkod.instance.TupleSet;
import kodkod.instance.Tuple;
import kodkod.instance.TupleFactory;

public final class Z3Translator 
    implements ReturnVisitor<Z3Translator.ExprWithDomain, BoolExpr, Z3Translator.ExprWithDomain, IntExpr> {
    
    public final class ExprWithDomain {
        public ExprWithDomain(Expr expression, TupleSet domain) {
            this.expr = expression;
            this.domain = domain;
        }

        public final Expr expr;
        public final TupleSet domain;
    }

    private final Context context;
    private final Formula formula;
    private final Bounds bounds;
    private BoolExpr expression;
    private List<BoolExpr> extraConstraints;
    private HashMap<Relation, SetSort> relationTypeMapping;
    private EnumSort atomType;
    private HashMap<Object, Symbol> atomToSymbolMappings;
    private HashMap<Symbol, Object> symbolToAtomMappings;
    private HashMap<Object, Integer> atomToIndexMappings;
    private HashMap<Relation, Symbol> relationToSymbolMappings;
    private HashMap<Symbol, Relation> symbolToRelationMappings;
    private HashMap<Integer, TupleSort> arityToTupleType;
    private HashMap<Integer, SetSort> arityToSetType;

    int currentId = 0;

    private TupleSort tupleTypeForArity(int arity) {
        TupleSort sort = arityToTupleType.get(arity);
        if (sort == null) {
            try {
                Symbol typeName = context.MkSymbol("tuple$" + arity);
                Symbol[] fieldNames = new Symbol[arity];
                Sort[] fieldTypes = new Sort[arity];
                for (int i = 0; i < arity; i += 1) {
                    fieldNames[i] = context.MkSymbol("tuple$" + arity + "[" + i + "]");
                    fieldTypes[i] = atomType;
                }
                sort = context.MkTupleSort(typeName, fieldNames, fieldTypes);
            } catch (Z3Exception e) {
                throw new RuntimeException(e);
            }
            arityToTupleType.put(arity, sort);
        }
        return sort;
    }

    private SetSort setTypeForArity(int arity) {
        SetSort sort = arityToSetType.get(arity);
        if (sort == null) {
            try {
                sort = context.MkSetSort(tupleTypeForArity(arity));
            } catch (Z3Exception e) {
                throw new RuntimeException(e);
            }
            arityToSetType.put(arity, sort);
        }
        return sort;
    }

    public Z3Translator(Context context, Formula f, Bounds b) {
        this.context = context;
        this.formula = f;
        this.bounds = b;

        this.extraConstraints = new LinkedList();
        this.relationTypeMapping = new HashMap<Relation, SetSort>();
        this.atomToSymbolMappings = new HashMap<Object, Symbol>();
        this.symbolToAtomMappings = new HashMap<Symbol, Object>();
        this.atomToIndexMappings = new HashMap<Object, Integer>();
        this.relationToSymbolMappings = new HashMap<Relation, Symbol>();
        this.symbolToRelationMappings = new HashMap<Symbol, Relation>();
        this.arityToTupleType = new HashMap<Integer, TupleSort>();
        this.arityToSetType = new HashMap<Integer, SetSort>();
    }

    public void generateTranslation() {
        System.out.println("Generating translation for:");
        System.out.println(formula);
        try {
            // Generate symbols for each atom in the universe.
            int atomIndex = 0;
            Symbol[] atomSymbols = new Symbol[bounds.universe().size()];
            for (Object o : bounds.universe()) {
                Symbol s = context.MkSymbol("a" + getId());
                atomToSymbolMappings.put(o, s);
                symbolToAtomMappings.put(s, o);
                atomToIndexMappings.put(o, atomIndex);
                atomSymbols[atomIndex] = s;
                atomIndex += 1;
            }
            // Create an EnumSort to represent the atom data type.
            atomType = context.MkEnumSort(context.MkSymbol("atom"), atomSymbols);

            // Translate the bounds into actual sets in Z3 with lower and upper bounds.
            for (Relation r : bounds.relations()) {
                Symbol s = context.MkSymbol("r" + getId());
                TupleSort tupleType = tupleTypeForArity(r.arity());
                SetSort relationType = setTypeForArity(r.arity());
                relationToSymbolMappings.put(r, s);
                symbolToRelationMappings.put(s, r);

                BoolExpr upperBoundsConstraint = null;
                BoolExpr lowerBoundsConstraint = null;
                if (bounds.upperBound(r) != null) {
                    Expr upperBoundDefinition = context.MkEmptySet(tupleType); 
                    for (Tuple t : bounds.upperBound(r)) {
                        Expr[] fields = new Expr[t.arity()];
                        for (int i = 0; i < t.arity(); i += 1) {
                            Object o = t.atom(i);
                            fields[i] = atomType.Consts()[atomToIndexMappings.get(o)];
                        }
                        Expr createdTuple = context.MkApp(tupleType.MkDecl(), fields);
                        upperBoundDefinition = context.MkSetAdd(upperBoundDefinition, createdTuple);
                    }
                    upperBoundsConstraint = (BoolExpr)context.MkSetSubset(context.MkConst(s, relationType), upperBoundDefinition);
                    extraConstraints.add(upperBoundsConstraint);
                }

                if (bounds.lowerBound(r) != null) {
                    Expr lowerBoundDefinition = context.MkEmptySet(tupleType);
                    for (Tuple t : bounds.lowerBound(r)) {
                        Expr[] fields = new Expr[t.arity()];
                        for (int i = 0; i < t.arity(); i += 1) {
                            Object o = t.atom(i);
                            fields[i] = atomType.Consts()[atomToIndexMappings.get(o)];
                        }
                        Expr createdTuple = context.MkApp(tupleType.MkDecl(), fields);
                        lowerBoundDefinition = context.MkSetAdd(lowerBoundDefinition, createdTuple);
                    }
                    lowerBoundsConstraint = (BoolExpr)context.MkSetSubset(context.MkConst(s, relationType), lowerBoundDefinition);
                    extraConstraints.add(lowerBoundsConstraint);
                }
            }
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
        expression = formula.accept(this);
        try {
            BoolExpr[] expressions = new BoolExpr[extraConstraints.size() + 1];
            expressions[0] = expression;
            for (int i = 1; i <= extraConstraints.size(); i += 1) {
                expressions[i] = extraConstraints.get(i);
            }
            expression = context.MkAnd(expressions);
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
    }

    public BoolExpr getTranslation() {
        return this.expression;
    }

    public Instance getInstance(Model model) {
        return null;
    }

    public ExprWithDomain visit(Decls decls) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public ExprWithDomain visit(Decl decl) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public ExprWithDomain visit(Relation relation) {
        Expr expression = null;
        TupleSet domain = null;
        try {
            SetSort relationType = setTypeForArity(relation.arity());
            expression = context.MkConst(relationToSymbolMappings.get(relation), relationType);
            domain = bounds.upperBound(relation);
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
        return new ExprWithDomain(expression, domain);
    }

    public ExprWithDomain visit(Variable variable) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public ExprWithDomain visit(ConstantExpression constExpr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public ExprWithDomain visit(UnaryExpression unaryExpr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public ExprWithDomain visit(BinaryExpression binExpr) {
        ExprWithDomain left = binExpr.left().accept(this);
        ExprWithDomain right = binExpr.right().accept(this);
        Expr exprToReturn = null;
        TupleSet domainToReturn = null;
        try {
            switch (binExpr.op()) {
                case UNION:
                    exprToReturn = context.MkSetUnion(new Expr[] {left.expr, right.expr});
                    domainToReturn = left.domain.clone(); 
                    domainToReturn.addAll(right.domain);
                    break;
                case INTERSECTION:
                    exprToReturn = context.MkSetIntersection(new Expr[] {left.expr, right.expr});
                    domainToReturn = left.domain.clone();
                    domainToReturn.retainAll(right.domain);
                    break;
                case OVERRIDE:
                    throw new RuntimeException("Not Implemented Yet.");
                case PRODUCT:
                    throw new RuntimeException("Not Implemented Yet.");
                case DIFFERENCE:
                    exprToReturn = context.MkSetDifference(left.expr, right.expr);
                    domainToReturn = left.domain;
                    break;
                case JOIN:
                    HashMap<Object, TupleSet> joinTable = new HashMap<Object, TupleSet>();
                    TupleFactory factory = bounds.universe().factory();

                    // Compute the new domain.

                    // Compute a join table for the right relation.
                    for (Tuple t : right.domain) {
                        Object joinColumnValue = t.atom(0);
                        TupleSet joinTableSet = joinTable.get(joinColumnValue);
                        if (joinTableSet == null) {
                            joinTableSet = factory.setOf(t);
                            joinTable.put(joinColumnValue, joinTableSet);
                        } else {
                            joinTableSet.add(t);
                        }
                    }

                    // Iterate over all left tuples and add the join to the new domain.
                    System.out.println("Left Arity: " + left.domain.arity());
                    System.out.println("Right Arity: " + right.domain.arity());
                    Object[] values = new Object[left.domain.arity() + right.domain.arity() - 2];
                    System.out.println("Size of values array: " + values.length);
                    for (Tuple leftTuple : left.domain) {
                        Object joinColumnValue = leftTuple.atom(leftTuple.arity() - 1);
                        TupleSet joinTableSet = joinTable.get(joinColumnValue);
                        if (joinTableSet != null) {
                            for (int i = 0; i < leftTuple.arity() - 1; i += 1) {
                                values[i] = leftTuple.atom(i);
                            }
                            for (Tuple rightTuple : joinTableSet) {
                                for (int i = 1; i < rightTuple.arity(); i += 1) {
                                    System.out.println("Writing to " + i);
                                    values[(i -  1) + (leftTuple.arity() - 1)] = rightTuple.atom(i);
                                }
                                Tuple newTuple = factory.tuple(values);
                                if (domainToReturn == null) {
                                    domainToReturn = factory.setOf(newTuple);
                                } else {
                                    domainToReturn.add(newTuple);
                                }
                            }
                        }
                    }

                    // Create an expression for the join.

                    // Define a new set that will store the result.
                    SetSort resultSort = setTypeForArity(left.domain.arity() + right.domain.arity() - 2);
                    Symbol resultSymbol = context.MkSymbol("tr" + getId());
                    exprToReturn = context.MkConst(resultSymbol, resultSort);

                    // Add the join constraints to additional constraints.
                    //      forall a, b : (a in left) and (b in right) and (a[n-1] == b[0])
                    //                       <=> (a[0..n-2] * b[1..n-1] in result)
                    
                    Symbol leftTupleSymbol = context.MkSymbol("lt" + getId());
                    TupleSort leftTupleSort = tupleTypeForArity(left.domain.arity());
                    Expr leftTupleConst = context.MkConst(leftTupleSymbol, leftTupleSort);
                    Symbol rightTupleSymbol = context.MkSymbol("rt" + getId());
                    TupleSort rightTupleSort = tupleTypeForArity(right.domain.arity());
                    Expr rightTupleConst = context.MkConst(rightTupleSymbol, rightTupleSort);
                    BoolExpr leftTupleInclusionConstraint = (BoolExpr)context.MkSetMembership(leftTupleConst, left.expr);
                    BoolExpr rightTupleInclusionConstraint = (BoolExpr)context.MkSetMembership(rightTupleConst, right.expr);
                    BoolExpr matchConstraint = context.MkEq(
                        context.MkApp(leftTupleSort.FieldDecls()[left.domain.arity() - 1], leftTupleConst),
                        context.MkApp(rightTupleSort.FieldDecls()[0], rightTupleConst));

                    TupleSort resultTupleSort = tupleTypeForArity(left.domain.arity() + right.domain.arity() - 2);

                    Expr[] resultValues = new Expr[left.domain.arity() + right.domain.arity() - 2];
                    for (int i = 0; i < left.domain.arity() - 1; i += 1) {
                        resultValues[i] = context.MkApp(leftTupleSort.FieldDecls()[i], leftTupleConst);
                    }
                    for (int i = 1; i < right.domain.arity(); i += 1) {
                        resultValues[(i - 1) + (left.domain.arity() - 1)] = context.MkApp(rightTupleSort.FieldDecls()[i], rightTupleConst);
                    }
                    Expr resultCreation = context.MkApp(resultTupleSort.MkDecl(), resultValues);
                    BoolExpr resultInclusionConstraint = (BoolExpr)context.MkSetMembership(resultCreation, exprToReturn);

                    BoolExpr joinConstraint = context.MkForall(
                        new Expr[]{leftTupleConst, rightTupleConst},
                        context.MkIff(
                            context.MkAnd(new BoolExpr[] {
                                leftTupleInclusionConstraint,
                                rightTupleInclusionConstraint,
                                matchConstraint
                            }),
                            resultInclusionConstraint
                        ),
                        0,
                        null,
                        null,
                        null,
                        null
                    );
                    extraConstraints.add(joinConstraint);
                    break;
                default:
                    throw new RuntimeException("Unsupported ExprOperator " + binExpr.op());
            }
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
        return new ExprWithDomain(exprToReturn, domainToReturn);
    }

    public ExprWithDomain visit(NaryExpression expr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public ExprWithDomain visit(Comprehension comprehension) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public ExprWithDomain visit(IfExpression ifExpr) {
        BoolExpr condition = ifExpr.condition().accept(this);
        ExprWithDomain thenExpr = ifExpr.thenExpr().accept(this);
        ExprWithDomain elseExpr = ifExpr.elseExpr().accept(this);
        Expr toReturnExpr;
        try {
            toReturnExpr = context.MkITE(condition, thenExpr.expr, elseExpr.expr);
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
        TupleSet toReturnDomain = thenExpr.domain.clone();
        toReturnDomain.addAll(elseExpr.domain);

        return new ExprWithDomain(toReturnExpr, toReturnDomain);
    }

    public ExprWithDomain visit(ProjectExpression project) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public ExprWithDomain visit(IntToExprCast castExpr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public IntExpr visit(IntConstant intConst) {
        IntExpr toReturn;
        try {
            toReturn = context.MkInt(intConst.value());
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
        return toReturn;
    }

    public IntExpr visit(IfIntExpression intExpr) {
        BoolExpr condition = intExpr.condition().accept(this);
        IntExpr thenExpr = intExpr.thenExpr().accept(this);
        IntExpr elseExpr = intExpr.elseExpr().accept(this);
        IntExpr toReturn;
        
        try {
            toReturn = (IntExpr)context.MkITE(condition, thenExpr, elseExpr);
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }

        return toReturn;
    }

    public IntExpr visit(ExprToIntCast intExpr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public IntExpr visit(NaryIntExpression intExpr) {
        IntExpr[] exprs = new IntExpr[intExpr.size()];
        for(int i = 0; i < intExpr.size(); i += 1) {
            exprs[i] = intExpr.child(i).accept(this);
        }

        IntExpr toReturn;
        try {
            switch(intExpr.op()) {
                case PLUS:
                    toReturn = (IntExpr)context.MkAdd(exprs);
                    break;
                case MULTIPLY:
                    toReturn = (IntExpr)context.MkMul(exprs);
                    break;
                case AND:
                    throw new RuntimeException("Not implemented yet.");
                case OR:
                    throw new RuntimeException("Not implemented yet.");
                default:
                    throw new RuntimeException("Unsupported IntOperator " + intExpr.op());
            }
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }

        return toReturn;
    }

    public IntExpr visit(BinaryIntExpression intExpr) {
        IntExpr left = intExpr.left().accept(this);
        IntExpr right = intExpr.right().accept(this);

        IntExpr toReturn;
        try {
            switch(intExpr.op()) {
                case PLUS:
                    toReturn = (IntExpr)context.MkAdd(new IntExpr[]{left, right});
                    break;
                case MULTIPLY:
                    toReturn = (IntExpr)context.MkMul(new IntExpr[]{left, right});
                    break;
                case MINUS:
                    toReturn = (IntExpr)context.MkDiv(left, right);
                    break;
                case MODULO:
                    toReturn = (IntExpr)context.MkMod(left, right);
                    break;
                case AND:
                    throw new RuntimeException("Not implemented yet.");
                case OR:
                    throw new RuntimeException("Not implemented yet.");
                case XOR:
                    throw new RuntimeException("Not implemented yet.");
                case SHL:
                    throw new RuntimeException("Not implemented yet.");
                case SHR:
                    throw new RuntimeException("Not implemented yet.");
                case SHA:
                    throw new RuntimeException("Not implemented yet.");
                default:
                    throw new RuntimeException("Unsupported IntOperator " + intExpr.op());
            }
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
        return toReturn;
    }

    public IntExpr visit(UnaryIntExpression intExpr) {
        IntExpr internalExpr = intExpr.intExpr().accept(this);
        IntExpr toReturn;
        try {
            switch(intExpr.op()) {
                case NEG:
                    toReturn = (IntExpr)context.MkUnaryMinus(internalExpr);
                    break;
                case ABS:
                    throw new RuntimeException("Not implemented yet.");
                case NOT:
                    throw new RuntimeException("Not implemented yet.");
                case SGN:
                    throw new RuntimeException("NotImplemented yet.");
                default:
                    throw new RuntimeException("Unsupported IntOperator " + intExpr.op());
            }
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
        return toReturn;
    }

    public IntExpr visit(SumExpression intExpr) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public BoolExpr visit(IntComparisonFormula intComp) {
        IntExpr left = intComp.left().accept(this);
        IntExpr right = intComp.right().accept(this);
        BoolExpr toReturn;
        try {
            switch(intComp.op()) {
                case EQ:
                    toReturn = context.MkEq(left, right);
                    break;
                case LT:
                    toReturn = context.MkLt(left, right);
                    break;
                case LTE:
                    toReturn = context.MkLe(left, right);
                    break;
                case GT:
                    toReturn = context.MkGt(left, right);
                    break;
                case GTE:
                    toReturn = context.MkGe(left, right);
                    break;
                default:
                    throw new RuntimeException("Unsupported IntCompOperator " + intComp.op());
            }
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
        return toReturn;
    }

    public BoolExpr visit(QuantifiedFormula quantFormula) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public BoolExpr visit(NaryFormula formula) {
        BoolExpr[] exprs = new BoolExpr[formula.size()];
        for (int i = 0; i < formula.size(); i++) {
            exprs[i] = formula.child(i).accept(this);
        }
        BoolExpr toReturn;
        try {
            switch(formula.op()) {
                case AND:
                    toReturn = context.MkAnd(exprs);
                    break;
                case OR:
                    toReturn = context.MkOr(exprs);
                    break;
                default:
                    throw new RuntimeException("Invalid formula operator " + formula.op());
            }
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
        return toReturn;
    }

    public BoolExpr visit(BinaryFormula binFormula) {
        BoolExpr left = binFormula.left().accept(this);
        BoolExpr right = binFormula.right().accept(this);
        BoolExpr toReturn;
        try {
            switch(binFormula.op()) {
                case AND:
                    toReturn = context.MkAnd(new BoolExpr[]{left, right});
                    break;
                case OR:
                    toReturn = context.MkOr(new BoolExpr[]{left, right});
                    break;
                case IFF:
                    toReturn = context.MkIff(left, right);
                    break;
                case IMPLIES:
                    toReturn = context.MkImplies(left, right);
                    break;
                default:
                    throw new RuntimeException("Invalid formula operator " + binFormula.op());
            }
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
        return toReturn;
    }

    public BoolExpr visit(NotFormula not) {
        BoolExpr innerFormula = not.formula().accept(this);
        BoolExpr toReturn;
        try {
            toReturn = context.MkNot(innerFormula);
        } catch(Z3Exception e) {
            throw new RuntimeException(e);
        }
        return toReturn;
    }

    public BoolExpr visit(ConstantFormula constant) {
        BoolExpr toReturn;
        try {
            toReturn = context.MkBool(constant.booleanValue());
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
        return toReturn;
    }

    public BoolExpr visit(ComparisonFormula compFormula) {
        ExprWithDomain left = compFormula.left().accept(this);
        ExprWithDomain right = compFormula.right().accept(this);
        switch(compFormula.op()) {
            case SUBSET:
                throw new RuntimeException("Not implemented yet.");
            case EQUALS:
                throw new RuntimeException("Not implemented yet.");
            default:
                throw new RuntimeException("Unknown ExprCompOperator: " + compFormula.op());
        }
    }

    public BoolExpr visit(MultiplicityFormula multFormula) {
        BoolExpr toReturn = null;
        ExprWithDomain innerFormula = multFormula.expression().accept(this);
        try {
            switch (multFormula.multiplicity()) {
                case NO:
                    if (innerFormula.domain.size() == 0) {
                        toReturn = context.MkBool(true);
                    } else {
                        throw new RuntimeException("Not implemented yet.");
                    }
                    break;
                case LONE:
                    throw new RuntimeException("Not implemented yet.");
                case ONE:
                    throw new RuntimeException("Not implemented yet.");
                case SOME:
                    throw new RuntimeException("Not implemented yet.");
                default:
                    throw new RuntimeException("Unsupported multiplicity " + multFormula.multiplicity());
            }
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
        return toReturn;
    }

    public BoolExpr visit(RelationPredicate predicate) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    private int getId() {
        currentId += 1;
        return currentId - 1;
    }
}