package upparse.model;

import upparse.corpus.*;

/**
 * Right-regular grammar model
 * 
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class RRG extends SequenceModel {

  /** The original training set */
  private final CombinedProb combinedP;

  private RRG(final TagEncoder _encoder, final int[] _orig,
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
   * @param corpus
   *          Corpus output by a separate model
   * @param encoder
   *          A {@link TagEncoder} for encoding data-sets
   * @param scaleFactor
   * @param scaleFactor2
   * @return A new right-regular grammar model
   */
  public static RRG mleEstimate(final ChunkedSegmentedCorpus corpus,
      final TagEncoder encoder, final double smooth) throws EncoderError {
    final double[][][] counts = encoder.hardCounts(corpus);
    final int[] tokens = encoder.tokensFromClumpedCorpus(corpus);
    return fromCounts(counts, encoder, tokens, smooth);
  }

  public static RRG fromCounts(double[][][] counts, TagEncoder encoder,
      int[] tokens, double smooth) {
    final HMM backoff = HMM.fromCounts(counts, encoder, tokens, smooth);
    final CombinedProb combined = CombinedProb.fromCounts(counts, backoff,
        smooth);
    return new RRG(encoder, tokens, combined);
  }

  public static SequenceModel softCountEstimate(StopSegmentCorpus corpus,
      TagEncoder encoder, double smooth) {
    final int[] tokens = encoder.tokensFromStopSegmentCorpus(corpus);
    final double[][][] counts = encoder.softCounts(tokens);
    return fromCounts(counts, encoder, tokens, smooth);
  }

  private static SequenceModel fromProbs(final double[][][] prob,
      final StopSegmentCorpus corpus, final HMM hmm, final TagEncoder encoder,
      final double smooth) {
    final int[] tokens = encoder.tokensFromStopSegmentCorpus(corpus);
    final CombinedProb cp = CombinedProb.fromProb(prob, hmm, smooth);
    return new RRG(encoder, tokens, cp);
  }

  public static SequenceModel uniformEstimate(final StopSegmentCorpus corpus,
      final TagEncoder encoder, final double smooth) {
    final double[][][] prob = encoder.altUniformJoint(corpus.getAlpha().size());
    final HMM hmm = HMM.uniformEstimate(corpus, encoder, smooth);
    return fromProbs(prob, corpus, hmm, encoder, smooth);
  }

  public static SequenceModel randomEstimate(final StopSegmentCorpus corpus,
      TagEncoder encoder, double smooth) {
    final double[][][] prob = encoder.randomJoint(corpus.getAlpha().size());
    final HMM hmm = HMM.randomEstimate(corpus, encoder, smooth);
    return fromProbs(prob, corpus, hmm, encoder, smooth);
  }
}
