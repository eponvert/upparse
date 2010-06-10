package upparse;

import static upparse.MaxVals.*;
import static java.lang.Math.*;

/**
 * Right-regular grammar model
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class RRG implements SequenceModel {

  /** The original training set */
  private final int[] orig;
  private final BIOEncoder encoder;
  private final CombinedProb combinedP;
  private final double[] initTag;
  private int[][] tagdict;
  private double perplex = 1e16;
  
  private RRG(
      final BIOEncoder _encoder,
      final int[] _orig,
      final CombinedProb _combinedProb,
      final double[] _initTag,
      final int _origLastSeenToken,
      final int _origLastSeenTag) {
    encoder = _encoder;
    orig = _orig;
    combinedP = _combinedProb;
    initTag = _initTag;
    updateTagDict();
    checkSanity();
  }

  private void checkSanity() {
    combinedP.checkSanity();
    double sum = 0;
    for (double d: initTag) 
      sum += exp(d);
    assert abs(sum-1) < 1e-5;
  }

  private void updateTagDict() {
    final int ntag = combinedP.numTags(), nterm = combinedP.numTerms();
    tagdict = new int[nterm][ntag];
    int[] alltags = new int[ntag];
    for (int i = 0; i < ntag; i++) alltags[i] = i;
    for (int w = 0; w < nterm; w++) tagdict[w] = alltags;
  }

  @Override
  public double currPerplex() {
    return perplex;
  }

  @Override
  public void emUpdateFrom(final int[] data) {
    int 
      ndata = data.length,
      last = ndata - 1,
      ntag = combinedP.numTags(),
      nvocab = combinedP.numTerms();
    
    final double neginf = Double.NEGATIVE_INFINITY;
    
    // Forward-backward algorithm applied to RRGs
    
    double[][]
      forward = new double[ndata][ntag],
      backward = new double[ndata][ntag];
    
    for (int d = 0; d < ndata; d++) {
      for (int t = 0; t < ntag; t++) {
        forward[d][t] = neginf;
        backward[d][t] = neginf;
      }
    }
    
    // Forward probabilities
    // Time n = 0
    for (int j = 0; j < ntag; j++)
      forward[0][j] = initTag[j];
    
    // Times n+1 to N
    for (int n = 1; n < ndata; n++) {
      for (int j: tagdict[data[n-1]]) {
        for (int k: tagdict[data[n]]) {
          final double forwUpd = 
            forward[n-1][j] + combinedP.getProb(j, data[n-1], k);
          forward[n][k] = Util.logadd(forward[n][k], forwUpd);
        }
      }
    }
    
    // Finally update forward probabilities with last token probs
    for (int j: tagdict[data[last-1]]) {
      for (int k: tagdict[data[last]]) {
        final double forwUpd = 
          forward[last-1][j] + combinedP.lastTok(k, data[last]);
        forward[last][j] = Util.logadd(forward[last][j], forwUpd);
      }
    }
    
    // TODO check end conditions
    // for now assuming last tag is STOP with index 0
    // 
    final double forwTotal = forward[last][0];
    backward[last][0] = 0;
    
    // Backward probabilities. Also collecting new training counts as we go
    final double[][][] counts = new double[ntag][nvocab][ntag];
    
    for (int n = last; n > 0; n--) {
      for (int k: tagdict[data[n]]) {
        for (int j: tagdict[data[n-1]]) {
          final double 
            fwd = forward[n-1][j], 
            bwd = backward[n][k],
            arcprob = combinedP.getProb(j, data[n-1], k),
            backUpd = arcprob + bwd;
          
          backward[n-1][j] = Util.logadd(backward[n-1][j], backUpd);
          if (arcprob != neginf && fwd != neginf && bwd != neginf) {
            final double
              logupd = fwd + bwd + arcprob - forwTotal,
              upd = exp(logupd);
            final int w = data[n-1];
            counts[j][w][k] += upd; 
          }
        }
      }
    }
    
    combinedP.update(counts);
    
    updateTagDict();
    checkSanity();

    // Get perplexity
    perplex = exp(-forward[last][0]/ndata);
  }

  @Override
  public void emUpdateFromTrain() {
    emUpdateFrom(orig);
  }

  @Override
  public ChunkedSegmentedCorpus tagCC(int[] testCorpus)
      throws RRGError, EncoderError {
    int[] output = tag(testCorpus);
    return encoder.clumpedCorpusFromBIOOutput(testCorpus, output);
  }

  /** Viterbi tagger for right-regular grammars */
  private int[] tag(int[] tokens) {
    int ndata = tokens.length;
    
    final double[][] viterbi = new double[ndata][];
    final int[][] backpointer = new int[ndata][];
    int[] tags, _tags = tagdict[tokens[0]];
    
    double[] v;
    MaxVals mv;
    
    viterbi[0] = new double[_tags.length];
    for (int j: _tags) 
      viterbi[0][j] = initTag[j];

    for (int t = 1; t < ndata; t++) {
      tags = tagdict[tokens[t]];
      viterbi[t] = new double[tags.length];
      backpointer[t] = new int[tags.length];
      for (int k: tags) {
        v = new double[_tags.length];
        for (int j: _tags) {
          v[j] = viterbi[t-1][j] + combinedP.getProb(j, tokens[t-1], k);
        }
        mv = new MaxVals(v);
        viterbi[t][k] = mv.max;
        backpointer[t][k] = mv.argmax;
      }
      _tags = tags;
    }
    
    final int last = ndata - 1;
    for (int t: tagdict[tokens[last]])
      viterbi[last][t] += combinedP.lastTok(t, tokens[last]);
    
    mv = new MaxVals(viterbi[last]);
    
    // first past gets the indices for the tags in the tagdict
    final int[] output = new int[ndata];
    int curr, next;
    output[last] = mv.argmax;
    for (int i = last; i > 0; i--) {
      curr = output[i];
      next = backpointer[i][curr];
      output[i-1] = next;
    }
    
    // second pass fills in the actual tags from the tagdict
    for (int i = 0; i < ndata; i++)
      output[i] = tagdict[tokens[i]][output[i]];
    
    return output;
  }

  @Override
  public int[] getOrig() {
    return orig;
  }

  /**
   * @param corpus Corpus output by a separate model
   * @param encoder A {@link BIOEncoder} for encoding data-sets
   * @return A new right-regular grammar model
   */
  public static RRG mleEstimate(
      final ChunkedSegmentedCorpus corpus,
      final BIOEncoder encoder, 
      final double smoothFactor) throws EncoderError {
    int[] tokens = encoder.tokensFromClumpedCorpus(corpus);
    int[] bioTrain = encoder.bioTrain(corpus, tokens.length);
    return mleEstimate(tokens, bioTrain, encoder, smoothFactor);
  }

  /**
   * @param tokens Corpus tokens
   * @param train Tag training set
   * @param _encoder A {@link BIOEncoder} for creating corpus datasets
   * @return
   */
  public static RRG mleEstimate(
      final int[] tokens, 
      final int[] train, 
      final BIOEncoder _encoder,
      final double smoothFactor) {
    final CombinedProb combined = getCombinedProb(tokens, train, smoothFactor);
    final double[] initTag = HMM.getInitTag(train, combined.numTags());
    assert tokens.length == train.length;
    final int last = tokens.length - 1;
    return new 
    RRG(_encoder, tokens, combined, initTag, tokens[last], train[last]);
  }

  /**
   * @param tokens
   * @param train
   * @return
   */
  private static CombinedProb getCombinedProb(
      final int[] tokens, 
      final int[] train,
      final double smoothFactor) {
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
    
    return CombinedProb.fromCounts(countsD, smoothFactor);
  }
}
