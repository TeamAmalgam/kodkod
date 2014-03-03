package kodkod.multiobjective.algorithms;

import kodkod.multiobjective.MultiObjectiveOptions;

public abstract class AlgorithmFactory {
  
  protected AlgorithmFactory() {}

  public abstract MultiObjectiveAlgorithm instance(MultiObjectiveOptions options);

  public static final AlgorithmFactory GIA = new AlgorithmFactory() {
    public MultiObjectiveAlgorithm instance(MultiObjectiveOptions options) {
      return new GuidedImprovementAlgorithm("GIA", options);
    }
  };

  public static final AlgorithmFactory IGIA = new AlgorithmFactory() {
    public MultiObjectiveAlgorithm instance(MultiObjectiveOptions options) {
      return new IncrementalGuidedImprovementAlgorithm("IGIA", options);
    }
  };

  public static final AlgorithmFactory CGIA = new AlgorithmFactory() {
    public MultiObjectiveAlgorithm instance(MultiObjectiveOptions options) {
      return new CheckpointedGuidedImprovementAlgorithm("CGIA", options);
    }
  };

  public static final AlgorithmFactory PGIA = new AlgorithmFactory() {
    public MultiObjectiveAlgorithm instance(MultiObjectiveOptions options) {
      return new PartitionedGuidedImprovementAlgorithm("PGIA", options);
    }
  };

  public static final AlgorithmFactory OGIA = new AlgorithmFactory() {
    public MultiObjectiveAlgorithm instance(MultiObjectiveOptions options) {
      return new OverlappingGuidedImprovementAlgorithm("OGIA", options);
    }
  };
}
