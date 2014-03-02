package kodkod.multiobjective;

import kodkod.engine.config.Options;
import kodkod.multiobjective.algorithms.AlgorithmFactory;
import kodkod.engine.satlab.SATFactory;

public final class MultiObjectiveOptions implements Cloneable {
	
	final Options kodkodOptions;
	private boolean allSolutionsPerPoint = true;
  private AlgorithmFactory multiObjectiveAlgorithm = AlgorithmFactory.CGIA;
	
	public MultiObjectiveOptions clone() {
		final MultiObjectiveOptions c = new MultiObjectiveOptions(kodkodOptions);
		c.setAllSolutionsPerPoint(allSolutionsPerPoint);
    c.setAlgorithm(multiObjectiveAlgorithm);
		return c;
	}
	
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("Kodkod ");
		b.append(kodkodOptions.toString());
		b.append("\nMultiObjectiveOptions:");
		b.append("\n allSolutionsPerPoint: ");
		b.append(allSolutionsPerPoint);
    b.append("\n algorithm: ");
    b.append(multiObjectiveAlgorithm);
		return b.toString();
	}
	
	public MultiObjectiveOptions() {
    this(new Options());
    kodkodOptions.setSolver(SATFactory.MiniSat);
	}
	
	public MultiObjectiveOptions( Options options ) {
		if (options == null) {
			throw new NullPointerException();
		}
		kodkodOptions = options;
	}

	public Options getKodkodOptions() {
		return kodkodOptions;
	}
	
	// [TeamAmalgam] - Adding for Alloy support
	// We can't get rid of this because it gets called, even though nothing
	// tries to read flatten'. So for our purposes, this will just be stubbed out.
	/**
	 * Sets the flattening option to the given value.
	 * @ensures this.flatten' = flatten
	 * @throws IllegalArgumentException - this.logTranslation>0 && flatten
	 */
	public void setFlatten(boolean flatten) {
		if (kodkodOptions.logTranslation()>0 && flatten)
			throw new IllegalStateException("logTranslation enabled:  flattening must be off.");
	}

	/**
	 * Returns whether all solutions for a given Pareto point should be enumerated,
	 * only meaningful when using Moolloy.
	 * @return this.allSolutionsPerPoint
	 */
	public Boolean allSolutionsPerPoint(){
		return allSolutionsPerPoint;
	}

	/**
	 * Sets whether all solutions for a given Pareto point should be enumerated,
	 * only meaningful when using Moolloy.
	 * @ensures this.allSolutionsPerPoint' = allSolutionsPerPoint
	 */
	public void setAllSolutionsPerPoint(boolean allSolutionsPerPoint){
		this.allSolutionsPerPoint = allSolutionsPerPoint;
	}

  /**
   * Returns the AlgorithmFactory to use.
   * @return this.multiobjectiveAlgorithm
   */
  public AlgorithmFactory getAlgorithm() {
    return multiObjectiveAlgorithm;
  }

  /**
   * Sets the AlgorithmFactory to use.
   * @ensures this.multiObjectiveAlgorithm = multiObjectiveAlgorithm
   */
  public void setAlgorithm(AlgorithmFactory multiObjectiveAlgorithm) {
    this.multiObjectiveAlgorithm = multiObjectiveAlgorithm;
  }
}
