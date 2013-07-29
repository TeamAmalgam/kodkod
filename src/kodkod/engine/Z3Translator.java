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
    private HashMap<Relation, TupleSort> relationToTupleTypeMappings;

    int currentId = 0;

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
        this.relationToTupleTypeMappings = new HashMap<Relation, TupleSort>();
    }

    public void generateTranslation() {
        try {
            // Generate symbols for each atom in the universe.
            int atomIndex = 0;
            for (Object o : bounds.universe()) {
                Symbol s = context.MkSymbol("a" + getId());
                atomToSymbolMappings.put(o, s);
                symbolToAtomMappings.put(s, o);
                atomToIndexMappings.put(o, atomIndex);
                atomIndex += 1;
            }
            // Create an EnumSort to represent the atom data type.
            atomType = context.MkEnumSort(context.MkSymbol("atom"),
                                          (Symbol[])symbolToAtomMappings.keySet().toArray());

            // Translate the bounds into actual sets in Z3 with lower and upper bounds.
            for (Relation r : bounds.relations()) {
                Symbol s = context.MkSymbol("r" + getId());
                Symbol[] fieldNames = new Symbol[r.arity()];
                Sort[] fieldTypes = new Sort[r.arity()];
                for (int i = 0; i < r.arity(); i++) {
                    fieldNames[i] = context.MkSymbol("f" + i);
                    fieldTypes[i] = atomType;
                }
                TupleSort tupleType = context.MkTupleSort(context.MkSymbol(s.toString() + "$type"), fieldNames, fieldTypes);
                SetSort relationType = context.MkSetSort(tupleType);
                relationToSymbolMappings.put(r, s);
                symbolToRelationMappings.put(s, r);
                relationToTupleTypeMappings.put(r, tupleType);

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
                }

                BoolExpr totalBoundsConstraint = null;
                if (upperBoundsConstraint != null && lowerBoundsConstraint != null) {
                    totalBoundsConstraint = context.MkAnd(new BoolExpr[] {upperBoundsConstraint, lowerBoundsConstraint});
                } else if (upperBoundsConstraint != null) {
                    totalBoundsConstraint = upperBoundsConstraint;
                } else if (lowerBoundsConstraint != null) {
                    totalBoundsConstraint = lowerBoundsConstraint;
                }

                if (totalBoundsConstraint != null) {
                    extraConstraints.add(totalBoundsConstraint);
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
        throw new RuntimeException("Not Implemented Yet.");
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
        throw new RuntimeException("Not Implemented Yet.");
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
        throw new RuntimeException("Not Implemented Yet.");
    }

    public BoolExpr visit(MultiplicityFormula multFormula) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    public BoolExpr visit(RelationPredicate predicate) {
        throw new RuntimeException("Not Implemented Yet.");
    }

    private int getId() {
        currentId += 1;
        return currentId - 1;
    }
}