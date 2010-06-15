package upparse;

import static upparse.MaxVals.*;

/**
 * Right-regular grammar model
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class RRG extends SequenceModel {

  /** The original training set */
  private final CombinedProb combinedP;
  
  private RRG(
      final BIOEncoder _encoder,
      final int[] _orig,
      final CombinedProb _combinedProb) {
    super(_encoder, _orig, new int[_combinedProb.numTerms()][]);
    combinedP = _combinedProb;
    updateTagDict();
    checkSanity();
  }

  @Override
  public void checkSanity() {
    combinedP.checkSanity();
    super.checkSanity();
  }

  @Override
  public void updateTagDict() {
    combinedP.backoffHmm.updateTagDict();
    super.updateTagDict();
  }

  @Override
  public double arcprob(int j, int w, int k) {
    return combinedP.arcprob(j, w, k);
  }

  @Override
  public void update(double[][][] counts) {
    combinedP.update(counts);
  }

  @Override
  public int numTags() {
    return combinedP.numTags();
  }

  @Override
  public int numTerms() {
    return combinedP.numTerms();
  }

  @Override
  public double initTagProb(int tag) {
    return combinedP.backoffHmm.initTagProb(tag);
  }

  @Override
  public double termProb(int tag, int term) {
    return combinedP.backoffHmm.termProb(tag, term);
  }

  /**
   * @param corpus Corpus output by a separate model
   * @param encoder A {@link BIOEncoder} for encoding data-sets
   * @param scaleFactor 
   * @param scaleFactor2 
   * @return A new right-regular grammar model
   */
  public static RRG mleEstimate(
      final ChunkedSegmentedCorpus corpus,
      final BIOEncoder encoder, double scaleFactor2, double scaleFactor) throws EncoderError {
    int[] tokens = encoder.tokensFromClumpedCorpus(corpus);
    int[] bioTrain = encoder.bioTrain(corpus, tokens.length);
    return mleEstimate(tokens, bioTrain, encoder, scaleFactor2, scaleFactor);
  }

  /**
   * @param tokens Corpus tokens
   * @param train Tag training set
   * @param _encoder A {@link BIOEncoder} for creating corpus datasets
   * @param scaleFactor 
   * @param scaleFactor2 
   * @return
   */
  public static RRG mleEstimate(
      final int[] tokens, 
      final int[] train, 
      final BIOEncoder _encoder, 
      double scaleFactor2, double scaleFactor) {
    final HMM backoffHmm = HMM.mleEstimate(tokens, train, _encoder, scaleFactor);
    final CombinedProb combined = 
      getCombinedProb(tokens, train, backoffHmm, scaleFactor2);
    assert tokens.length == train.length;
    return new RRG(_encoder, tokens, combined);
  }

  /**
   * @param tokens
   * @param train
   * @param scaleFactor 
   * @return
   */
  private static CombinedProb getCombinedProb(
      final int[] tokens, 
      final int[] train,
      final HMM backoffHmm, double scaleFactor) {
    final int 
      nterm = arrayMax(tokens) + 1,
      ntag = arrayMax(train) + 1;
    
    final int[][][] counts = new int[ntag][nterm][ntag];
    assert tokens.length == train.length;
    for (int t = 1; t < tokens.length; t++) 
      counts[train[t-1]][tokens[t-1]][train[t]]++;
    
    final double[][][] countsD = new double[ntag][nterm][ntag];
    for (int j = 0; j < ntag; j++)
      for (int w = 0; w < nterm; w++)
        for (int k = 0; k < ntag; k++)
          countsD[j][w][k] = (double) counts[j][w][k];
    
    return CombinedProb.fromCounts(countsD, backoffHmm, scaleFactor);
  }
}
