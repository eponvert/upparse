package upparse.model;

import upparse.corpus.*;

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
      final BIOEncoder encoder) throws EncoderError {
    final double[][][] counts = encoder.hardCounts(corpus);
    final int[] tokens = encoder.tokensFromClumpedCorpus(corpus);
    return fromCounts(counts, encoder, tokens);
  }

  public static RRG fromCounts(
      double[][][] counts, BIOEncoder encoder, int[] tokens) {
    final HMM backoff = HMM.fromCounts(counts, encoder, tokens);
    final CombinedProb combined = CombinedProb.fromCounts(counts, backoff);
    return new RRG(encoder, tokens, combined);
  }

  public static SequenceModel softCountEstimate(
      StopSegmentCorpus corpus, BIOEncoder encoder) {
    final int[] tokens = encoder.tokensFromStopSegmentCorpus(corpus);
    final double[][][] counts = encoder.softCounts(tokens);
    return fromCounts(counts, encoder, tokens);
  }
}
