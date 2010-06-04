package upparse;

import java.io.*;
import java.util.*;

import static java.lang.Math.*;

/**
 * Hidden Markov model 
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class HMM {

  private double perplex = 1e10;
  private final BIOEncoder encoder;
  final int[] orig;
  EmissionProbs emiss;
  double[][] trans;
  private double[] initTag;
  private int[][] tagdict;

  private static final double LOGEPS = -1e+16;

  private HMM(
      final BIOEncoder _encoder, 
      final int[] _tokens,
      final EmissionProbs _emiss, 
      final double[][] _trans, 
      final double[] _initTag) {
    encoder = _encoder;
    orig = _tokens;
    emiss = _emiss;
    trans = _trans;
    initTag = _initTag;
    tagdict = new int[emiss.numTerms()][];
    updateTagDict();
    checkSanity();
  }

  private void updateTagDict() {
    int ntag = emiss.numTags(), nterm = emiss.numTerms(), numTags, w, t;
    int[] temp;
    final Double neginf = Double.NEGATIVE_INFINITY;

    for (w = 0; w < nterm; w++) {
      temp = new int[ntag];
      numTags = 0;
      for (t = 0; t < ntag; t++) 
        if (emiss.getProb(t,w) != neginf)
          temp[numTags++] = t;

      tagdict[w] = Arrays.copyOf(temp, numTags);
    }
  }

  public void emUpdateFromTrain() {
    emUpdateFrom(orig);
  }

  public void emUpdateFrom(int[] data) {
    int 
      ndata = data.length,
      last = ndata - 1,
      ntag = trans.length,
      nvocab = emiss.numTerms();

    final double neginf = Double.NEGATIVE_INFINITY;
    
    // Forward-backward algorithm

    double[][] 
      forward = new double[ndata][ntag], 
      backward = new double[ndata][ntag];

    for (int d = 0; d < ndata; d++) {
      for (int t = 0; t < ntag; t++) {
        forward[d][t] = neginf;
        backward[d][t] = neginf;
      }
    }

    for (int j = 0; j < ntag; j++) {
      forward[0][j] = emiss.getProb(j, data[0]) + initTag[j];
    }

    // Forward probabilities
    for (int n = 1; n < ndata; n++) {
      for (int j: tagdict[data[n-1]]) {
        for (int k: tagdict[data[n]]) {
          double arcprob = trans[j][k] + emiss.getProb(k, data[n]);
          double forwUpd = forward[n-1][j] + arcprob;
          forward[n][k] = logadd(forward[n][k], forwUpd);
        }
      }
    }

    // TODO check end conditions
    double forwTotal = forward[last][0];
    backward[last][0] = 0;

    // Backward probabilities. Also collecting new emission and transition 
    // counts as we go
    final double[][] emissCount = new double[ntag][nvocab];
    final double[][] transCount = new double[ntag][ntag];

    for (int n = last; n > 0; n--) {
      for (int k: tagdict[data[n]]) {
        double pFwd = forward[n][k];
        double pBwd = backward[n][k];

        if (pFwd != neginf && pBwd != neginf) {
          final double emissUpd = exp(pFwd + pBwd - forwTotal);
          emissCount[k][data[n]] += emissUpd;


          for (int j: tagdict[data[n-1]]) {
            final double tprob = trans[j][k];
            final double eprob = emiss.getProb(k, data[n]);
            final double arcprob = tprob + eprob;
            final double backUpd = arcprob + backward[n][k];
            backward[n-1][j] = logadd(backward[n-1][j], backUpd);

            pFwd = forward[n-1][j];

            if (pFwd != neginf && pBwd != neginf) {
              final double transUpd = exp(pFwd + pBwd + arcprob - forwTotal);
              transCount[j][k] += transUpd;
            }
          }
        }
      }
    }

    for (int j = 0; j < ntag; j++)
      emissCount[j][data[0]] += exp(forward[0][j] + backward[0][j] - forwTotal); 

    // Update the HMM probabilities from the new counts
    
    emiss.update(emissCount);

    for (int j = 0; j < ntag; j++) {
      final double sum = log(sum(transCount[j]));
      for (int k = 0; k < ntag; k++) {
        trans[j][k] = log(transCount[j][k]) - sum;
        assert !Double.isNaN(trans[j][k]);
      }
    }


    updateTagDict();
    checkSanity();

    // Get perplexity
    perplex = exp(-forward[last][0]/ndata);
  }

  private void checkSanity() {
    for (int i = 0; i < emiss.numTags(); i++) {
      double s = 0, p; 
      for (int j = 0; j < emiss.numTerms(); j++) {
        p = emiss.getProb(i,j);
        assert !Double.isNaN(p);
        s += Math.exp(p);
      }
      assert abs(s - 1) < 1e-5;
    }

    for (int i = 0; i < trans.length; i++) {
      double s = 0.;
      for (int j = 0; j < trans[i].length; j++)
        s += exp(trans[i][j]);
      assert abs(s - 1) < 1e-5;
    }
  }

  static double sum(final double[] ds) {
    double s = 0;
    for (double d: ds)
      s += d;
    return s;
  }

  private static double logadd(final double x, final double y) {
    assert !Double.isNaN(y);
    assert !Double.isNaN(x);
    if (x <= LOGEPS) 
      return y;
    else if (y <= LOGEPS) 
      return x;
    else if (y <= x) 
      return x + log(1 + exp(y-x)); 
    else 
      return y + log(1 + exp(x-y));
  }
  
  public ChunkedSegmentedCorpus tagCC(int[] testCorpus) throws HMMError {
    int[] output = tag(testCorpus);
    return encoder.clumpedCorpusFromBIOOutput(testCorpus, output);
  }

  private int[] tag(int[] tokens) {

    int ndata = tokens.length;
    double eprob, tprob, pprob;
    MaxVals m;

    double[][] viterbi = new double[ndata][];

    double[] v;

    int[][] backpointer = new int[ndata][];

    int[] tags, _tags = tagdict[tokens[0]];
    assert _tags.length != 0;
    int n, j, k;

    viterbi[0] = new double[_tags.length];
    for (j = 0; j < _tags.length; j++) {
      eprob = emiss.getProb(_tags[j], tokens[0]);
      tprob = initTag[_tags[j]];
      viterbi[0][j] = eprob + tprob;
    }

    for (n = 1; n < ndata; n++) {
      tags = tagdict[tokens[n]];

      viterbi[n] = new double[tags.length];
      backpointer[n] = new int[tags.length];

      for (k = 0; k < tags.length; k++) {
        eprob = emiss.getProb(tags[k], tokens[n]);

        v = new double[_tags.length];
        for (j = 0; j < _tags.length; j++) {
          tprob = trans[_tags[j]][tags[k]];
          pprob = viterbi[n-1][j];
          v[j] = pprob + tprob + eprob;
        }

        m = new MaxVals(v); 
        viterbi[n][k] = m.max;
        backpointer[n][k] = m.argmax;

        assert backpointer[n] != null;
      }

      _tags = tags;
    }

    m = new MaxVals(viterbi[ndata-1]);

    int[] output = new int[ndata];
    output[ndata-1] = m.argmax;

    // first pass gets the indices for the tags in the tagdict
    for (int i = ndata-1; i > 0; i--) {
      int curr = output[i];
      int next = backpointer[i][curr];
      output[i-1] = next;
    }

    // second pass fills in the actual tags from the tagdict
    for (int i = 0; i < ndata; i++)
      output[i] = tagdict[tokens[i]][output[i]];

    return output;
  }

  private static class MaxVals {
    int argmax = -1;
    double max = Double.NEGATIVE_INFINITY;
    public MaxVals(final double[] vals) {
      for (int i = 0; i < vals.length; i++) {
        if (vals[i] > max) {
          argmax = i;
          max = vals[i];
        }
      }
    }
  }

  public double currPerplex() {
    return perplex;
  }

  private abstract static class TransCountUpd {
    double[][] train;

    TransCountUpd(double[][] _train) {
      train = _train;
    }

    public abstract double get(int i, int j, int k);
  }

  private static boolean[][] getTagConstraintsFromFile(
      String fname, Alpha alpha) throws IOException {

    int n = alpha.size();
    boolean[][] constr = new boolean[n][n];
    BufferedReader br = new BufferedReader(new FileReader(new File(fname)));
    String s;
    String[] t;
    while ((s = br.readLine()) != null) {
      t = s.split(" ");
      assert t.length == 2;
      constr[alpha.getCode(t[0])][alpha.getCode(t[1])] = true;
    }
    return constr;
  }

  private static class NoConstraintsTransCountUpd extends TransCountUpd {

    NoConstraintsTransCountUpd(double[][] _train) {
      super(_train);
    }

    @Override
    public double get(final int i, final int j, final int k) {
      return train[i-1][j] * train[i][k];
    }
  }

  private static class UniformTransCountUpd extends TransCountUpd {

    final double[][] tagAdj;

    UniformTransCountUpd(double[][] _train, boolean[][] constr) {
      super(_train);
      int ntag = train[0].length;
      int[] possibleTags = new int[ntag];
      for (int j = 0; j < ntag; j++) 
        for (int k = 0; k < ntag; k++) 
          if (constr[j][k]) 
            possibleTags[j]++;

      tagAdj = new double[ntag][ntag];
      for (int j = 0; j < ntag; j++) 
        for (int k = 0; k < ntag; k++)
          if (constr[j][k])
            tagAdj[j][k] = (double) ntag / (double) possibleTags[j];
    }

    @Override
    public double get(final int i, final int j, final int k) {
      return tagAdj[j][k] * train[i-1][j] * train[i][k];
    }
  }

  private static class ByTagTransCountUpd extends TransCountUpd {

    ByTagTransCountUpd(double[][] _train, boolean[][] _constr) {
      super(_train);
    }

    @Override
    public double get(int i, int j, int k) {
      // TODO Auto-generated method stub
      return 0;
    }
  }

  private static TransCountUpd getTransCountUpd(
      final boolean[][] constraints, 
      final String constrMethod,  
      final double[][] train) 
  throws HMMError {

    if (constraints == null)
      return new NoConstraintsTransCountUpd(train);

    else if (constrMethod.equals("uniform"))
      return new UniformTransCountUpd(train, constraints);

    else if (constrMethod.equals("bytag"))
      return new ByTagTransCountUpd(train, constraints);

    else
      throw new HMMError(
          "Unknown transition probability constraint method " + constrMethod); 
  }

  /**
   * @param alpha Must be initialized with the tags expected to be seen
   * @throws IOException if there is any trouble with the tag constraints file
   * @throws HMMError if there is some problem with the constraints
   */
  public static HMM mleEstimate(
      int[] tokens, 
      double[][] train, 
      BIOEncoder encoder, 
      Alpha alpha, 
      String constrFname, 
      String constrMethod) 
  throws IOException, HMMError {
    boolean[][] constraints = 
      getTagConstraintsFromFile(constrFname, alpha);
    return mleEstimate(tokens, train, encoder, constraints, constrMethod);
  }

  public static HMM mleEstimate(
      final int[] tokens, final double[][] train, final BIOEncoder encoder) 
  throws HMMError {
    return mleEstimate(tokens, train, encoder, null, null);
  }

  private static EmissionProbs getEmiss(final int[] tokens, final double[][] train) {
    assert tokens.length == train.length;
    int nterm = arrayMax(tokens) + 1, ntag = train[0].length, i, j;

    final double[][] emissCount = new double[ntag][nterm];
    for (i = 0; i < tokens.length; i++) 
      for (j = 0; j < ntag; j++)
        emissCount[j][tokens[i]] += train[i][j];
    
    return EmissionProbs.fromCounts(emissCount);
  }

  private static EmissionProbs getEmiss(final int[] tokens, final int[] tags) {
    assert tokens.length == tags.length;

    int nterm = arrayMax(tokens) + 1, ntag = arrayMax(tags) + 1, i, j, w;

    final int[][] emissCount = new int[ntag][nterm];
    for (i = 0; i < tokens.length; i++)
      emissCount[tags[i]][tokens[i]]++;

    double[][] emissCountD = new double[ntag][nterm];
    for (j = 0; j < ntag; j++) 
      for (w = 0; w < nterm; w++)
        emissCountD[j][w] = (double) emissCount[j][w];
    
    return EmissionProbs.fromCounts(emissCountD);
  }

  private static double[] getInitTag(final double[][] train) {
    int ntag = train[0].length, k;
    double[] initTag = new double[ntag];
    for (k = 0; k < ntag; k++)
      initTag[k] = log(train[0][k]);
    return initTag;
  }

  private static double[] getInitTag(final int[] tag, final int ntag) {
    double[] initTag = new double[ntag];
    double log1 = log(1.), log0 = log(0.);
    for (int j = 0; j < ntag; j++)
      initTag[j] = j == tag[0] ? log1 : log0;
    return initTag;
  }

  private static double[][] getTrans(
      final double[][] train, 
      final boolean[][] constr, 
      final String constrMethod) throws HMMError {

    TransCountUpd getUpd = getTransCountUpd(constr, constrMethod, train);
    int ntag = train[0].length, i, j, k;
    double[][] transCount = new double[ntag][ntag];
    double sum;

    for (i = 1; i < train.length; i++)
      for (j = 0; j < ntag; j++)
        for (k = 0; k < ntag; k++)
          transCount[j][k] += getUpd.get(i,j,k);

    double[][] trans = new double[ntag][ntag];
    for (j = 0; j < ntag; j++) {
      sum = 0;
      for (k = 0; k < ntag; k++)
        sum += transCount[j][k];
      sum = log(sum);

      for (k = 0; k < ntag; k++)
        trans[j][k] = log(transCount[j][k]) - sum;
    }

    return trans;
  }

  private static double[][] getTrans(final int[] tags, final int ntag) {

    int[][] transCount = new int[ntag][ntag];
    int i, j, k;
    double sum;

    for (i = 1; i < tags.length; i++)
      transCount[tags[i-1]][tags[i]]++;

    double[][] transCountD = new double[ntag][ntag];
    for (j = 0; j < ntag; j++)
      for (k = 0; k < ntag; k++)
        transCountD[j][k] = (double) transCount[j][k];

    double[][] trans = new double[ntag][ntag];
    for (j = 0; j < ntag; j++) {
      sum = 0.;
      for (k = 0; k < ntag; k++) 
        sum += transCountD[j][k];
      sum = log(sum);

      for (k = 0; k < ntag; k++)
        trans[j][k] = log(transCount[j][k]) - sum;
    }

    return trans;
  }

  public static HMM mleEstimate(
      int[] tokens, double[][] train, BIOEncoder encoder,
      boolean[][] constraints, String constrMethod) throws HMMError {

    final EmissionProbs emiss = getEmiss(tokens, train);
    final double[] initTag = getInitTag(train);
    final double[][] trans = getTrans(train, constraints, constrMethod);
    return new HMM(encoder, tokens, emiss, trans, initTag);
  }

  public static HMM mleEstimate(
      int[] tokens, int[] tags, BIOEncoder encoder) {
    final EmissionProbs emiss = getEmiss(tokens, tags);
    int ntag = emiss.numTags();
    final double[] initTag = getInitTag(tags, ntag);
    final double[][] trans = getTrans(tags, ntag);
    return new HMM(encoder, tokens, emiss, trans, initTag);
  }

  public static HMM mleEstimate(ChunkedSegmentedCorpus corpus, BIOEncoder encoder) 
  throws HMMError {
    int[] tokens = encoder.tokensFromClumpedCorpus(corpus);
    int[] bioTrain = encoder.bioTrain(corpus, tokens.length);
    return mleEstimate(tokens, bioTrain, encoder);
  }

  private static int arrayMax(int[] t) {
    assert t.length > 0;
    int v = t[0];
    for (int i = 1; i < t.length; i++) 
      if (t[i] > v) 
        v = t[i];
    return v;
  }
}
