package kodkod.intelision.test;
import java.util.Arrays;
import java.util.List;

import kodkod.ast.Decls;
import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.IntConstant;
import kodkod.ast.IntExpression;
import kodkod.ast.Relation;
import kodkod.ast.Variable;
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
    (all show_this: this/c1_Test | 
      one (show_this . this/c1_Test.r_c2_weight) && 
      (show_this . this/c1_Test.r_c2_weight) in this/c2_weight) && 
    (this/c1_Test.r_c2_weight . univ) in this/c1_Test && 
    (all show_this: this/c1_Test | 
      one (show_this . this/c1_Test.r_c3_x) && 
      (show_this . this/c1_Test.r_c3_x) in this/c3_x) && 
    (this/c1_Test.r_c3_x . univ) in this/c1_Test && 
    (all show_this: this/c1_Test | 
      one (show_this . this/c1_Test.r_c4_y) && 
      (show_this . this/c1_Test.r_c4_y) in this/c4_y) && 
    (this/c1_Test.r_c4_y . univ) in this/c1_Test && 
    (all show_this: this/c1_Test | 
      one (show_this . this/c1_Test.r_c5_z) && 
      (show_this . this/c1_Test.r_c5_z) in this/c5_z) && 
    (this/c1_Test.r_c5_z . univ) in this/c1_Test && 
    (all show_this: this/c1_Test | 
      ((show_this . this/c1_Test.r_c2_weight) . this/c2_weight.ref) = Int[int[(
      show_this . this/c1_Test.r_c3_x) . this/c3_x.ref] + int[(show_this . 
      this/c1_Test.r_c4_y) . this/c4_y.ref] + int[(show_this . 
      this/c1_Test.r_c5_z) . this/c5_z.ref]] && 
      int[(show_this . this/c1_Test.r_c2_weight) . this/c2_weight.ref] > -2 && 
      int[(show_this . this/c1_Test.r_c2_weight) . this/c2_weight.ref] < 4) && 
    (all show_this: this/c2_weight | 
      one (show_this . this/c2_weight.ref) && 
      (show_this . this/c2_weight.ref) in ints) && 
    (this/c2_weight.ref . univ) in this/c2_weight && 
    (all show_this: this/c2_weight | 
      one (this/c1_Test.r_c2_weight . show_this)) && 
    (all show_this: this/c3_x | 
      one (show_this . this/c3_x.ref) && 
      (show_this . this/c3_x.ref) in ints) && 
    (this/c3_x.ref . univ) in this/c3_x && 
    (all show_this: this/c3_x | 
      one (this/c1_Test.r_c3_x . show_this)) && 
    (all show_this: this/c4_y | 
      one (show_this . this/c4_y.ref) && 
      (show_this . this/c4_y.ref) in ints) && 
    (this/c4_y.ref . univ) in this/c4_y && 
    (all show_this: this/c4_y | 
      one (this/c1_Test.r_c4_y . show_this)) && 
    (all show_this: this/c5_z | 
      one (show_this . this/c5_z.ref) && 
      (show_this . this/c5_z.ref) in ints) && 
    (this/c5_z.ref . univ) in this/c5_z && 
    (all show_this: this/c5_z | 
      one (this/c1_Test.r_c5_z . show_this)) && 
    2 <= #(this/c1_Test) && 
    #(this/c1_Test) <= 2 && 
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
public final class ClaferWeightWith2 {

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
Relation x11 = Relation.nary("this/c1_Test.r_c2_weight", 2);
Relation x12 = Relation.nary("this/c1_Test.r_c3_x", 2);
Relation x13 = Relation.nary("this/c1_Test.r_c4_y", 2);
Relation x14 = Relation.nary("this/c1_Test.r_c5_z", 2);
Relation x15 = Relation.nary("this/c2_weight.ref", 2);
Relation x16 = Relation.nary("this/c3_x.ref", 2);
Relation x17 = Relation.nary("this/c4_y.ref", 2);
Relation x18 = Relation.nary("this/c5_z.ref", 2);

List<String> atomlist = Arrays.asList(
 "-1", "-2", "-3", "-4", "-5",
 "-6", "-7", "-8", "0", "1", "2",
 "3", "4", "5", "6", "7", "c1_Test$0",
 "c1_Test$1", "c2_weight$0", "c2_weight$1", "c3_x$0", "c3_x$1", "c4_y$0",
 "c4_y$1", "c5_z$0", "c5_z$1"
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
x6_upper.add(factory.tuple("c1_Test$1"));
bounds.bound(x6, x6_upper);

TupleSet x7_upper = factory.noneOf(1);
x7_upper.add(factory.tuple("c2_weight$0"));
x7_upper.add(factory.tuple("c2_weight$1"));
bounds.bound(x7, x7_upper);

TupleSet x8_upper = factory.noneOf(1);
x8_upper.add(factory.tuple("c3_x$0"));
x8_upper.add(factory.tuple("c3_x$1"));
bounds.bound(x8, x8_upper);

TupleSet x9_upper = factory.noneOf(1);
x9_upper.add(factory.tuple("c4_y$0"));
x9_upper.add(factory.tuple("c4_y$1"));
bounds.bound(x9, x9_upper);

TupleSet x10_upper = factory.noneOf(1);
x10_upper.add(factory.tuple("c5_z$0"));
x10_upper.add(factory.tuple("c5_z$1"));
bounds.bound(x10, x10_upper);

TupleSet x11_upper = factory.noneOf(2);
x11_upper.add(factory.tuple("c1_Test$0").product(factory.tuple("c2_weight$0")));
x11_upper.add(factory.tuple("c1_Test$0").product(factory.tuple("c2_weight$1")));
x11_upper.add(factory.tuple("c1_Test$1").product(factory.tuple("c2_weight$0")));
x11_upper.add(factory.tuple("c1_Test$1").product(factory.tuple("c2_weight$1")));
bounds.bound(x11, x11_upper);

TupleSet x12_upper = factory.noneOf(2);
x12_upper.add(factory.tuple("c1_Test$0").product(factory.tuple("c3_x$0")));
x12_upper.add(factory.tuple("c1_Test$0").product(factory.tuple("c3_x$1")));
x12_upper.add(factory.tuple("c1_Test$1").product(factory.tuple("c3_x$0")));
x12_upper.add(factory.tuple("c1_Test$1").product(factory.tuple("c3_x$1")));
bounds.bound(x12, x12_upper);

TupleSet x13_upper = factory.noneOf(2);
x13_upper.add(factory.tuple("c1_Test$0").product(factory.tuple("c4_y$0")));
x13_upper.add(factory.tuple("c1_Test$0").product(factory.tuple("c4_y$1")));
x13_upper.add(factory.tuple("c1_Test$1").product(factory.tuple("c4_y$0")));
x13_upper.add(factory.tuple("c1_Test$1").product(factory.tuple("c4_y$1")));
bounds.bound(x13, x13_upper);

TupleSet x14_upper = factory.noneOf(2);
x14_upper.add(factory.tuple("c1_Test$0").product(factory.tuple("c5_z$0")));
x14_upper.add(factory.tuple("c1_Test$0").product(factory.tuple("c5_z$1")));
x14_upper.add(factory.tuple("c1_Test$1").product(factory.tuple("c5_z$0")));
x14_upper.add(factory.tuple("c1_Test$1").product(factory.tuple("c5_z$1")));
bounds.bound(x14, x14_upper);

TupleSet x15_upper = factory.noneOf(2);
x15_upper.add(factory.tuple("c2_weight$0").product(factory.tuple("-8")));
x15_upper.add(factory.tuple("c2_weight$0").product(factory.tuple("-7")));
x15_upper.add(factory.tuple("c2_weight$0").product(factory.tuple("-6")));
x15_upper.add(factory.tuple("c2_weight$0").product(factory.tuple("-5")));
x15_upper.add(factory.tuple("c2_weight$0").product(factory.tuple("-4")));
x15_upper.add(factory.tuple("c2_weight$0").product(factory.tuple("-3")));
x15_upper.add(factory.tuple("c2_weight$0").product(factory.tuple("-2")));
x15_upper.add(factory.tuple("c2_weight$0").product(factory.tuple("-1")));
x15_upper.add(factory.tuple("c2_weight$0").product(factory.tuple("0")));
x15_upper.add(factory.tuple("c2_weight$0").product(factory.tuple("1")));
x15_upper.add(factory.tuple("c2_weight$0").product(factory.tuple("2")));
x15_upper.add(factory.tuple("c2_weight$0").product(factory.tuple("3")));
x15_upper.add(factory.tuple("c2_weight$0").product(factory.tuple("4")));
x15_upper.add(factory.tuple("c2_weight$0").product(factory.tuple("5")));
x15_upper.add(factory.tuple("c2_weight$0").product(factory.tuple("6")));
x15_upper.add(factory.tuple("c2_weight$0").product(factory.tuple("7")));
x15_upper.add(factory.tuple("c2_weight$1").product(factory.tuple("-8")));
x15_upper.add(factory.tuple("c2_weight$1").product(factory.tuple("-7")));
x15_upper.add(factory.tuple("c2_weight$1").product(factory.tuple("-6")));
x15_upper.add(factory.tuple("c2_weight$1").product(factory.tuple("-5")));
x15_upper.add(factory.tuple("c2_weight$1").product(factory.tuple("-4")));
x15_upper.add(factory.tuple("c2_weight$1").product(factory.tuple("-3")));
x15_upper.add(factory.tuple("c2_weight$1").product(factory.tuple("-2")));
x15_upper.add(factory.tuple("c2_weight$1").product(factory.tuple("-1")));
x15_upper.add(factory.tuple("c2_weight$1").product(factory.tuple("0")));
x15_upper.add(factory.tuple("c2_weight$1").product(factory.tuple("1")));
x15_upper.add(factory.tuple("c2_weight$1").product(factory.tuple("2")));
x15_upper.add(factory.tuple("c2_weight$1").product(factory.tuple("3")));
x15_upper.add(factory.tuple("c2_weight$1").product(factory.tuple("4")));
x15_upper.add(factory.tuple("c2_weight$1").product(factory.tuple("5")));
x15_upper.add(factory.tuple("c2_weight$1").product(factory.tuple("6")));
x15_upper.add(factory.tuple("c2_weight$1").product(factory.tuple("7")));
bounds.bound(x15, x15_upper);

TupleSet x16_upper = factory.noneOf(2);
x16_upper.add(factory.tuple("c3_x$0").product(factory.tuple("-8")));
x16_upper.add(factory.tuple("c3_x$0").product(factory.tuple("-7")));
x16_upper.add(factory.tuple("c3_x$0").product(factory.tuple("-6")));
x16_upper.add(factory.tuple("c3_x$0").product(factory.tuple("-5")));
x16_upper.add(factory.tuple("c3_x$0").product(factory.tuple("-4")));
x16_upper.add(factory.tuple("c3_x$0").product(factory.tuple("-3")));
x16_upper.add(factory.tuple("c3_x$0").product(factory.tuple("-2")));
x16_upper.add(factory.tuple("c3_x$0").product(factory.tuple("-1")));
x16_upper.add(factory.tuple("c3_x$0").product(factory.tuple("0")));
x16_upper.add(factory.tuple("c3_x$0").product(factory.tuple("1")));
x16_upper.add(factory.tuple("c3_x$0").product(factory.tuple("2")));
x16_upper.add(factory.tuple("c3_x$0").product(factory.tuple("3")));
x16_upper.add(factory.tuple("c3_x$0").product(factory.tuple("4")));
x16_upper.add(factory.tuple("c3_x$0").product(factory.tuple("5")));
x16_upper.add(factory.tuple("c3_x$0").product(factory.tuple("6")));
x16_upper.add(factory.tuple("c3_x$0").product(factory.tuple("7")));
x16_upper.add(factory.tuple("c3_x$1").product(factory.tuple("-8")));
x16_upper.add(factory.tuple("c3_x$1").product(factory.tuple("-7")));
x16_upper.add(factory.tuple("c3_x$1").product(factory.tuple("-6")));
x16_upper.add(factory.tuple("c3_x$1").product(factory.tuple("-5")));
x16_upper.add(factory.tuple("c3_x$1").product(factory.tuple("-4")));
x16_upper.add(factory.tuple("c3_x$1").product(factory.tuple("-3")));
x16_upper.add(factory.tuple("c3_x$1").product(factory.tuple("-2")));
x16_upper.add(factory.tuple("c3_x$1").product(factory.tuple("-1")));
x16_upper.add(factory.tuple("c3_x$1").product(factory.tuple("0")));
x16_upper.add(factory.tuple("c3_x$1").product(factory.tuple("1")));
x16_upper.add(factory.tuple("c3_x$1").product(factory.tuple("2")));
x16_upper.add(factory.tuple("c3_x$1").product(factory.tuple("3")));
x16_upper.add(factory.tuple("c3_x$1").product(factory.tuple("4")));
x16_upper.add(factory.tuple("c3_x$1").product(factory.tuple("5")));
x16_upper.add(factory.tuple("c3_x$1").product(factory.tuple("6")));
x16_upper.add(factory.tuple("c3_x$1").product(factory.tuple("7")));
bounds.bound(x16, x16_upper);

TupleSet x17_upper = factory.noneOf(2);
x17_upper.add(factory.tuple("c4_y$0").product(factory.tuple("-8")));
x17_upper.add(factory.tuple("c4_y$0").product(factory.tuple("-7")));
x17_upper.add(factory.tuple("c4_y$0").product(factory.tuple("-6")));
x17_upper.add(factory.tuple("c4_y$0").product(factory.tuple("-5")));
x17_upper.add(factory.tuple("c4_y$0").product(factory.tuple("-4")));
x17_upper.add(factory.tuple("c4_y$0").product(factory.tuple("-3")));
x17_upper.add(factory.tuple("c4_y$0").product(factory.tuple("-2")));
x17_upper.add(factory.tuple("c4_y$0").product(factory.tuple("-1")));
x17_upper.add(factory.tuple("c4_y$0").product(factory.tuple("0")));
x17_upper.add(factory.tuple("c4_y$0").product(factory.tuple("1")));
x17_upper.add(factory.tuple("c4_y$0").product(factory.tuple("2")));
x17_upper.add(factory.tuple("c4_y$0").product(factory.tuple("3")));
x17_upper.add(factory.tuple("c4_y$0").product(factory.tuple("4")));
x17_upper.add(factory.tuple("c4_y$0").product(factory.tuple("5")));
x17_upper.add(factory.tuple("c4_y$0").product(factory.tuple("6")));
x17_upper.add(factory.tuple("c4_y$0").product(factory.tuple("7")));
x17_upper.add(factory.tuple("c4_y$1").product(factory.tuple("-8")));
x17_upper.add(factory.tuple("c4_y$1").product(factory.tuple("-7")));
x17_upper.add(factory.tuple("c4_y$1").product(factory.tuple("-6")));
x17_upper.add(factory.tuple("c4_y$1").product(factory.tuple("-5")));
x17_upper.add(factory.tuple("c4_y$1").product(factory.tuple("-4")));
x17_upper.add(factory.tuple("c4_y$1").product(factory.tuple("-3")));
x17_upper.add(factory.tuple("c4_y$1").product(factory.tuple("-2")));
x17_upper.add(factory.tuple("c4_y$1").product(factory.tuple("-1")));
x17_upper.add(factory.tuple("c4_y$1").product(factory.tuple("0")));
x17_upper.add(factory.tuple("c4_y$1").product(factory.tuple("1")));
x17_upper.add(factory.tuple("c4_y$1").product(factory.tuple("2")));
x17_upper.add(factory.tuple("c4_y$1").product(factory.tuple("3")));
x17_upper.add(factory.tuple("c4_y$1").product(factory.tuple("4")));
x17_upper.add(factory.tuple("c4_y$1").product(factory.tuple("5")));
x17_upper.add(factory.tuple("c4_y$1").product(factory.tuple("6")));
x17_upper.add(factory.tuple("c4_y$1").product(factory.tuple("7")));
bounds.bound(x17, x17_upper);

TupleSet x18_upper = factory.noneOf(2);
x18_upper.add(factory.tuple("c5_z$0").product(factory.tuple("-8")));
x18_upper.add(factory.tuple("c5_z$0").product(factory.tuple("-7")));
x18_upper.add(factory.tuple("c5_z$0").product(factory.tuple("-6")));
x18_upper.add(factory.tuple("c5_z$0").product(factory.tuple("-5")));
x18_upper.add(factory.tuple("c5_z$0").product(factory.tuple("-4")));
x18_upper.add(factory.tuple("c5_z$0").product(factory.tuple("-3")));
x18_upper.add(factory.tuple("c5_z$0").product(factory.tuple("-2")));
x18_upper.add(factory.tuple("c5_z$0").product(factory.tuple("-1")));
x18_upper.add(factory.tuple("c5_z$0").product(factory.tuple("0")));
x18_upper.add(factory.tuple("c5_z$0").product(factory.tuple("1")));
x18_upper.add(factory.tuple("c5_z$0").product(factory.tuple("2")));
x18_upper.add(factory.tuple("c5_z$0").product(factory.tuple("3")));
x18_upper.add(factory.tuple("c5_z$0").product(factory.tuple("4")));
x18_upper.add(factory.tuple("c5_z$0").product(factory.tuple("5")));
x18_upper.add(factory.tuple("c5_z$0").product(factory.tuple("6")));
x18_upper.add(factory.tuple("c5_z$0").product(factory.tuple("7")));
x18_upper.add(factory.tuple("c5_z$1").product(factory.tuple("-8")));
x18_upper.add(factory.tuple("c5_z$1").product(factory.tuple("-7")));
x18_upper.add(factory.tuple("c5_z$1").product(factory.tuple("-6")));
x18_upper.add(factory.tuple("c5_z$1").product(factory.tuple("-5")));
x18_upper.add(factory.tuple("c5_z$1").product(factory.tuple("-4")));
x18_upper.add(factory.tuple("c5_z$1").product(factory.tuple("-3")));
x18_upper.add(factory.tuple("c5_z$1").product(factory.tuple("-2")));
x18_upper.add(factory.tuple("c5_z$1").product(factory.tuple("-1")));
x18_upper.add(factory.tuple("c5_z$1").product(factory.tuple("0")));
x18_upper.add(factory.tuple("c5_z$1").product(factory.tuple("1")));
x18_upper.add(factory.tuple("c5_z$1").product(factory.tuple("2")));
x18_upper.add(factory.tuple("c5_z$1").product(factory.tuple("3")));
x18_upper.add(factory.tuple("c5_z$1").product(factory.tuple("4")));
x18_upper.add(factory.tuple("c5_z$1").product(factory.tuple("5")));
x18_upper.add(factory.tuple("c5_z$1").product(factory.tuple("6")));
x18_upper.add(factory.tuple("c5_z$1").product(factory.tuple("7")));
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

Variable x22=Variable.unary("show_this");
Decls x21=x22.oneOf(x6);
Expression x25=x22.join(x11);
Formula x24=x25.one();
Formula x26=x25.in(x7);
Formula x23=x24.and(x26);
Formula x20=x23.forAll(x21);
Expression x28=x11.join(Expression.UNIV);
Formula x27=x28.in(x6);
Variable x32=Variable.unary("show_this");
Decls x31=x32.oneOf(x6);
Expression x35=x32.join(x12);
Formula x34=x35.one();
Formula x36=x35.in(x8);
Formula x33=x34.and(x36);
Formula x30=x33.forAll(x31);
Expression x38=x12.join(Expression.UNIV);
Formula x37=x38.in(x6);
Variable x41=Variable.unary("show_this");
Decls x40=x41.oneOf(x6);
Expression x44=x41.join(x13);
Formula x43=x44.one();
Formula x45=x44.in(x9);
Formula x42=x43.and(x45);
Formula x39=x42.forAll(x40);
Expression x47=x13.join(Expression.UNIV);
Formula x46=x47.in(x6);
Variable x50=Variable.unary("show_this");
Decls x49=x50.oneOf(x6);
Expression x53=x50.join(x14);
Formula x52=x53.one();
Formula x54=x53.in(x10);
Formula x51=x52.and(x54);
Formula x48=x51.forAll(x49);
Expression x56=x14.join(Expression.UNIV);
Formula x55=x56.in(x6);
Variable x59=Variable.unary("show_this");
Decls x58=x59.oneOf(x6);
Expression x64=x59.join(x11);
Expression x63=x64.join(x15);
Expression x70=x59.join(x12);
Expression x69=x70.join(x16);
IntExpression x68=x69.sum();
Expression x73=x59.join(x13);
Expression x72=x73.join(x17);
IntExpression x71=x72.sum();
IntExpression x67=x68.plus(x71);
Expression x76=x59.join(x14);
Expression x75=x76.join(x18);
IntExpression x74=x75.sum();
IntExpression x66=x67.plus(x74);
Expression x65=x66.toExpression();
Formula x62=x63.eq(x65);
Expression x80=x59.join(x11);
Expression x79=x80.join(x15);
IntExpression x78=x79.sum();
IntExpression x81=IntConstant.constant(-2);
Formula x77=x78.gt(x81);
Formula x61=x62.and(x77);
Expression x85=x59.join(x11);
Expression x84=x85.join(x15);
IntExpression x83=x84.sum();
IntExpression x86=IntConstant.constant(4);
Formula x82=x83.lt(x86);
Formula x60=x61.and(x82);
Formula x57=x60.forAll(x58);
Variable x89=Variable.unary("show_this");
Decls x88=x89.oneOf(x7);
Expression x92=x89.join(x15);
Formula x91=x92.one();
Formula x93=x92.in(Expression.INTS);
Formula x90=x91.and(x93);
Formula x87=x90.forAll(x88);
Expression x96=x15.join(Expression.UNIV);
Formula x95=x96.in(x7);
Variable x99=Variable.unary("show_this");
Decls x98=x99.oneOf(x7);
Expression x101=x11.join(x99);
Formula x100=x101.one();
Formula x97=x100.forAll(x98);
Variable x104=Variable.unary("show_this");
Decls x103=x104.oneOf(x8);
Expression x107=x104.join(x16);
Formula x106=x107.one();
Formula x108=x107.in(Expression.INTS);
Formula x105=x106.and(x108);
Formula x102=x105.forAll(x103);
Expression x110=x16.join(Expression.UNIV);
Formula x109=x110.in(x8);
Variable x113=Variable.unary("show_this");
Decls x112=x113.oneOf(x8);
Expression x115=x12.join(x113);
Formula x114=x115.one();
Formula x111=x114.forAll(x112);
Variable x118=Variable.unary("show_this");
Decls x117=x118.oneOf(x9);
Expression x121=x118.join(x17);
Formula x120=x121.one();
Formula x122=x121.in(Expression.INTS);
Formula x119=x120.and(x122);
Formula x116=x119.forAll(x117);
Expression x124=x17.join(Expression.UNIV);
Formula x123=x124.in(x9);
Variable x127=Variable.unary("show_this");
Decls x126=x127.oneOf(x9);
Expression x129=x13.join(x127);
Formula x128=x129.one();
Formula x125=x128.forAll(x126);
Variable x132=Variable.unary("show_this");
Decls x131=x132.oneOf(x10);
Expression x135=x132.join(x18);
Formula x134=x135.one();
Formula x136=x135.in(Expression.INTS);
Formula x133=x134.and(x136);
Formula x130=x133.forAll(x131);
Expression x138=x18.join(Expression.UNIV);
Formula x137=x138.in(x10);
Variable x141=Variable.unary("show_this");
Decls x140=x141.oneOf(x10);
Expression x143=x14.join(x141);
Formula x142=x143.one();
Formula x139=x142.forAll(x140);
IntExpression x145=IntConstant.constant(2);
IntExpression x146=x6.count();
Formula x144=x145.lte(x146);
IntExpression x148=x6.count();
IntExpression x149=IntConstant.constant(2);
Formula x147=x148.lte(x149);
Formula x150=x0.eq(x0);
Formula x151=x1.eq(x1);
Formula x152=x2.eq(x2);
Formula x153=x3.eq(x3);
Formula x154=x4.eq(x4);
Formula x155=x5.eq(x5);
Formula x156=x6.eq(x6);
Formula x157=x7.eq(x7);
Formula x158=x8.eq(x8);
Formula x159=x9.eq(x9);
Formula x160=x10.eq(x10);
Formula x161=x11.eq(x11);
Formula x162=x12.eq(x12);
Formula x163=x13.eq(x13);
Formula x164=x14.eq(x14);
Formula x165=x15.eq(x15);
Formula x166=x16.eq(x16);
Formula x167=x17.eq(x17);
Formula x168=x18.eq(x18);
Formula newFormula2=Formula.compose(FormulaOperator.AND, x20,x27,x30,x37,x39,x46,x48,x55,x57,x87,x95,x97,x102,x109,x111,x116,x123,x125,x130,x137,x139,x144,x147,x150,x151,x152,x153,x154,x155,x156,x157,x158,x159,x160,x161,x162,x163,x164,x165,x166,x167,x168);
Solver solver = new Solver();
solver.options().setSolver(SATFactory.DefaultSAT4J);
solver.options().setBitwidth(4);
solver.options().setIntEncoding(Options.IntEncoding.TWOSCOMPLEMENT);
solver.options().setSymmetryBreaking(20);
solver.options().setSkolemDepth(0);
System.out.println("Solving...");
System.out.flush();
Solution sol = solver.solve(newFormula2,bounds);
System.out.println(sol.toString());
IntExprReduction ier = new IntExprReduction();
Formula[] formulas = ier.reduceIntExpressions(x20,x27,x30,x37,x39,x46,x48,x55,x57,x87,x95,x97,x102,x109,x111,x116,x123,x125,x130,x137,x139,x144,x147,x150,x151,x152,x153,x154,x155,x156,x157,x158,x159,x160,x161,x162,x163,x164,x165,x166,x167,x168);
Formula newFormula=Formula.compose(FormulaOperator.AND, formulas);
ier.solve(newFormula, bounds, factory, universe, 32);
}}
