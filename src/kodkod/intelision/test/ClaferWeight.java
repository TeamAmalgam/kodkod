package kodkod.intelision.test;
import java.util.Arrays;
import java.util.List;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntConstant;
import kodkod.ast.IntExpression;
import kodkod.ast.Relation;
import kodkod.ast.operator.FormulaOperator;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.config.Options;
import kodkod.engine.satlab.SATFactory;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.TupleSet;
import kodkod.instance.Universe;
import kodkod.intelision.IntExprReduction;

/* 
  ==================================================
    kodkod formula: 
  ==================================================
    one (this/c1_Test . (this/c1_Test -> this/c1_Test.r_c2_weight)) && 
    (this/c1_Test . (this/c1_Test -> this/c1_Test.r_c2_weight)) in 
    this/c2_weight && 
    one (this/c1_Test . (this/c1_Test -> this/c1_Test.r_c3_x)) && 
    (this/c1_Test . (this/c1_Test -> this/c1_Test.r_c3_x)) in this/c3_x && 
    one (this/c1_Test . (this/c1_Test -> this/c1_Test.r_c4_y)) && 
    (this/c1_Test . (this/c1_Test -> this/c1_Test.r_c4_y)) in this/c4_y && 
    one (this/c1_Test . (this/c1_Test -> this/c1_Test.r_c5_z)) && 
    (this/c1_Test . (this/c1_Test -> this/c1_Test.r_c5_z)) in this/c5_z && 
    (this/c1_Test.r_c2_weight . (this/c2_weight -> this/c2_weight.ref)) = Int[
    int[this/c1_Test.r_c3_x . (this/c3_x -> this/c3_x.ref)] + int[
    this/c1_Test.r_c4_y . (this/c4_y -> this/c4_y.ref)] + int[
    this/c1_Test.r_c5_z . (this/c5_z -> this/c5_z.ref)]] && 
    int[this/c1_Test.r_c2_weight . (this/c2_weight -> this/c2_weight.ref)] > -2 && 
    int[this/c1_Test.r_c2_weight . (this/c2_weight -> this/c2_weight.ref)] < 4 && 
    one (this/c2_weight . (this/c2_weight -> this/c2_weight.ref)) && 
    (this/c2_weight . (this/c2_weight -> this/c2_weight.ref)) in ints && 
    one (this/c3_x . (this/c3_x -> this/c3_x.ref)) && 
    (this/c3_x . (this/c3_x -> this/c3_x.ref)) in ints && 
    one (this/c4_y . (this/c4_y -> this/c4_y.ref)) && 
    (this/c4_y . (this/c4_y -> this/c4_y.ref)) in ints && 
    one (this/c5_z . (this/c5_z -> this/c5_z.ref)) && 
    (this/c5_z . (this/c5_z -> this/c5_z.ref)) in ints && 
    Int/min = Int/min && 
    Int/zero = Int/zero && 
    Int/max = Int/max && 
    Int/next = Int/next && 
    seq/Int = seq/Int && 
    String = String && 
    this/c1_Test = this/c1_Test && 
    this/c2_weight = this/c2_weight && 
    this/c3_x = this/c3_x && 
    this/c4_y = this/c4_y && 
    this/c5_z = this/c5_z && 
    this/c1_Test.r_c2_weight = this/c1_Test.r_c2_weight && 
    this/c1_Test.r_c3_x = this/c1_Test.r_c3_x && 
    this/c1_Test.r_c4_y = this/c1_Test.r_c4_y && 
    this/c1_Test.r_c5_z = this/c1_Test.r_c5_z && 
    this/c2_weight.ref = this/c2_weight.ref && 
    this/c3_x.ref = this/c3_x.ref && 
    this/c4_y.ref = this/c4_y.ref && 
    this/c5_z.ref = this/c5_z.ref
  ==================================================
*/
public final class ClaferWeight {

public static void main(String[] args) throws Exception {

Relation x0 = Relation.unary("Int/min");
Relation x1 = Relation.unary("Int/zero");
Relation x2 = Relation.unary("Int/max");
Relation x3 = Relation.nary("Int/next", 2);
Relation x4 = Relation.unary("seq/Int");
Relation x5 = Relation.unary("String");
Relation x6 = Relation.unary("this/c1_Test");
Relation x7 = Relation.unary("this/c2_weight");
Relation x8 = Relation.unary("this/c3_x");
Relation x9 = Relation.unary("this/c4_y");
Relation x10 = Relation.unary("this/c5_z");
Relation x11 = Relation.unary("this/c1_Test.r_c2_weight");
Relation x12 = Relation.unary("this/c1_Test.r_c3_x");
Relation x13 = Relation.unary("this/c1_Test.r_c4_y");
Relation x14 = Relation.unary("this/c1_Test.r_c5_z");
Relation x15 = Relation.unary("this/c2_weight.ref");
Relation x16 = Relation.unary("this/c3_x.ref");
Relation x17 = Relation.unary("this/c4_y.ref");
Relation x18 = Relation.unary("this/c5_z.ref");

List<String> atomlist = Arrays.asList(
 "-1", "-2", "-3", "-4", "-5",
 "-6", "-7", "-8", "0", "1", "2",
 "3", "4", "5", "6", "7", "c1_Test$0",
 "c2_weight$0", "c3_x$0", "c4_y$0", "c5_z$0"
);

Universe universe = new Universe(atomlist);
TupleFactory factory = universe.factory();
Bounds bounds = new Bounds(universe);

TupleSet x0_upper = factory.noneOf(1);
x0_upper.add(factory.tuple("-8"));
bounds.boundExactly(x0, x0_upper);

TupleSet x1_upper = factory.noneOf(1);
x1_upper.add(factory.tuple("0"));
bounds.boundExactly(x1, x1_upper);

TupleSet x2_upper = factory.noneOf(1);
x2_upper.add(factory.tuple("7"));
bounds.boundExactly(x2, x2_upper);

TupleSet x3_upper = factory.noneOf(2);
x3_upper.add(factory.tuple("-8").product(factory.tuple("-7")));
x3_upper.add(factory.tuple("-7").product(factory.tuple("-6")));
x3_upper.add(factory.tuple("-6").product(factory.tuple("-5")));
x3_upper.add(factory.tuple("-5").product(factory.tuple("-4")));
x3_upper.add(factory.tuple("-4").product(factory.tuple("-3")));
x3_upper.add(factory.tuple("-3").product(factory.tuple("-2")));
x3_upper.add(factory.tuple("-2").product(factory.tuple("-1")));
x3_upper.add(factory.tuple("-1").product(factory.tuple("0")));
x3_upper.add(factory.tuple("0").product(factory.tuple("1")));
x3_upper.add(factory.tuple("1").product(factory.tuple("2")));
x3_upper.add(factory.tuple("2").product(factory.tuple("3")));
x3_upper.add(factory.tuple("3").product(factory.tuple("4")));
x3_upper.add(factory.tuple("4").product(factory.tuple("5")));
x3_upper.add(factory.tuple("5").product(factory.tuple("6")));
x3_upper.add(factory.tuple("6").product(factory.tuple("7")));
bounds.boundExactly(x3, x3_upper);

TupleSet x4_upper = factory.noneOf(1);
x4_upper.add(factory.tuple("0"));
bounds.boundExactly(x4, x4_upper);

TupleSet x5_upper = factory.noneOf(1);
bounds.boundExactly(x5, x5_upper);

TupleSet x6_upper = factory.noneOf(1);
x6_upper.add(factory.tuple("c1_Test$0"));
bounds.boundExactly(x6, x6_upper);

TupleSet x7_upper = factory.noneOf(1);
x7_upper.add(factory.tuple("c2_weight$0"));
bounds.boundExactly(x7, x7_upper);

TupleSet x8_upper = factory.noneOf(1);
x8_upper.add(factory.tuple("c3_x$0"));
bounds.boundExactly(x8, x8_upper);

TupleSet x9_upper = factory.noneOf(1);
x9_upper.add(factory.tuple("c4_y$0"));
bounds.boundExactly(x9, x9_upper);

TupleSet x10_upper = factory.noneOf(1);
x10_upper.add(factory.tuple("c5_z$0"));
bounds.boundExactly(x10, x10_upper);

TupleSet x11_upper = factory.noneOf(1);
x11_upper.add(factory.tuple("c2_weight$0"));
bounds.bound(x11, x11_upper);

TupleSet x12_upper = factory.noneOf(1);
x12_upper.add(factory.tuple("c3_x$0"));
bounds.bound(x12, x12_upper);

TupleSet x13_upper = factory.noneOf(1);
x13_upper.add(factory.tuple("c4_y$0"));
bounds.bound(x13, x13_upper);

TupleSet x14_upper = factory.noneOf(1);
x14_upper.add(factory.tuple("c5_z$0"));
bounds.bound(x14, x14_upper);

TupleSet x15_upper = factory.noneOf(1);
x15_upper.add(factory.tuple("-8"));
x15_upper.add(factory.tuple("-7"));
x15_upper.add(factory.tuple("-6"));
x15_upper.add(factory.tuple("-5"));
x15_upper.add(factory.tuple("-4"));
x15_upper.add(factory.tuple("-3"));
x15_upper.add(factory.tuple("-2"));
x15_upper.add(factory.tuple("-1"));
x15_upper.add(factory.tuple("0"));
x15_upper.add(factory.tuple("1"));
x15_upper.add(factory.tuple("2"));
x15_upper.add(factory.tuple("3"));
x15_upper.add(factory.tuple("4"));
x15_upper.add(factory.tuple("5"));
x15_upper.add(factory.tuple("6"));
x15_upper.add(factory.tuple("7"));
bounds.bound(x15, x15_upper);

TupleSet x16_upper = factory.noneOf(1);
x16_upper.add(factory.tuple("-8"));
x16_upper.add(factory.tuple("-7"));
x16_upper.add(factory.tuple("-6"));
x16_upper.add(factory.tuple("-5"));
x16_upper.add(factory.tuple("-4"));
x16_upper.add(factory.tuple("-3"));
x16_upper.add(factory.tuple("-2"));
x16_upper.add(factory.tuple("-1"));
x16_upper.add(factory.tuple("0"));
x16_upper.add(factory.tuple("1"));
x16_upper.add(factory.tuple("2"));
x16_upper.add(factory.tuple("3"));
x16_upper.add(factory.tuple("4"));
x16_upper.add(factory.tuple("5"));
x16_upper.add(factory.tuple("6"));
x16_upper.add(factory.tuple("7"));
bounds.bound(x16, x16_upper);

TupleSet x17_upper = factory.noneOf(1);
x17_upper.add(factory.tuple("-8"));
x17_upper.add(factory.tuple("-7"));
x17_upper.add(factory.tuple("-6"));
x17_upper.add(factory.tuple("-5"));
x17_upper.add(factory.tuple("-4"));
x17_upper.add(factory.tuple("-3"));
x17_upper.add(factory.tuple("-2"));
x17_upper.add(factory.tuple("-1"));
x17_upper.add(factory.tuple("0"));
x17_upper.add(factory.tuple("1"));
x17_upper.add(factory.tuple("2"));
x17_upper.add(factory.tuple("3"));
x17_upper.add(factory.tuple("4"));
x17_upper.add(factory.tuple("5"));
x17_upper.add(factory.tuple("6"));
x17_upper.add(factory.tuple("7"));
bounds.bound(x17, x17_upper);

TupleSet x18_upper = factory.noneOf(1);
x18_upper.add(factory.tuple("-8"));
x18_upper.add(factory.tuple("-7"));
x18_upper.add(factory.tuple("-6"));
x18_upper.add(factory.tuple("-5"));
x18_upper.add(factory.tuple("-4"));
x18_upper.add(factory.tuple("-3"));
x18_upper.add(factory.tuple("-2"));
x18_upper.add(factory.tuple("-1"));
x18_upper.add(factory.tuple("0"));
x18_upper.add(factory.tuple("1"));
x18_upper.add(factory.tuple("2"));
x18_upper.add(factory.tuple("3"));
x18_upper.add(factory.tuple("4"));
x18_upper.add(factory.tuple("5"));
x18_upper.add(factory.tuple("6"));
x18_upper.add(factory.tuple("7"));
bounds.bound(x18, x18_upper);

bounds.boundExactly(-8,factory.range(factory.tuple("-8"),factory.tuple("-8")));
bounds.boundExactly(-7,factory.range(factory.tuple("-7"),factory.tuple("-7")));
bounds.boundExactly(-6,factory.range(factory.tuple("-6"),factory.tuple("-6")));
bounds.boundExactly(-5,factory.range(factory.tuple("-5"),factory.tuple("-5")));
bounds.boundExactly(-4,factory.range(factory.tuple("-4"),factory.tuple("-4")));
bounds.boundExactly(-3,factory.range(factory.tuple("-3"),factory.tuple("-3")));
bounds.boundExactly(-2,factory.range(factory.tuple("-2"),factory.tuple("-2")));
bounds.boundExactly(-1,factory.range(factory.tuple("-1"),factory.tuple("-1")));
bounds.boundExactly(0,factory.range(factory.tuple("0"),factory.tuple("0")));
bounds.boundExactly(1,factory.range(factory.tuple("1"),factory.tuple("1")));
bounds.boundExactly(2,factory.range(factory.tuple("2"),factory.tuple("2")));
bounds.boundExactly(3,factory.range(factory.tuple("3"),factory.tuple("3")));
bounds.boundExactly(4,factory.range(factory.tuple("4"),factory.tuple("4")));
bounds.boundExactly(5,factory.range(factory.tuple("5"),factory.tuple("5")));
bounds.boundExactly(6,factory.range(factory.tuple("6"),factory.tuple("6")));
bounds.boundExactly(7,factory.range(factory.tuple("7"),factory.tuple("7")));

Expression x23=x6.product(x11);
Expression x22=x6.join(x23);
Formula x21=x22.one();
Formula x24=x22.in(x7);
Formula x20=x21.and(x24);
Expression x28=x6.product(x12);
Expression x27=x6.join(x28);
Formula x26=x27.one();
Formula x29=x27.in(x8);
Formula x25=x26.and(x29);
Expression x33=x6.product(x13);
Expression x32=x6.join(x33);
Formula x31=x32.one();
Formula x34=x32.in(x9);
Formula x30=x31.and(x34);
Expression x38=x6.product(x14);
Expression x37=x6.join(x38);
Formula x36=x37.one();
Formula x39=x37.in(x10);
Formula x35=x36.and(x39);
Expression x44=x7.product(x15);
Expression x43=x11.join(x44);
Expression x50=x8.product(x16);
Expression x49=x12.join(x50);
IntExpression x48=x49.sum();
Expression x53=x9.product(x17);
Expression x52=x13.join(x53);
IntExpression x51=x52.sum();
IntExpression x47=x48.plus(x51);
Expression x56=x10.product(x18);
Expression x55=x14.join(x56);
IntExpression x54=x55.sum();
IntExpression x46=x47.plus(x54);
Expression x45=x46.toExpression();
Formula x42=x43.eq(x45);
Expression x59=x11.join(x44);
IntExpression x58=x59.sum();
IntExpression x60=IntConstant.constant(-2);
Formula x57=x58.gt(x60);
Formula x41=x42.and(x57);
Expression x63=x11.join(x44);
IntExpression x62=x63.sum();
IntExpression x64=IntConstant.constant(4);
Formula x61=x62.lt(x64);
Formula x40=x41.and(x61);
Expression x67=x7.join(x44);
Formula x66=x67.one();
Formula x68=x67.in(Expression.INTS);
Formula x65=x66.and(x68);
Expression x72=x8.join(x50);
Formula x71=x72.one();
Formula x73=x72.in(Expression.INTS);
Formula x70=x71.and(x73);
Expression x76=x9.join(x53);
Formula x75=x76.one();
Formula x77=x76.in(Expression.INTS);
Formula x74=x75.and(x77);
Expression x80=x10.join(x56);
Formula x79=x80.one();
Formula x81=x80.in(Expression.INTS);
Formula x78=x79.and(x81);
Formula x82=x0.eq(x0);
Formula x83=x1.eq(x1);
Formula x84=x2.eq(x2);
Formula x85=x3.eq(x3);
Formula x86=x4.eq(x4);
Formula x87=x5.eq(x5);
Formula x88=x6.eq(x6);
Formula x89=x7.eq(x7);
Formula x90=x8.eq(x8);
Formula x91=x9.eq(x9);
Formula x92=x10.eq(x10);
Formula x93=x11.eq(x11);
Formula x94=x12.eq(x12);
Formula x95=x13.eq(x13);
Formula x96=x14.eq(x14);
Formula x97=x15.eq(x15);
Formula x98=x16.eq(x16);
Formula x99=x17.eq(x17);
Formula x100=x18.eq(x18);
Formula x19=Formula.compose(FormulaOperator.AND, x20, x25, x30, x35, x40, x65, x70, x74, x78, x82, x83, x84, x85, x86, x87, x88, x89, x90, x91, x92, x93, x94, x95, x96, x97, x98, x99, x100);

Solver solver = new Solver();
solver.options().setSolver(SATFactory.DefaultSAT4J);
solver.options().setBitwidth(4);
solver.options().setIntEncoding(Options.IntEncoding.TWOSCOMPLEMENT);
solver.options().setSymmetryBreaking(20);
solver.options().setSkolemDepth(0);
System.out.println("Solving...");
System.out.flush();
Solution sol = solver.solve(x19,bounds);
System.out.println(sol.toString());


IntExprReduction ier = new IntExprReduction();
Formula[] formulas = ier.reduceIntExpressions(x20, x25, x30, x35, x40, x65, x70, x74, x78, x82, x83, x84, x85, x86, x87, x88, x89, x90, x91, x92, x93, x94, x95, x96, x97, x98, x99, x100);//x77, x78
 Formula newFormula=Formula.compose(FormulaOperator.AND, formulas);
ier.solve(newFormula, bounds, factory, universe, 32); 

}}
