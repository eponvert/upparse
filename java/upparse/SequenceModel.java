package upparse;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public interface SequenceModel {

  /** Update model using expectation maximization on original training data */ 
  public void emUpdateFromTrain();

  /** Update model using (new) data */ 
  public void emUpdateFrom(final int[] data);

  /** Tag the corpus, return structured corpus */
  ChunkedSegmentedCorpus tagCC(final int[] testCorpus) 
  throws SequenceModelError;

  /** @return perplexity of current EM step */ 
  double currPerplex();

  /** @return the original training data-set */ 
  int[] getOrig();
}
