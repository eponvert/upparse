package upparse;

import static java.lang.Math.*;

import static java.util.Arrays.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public abstract class SequenceModel {
  
  private final int[] orig;
  private final BIOEncoder encoder;
  private double perplex = 1e16;
  private int[][] tagdict;
  
  public SequenceModel(
      final BIOEncoder _encoder, final int[] _orig, final int[][] _tagdict) {
    encoder = _encoder;
    orig = _orig;
    tagdict = _tagdict;
  }

  /** Tag the corpus, return structured corpus */
  public final ChunkedSegmentedCorpus tagCC(int[] testCorpus)
  throws SequenceModelError, EncoderError {
    int[] output = tag(testCorpus);
    return getEncoder().clumpedCorpusFromBIOOutput(testCorpus, output);
  }
  
  public final int[] getOrig() {
    return orig;
  }

  public final BIOEncoder getEncoder() {
    return encoder;
  }

  /** Update model using (new) data */ 
  public final void emUpdateFrom(final int[] data) {
    int 
      ndata = data.length,
      last = ndata - 1,
      ntag = numTags(),
      nvocab = numTerms();
    
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
      forward[0][j] = initTagProb(j);
    
    // Times n+1 to N
    for (int n = 1; n < ndata; n++) {
      for (int j: getTagdict(data[n-1])) {
        for (int k: getTagdict(data[n])) {
          final double forwUpd = forward[n-1][j] + arcprob(j, data[n-1], k);
          forward[n][k] = Util.logadd(forward[n][k], forwUpd);
        }
      }
    }
    
    // Finally update forward probabilities with last token probs
    for (int j: getTagdict(data[last-1])) {
      for (int k: getTagdict(data[last])) {
        final double forwUpd = 
          forward[last-1][j] + termProb(k, data[last]);
        forward[last][j] = Util.logadd(forward[last][j], forwUpd);
      }
    }
    
    double forwTotal = neginf;
    for (int t: getTagdict(data[last])) {
      forwTotal = Util.logadd(forwTotal, forward[last][t]);
      backward[last][t] = termProb(t, data[last]);
    }
    
    // TODO check end conditions
    // for now assuming last tag is STOP with index 0
    // 
    // final double forwTotal = forward[last][0];
    // backward[last][0] = 0;
    
    // Backward probabilities. Also collecting new training counts as we go
    final double[][][] counts = new double[ntag][nvocab][ntag];
    
    for (int n = last; n > 0; n--) {
      for (int k: getTagdict(data[n])) {
        for (int j: getTagdict(data[n-1])) {
          final double 
            fwd = forward[n-1][j], 
            bwd = backward[n][k],
            aprob = arcprob(j, data[n-1], k),
            backUpd = aprob + bwd;
          
          backward[n-1][j] = Util.logadd(backward[n-1][j], backUpd);
          
          // TODO shouldn't need to check for neginfs
          if (aprob != neginf && fwd != neginf && bwd != neginf) {
            final double
              logupd = fwd + bwd + aprob - forwTotal,
              upd = exp(logupd);
            final int w = data[n-1];
            counts[j][w][k] += upd; 
          }
        }
      }
    }
    
    update(counts);
    
    updateTagDict();
    checkSanity();

    // Get perplexity
    setPerplex(exp(-forwTotal/ndata));
  }
  
  public void updateTagDict() {
    int[] temp;
    final Double neginf = Double.NEGATIVE_INFINITY;

    for (int w = 0; w < numTerms(); w++) {
      temp = new int[numTags()];
      int ntag = 0;
      for (int t = 0; t < numTags(); t++) 
        if (termProb(t,w) != neginf)
          temp[ntag++] = t;

      tagdict[w] = copyOf(temp, ntag);
    }
  }

  private final void setPerplex(final double p) {
    perplex = p;
  }

  /** Update model using expectation maximization on original training data */ 
  public final void emUpdateFromTrain() {
    emUpdateFrom(orig);
  }

  public final int[] getTagdict(int tag) {
    return tagdict[tag];
  }

  /** Viterbi tagger for right-regular grammars */
  public final int[] tag(int[] tokens) {
    int ndata = tokens.length;
    
    final double[][] viterbi = new double[ndata][];
    final int[][] backpointer = new int[ndata][];
    int[] tags, _tags = getTagdict(tokens[0]);
    
    double[] v;
    MaxVals mv;
    
    viterbi[0] = new double[_tags.length];
    for (int j = 0; j < _tags.length; j++)
      viterbi[0][j] = initTagProb(_tags[j]);

    for (int t = 1; t < ndata; t++) {
      tags = getTagdict(tokens[t]);
      viterbi[t] = new double[tags.length];
      backpointer[t] = new int[tags.length];
      final double[][] arcprobs = new double[_tags.length][tags.length];
      final int token = tokens[t-1];
      for (int j = 0; j < _tags.length; j++) 
        for (int k = 0; k < tags.length; k++) 
          arcprobs[j][k] = arcprob(_tags[j], token, tags[k]);
      
      for (int k = 0; k < tags.length; k++) {
        v = new double[_tags.length];
        for (int j = 0; j < _tags.length; j++)  
          v[j] = viterbi[t-1][j] + arcprobs[j][k];

        mv = new MaxVals(v);
        viterbi[t][k] = mv.max;
        backpointer[t][k] = mv.argmax;
      }
      
      mv = new MaxVals(viterbi[t]);
      assert mv.argmax != -1;
      
      _tags = tags;
    }
    
    final int last = ndata - 1;
    final int[] lastTags = getTagdict(tokens[last]); 
    for (int t = 0; t < lastTags.length; t++)
      viterbi[last][t] += termProb(lastTags[t], tokens[last]);
   
    mv = new MaxVals(viterbi[last]);
    assert mv.argmax != -1;
    
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
      output[i] = getTagdict(tokens[i])[output[i]];
    
    return output;
  }

  /** @return perplexity of current EM step */ 
  public final double currPerplex() {
    return perplex;
  }

  /** Check that the probability distributions are not degenerate */
  public void checkSanity() {
    for (int t = 0; t < numTags(); t++) {
      double sum = 0;
      for (int w = 0; w < numTerms(); w++) 
        for (int _t = 0; _t < numTags(); _t++) 
          sum += exp(arcprob(t, w, _t));
      
      assert abs(sum - 1.) < 1e-5;
    }
  }

  /**
   * @param j Previous tag
   * @param w Previous token
   * @param k Current tag
   * @return The arc probability p(tag_k, word_w | tag_j)
   */
  public abstract double arcprob(int j, int w, int k);

  /**
   * Update the model with combined emission/transition counts
   * @param counts 
   */
  public abstract void update(double[][][] counts);
  
  public abstract int numTags();
  
  public abstract int numTerms();
  
  public abstract double initTagProb(int tag);
  
  public abstract double termProb(int tag, int term);
}
