package kodkod;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  kodkod.engine.TestSuite.class,
  kodkod.multiobjective.TestSuite.class,
  kodkod.optimizer.TestSuite.class
})
public class TestSuite {
}
