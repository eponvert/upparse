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
  private final int[] orig;
  private double[][] emiss;
  private double[][] trans;
  private double[] initTag;
  private int[][] tagdict;
  
  private HMM(
      final BIOEncoder _encoder, 
      final int[] _tokens,
      final double[][] _emiss, 
      final double[][] _trans, 
      final double[] _initTag) {
    encoder = _encoder;
    orig = _tokens;
    emiss = _emiss;
    trans = _trans;
    initTag = _initTag;
    tagdict = new int[emiss[0].length][];
    updateTagDict();
  }
  
  private void updateTagDict() {
    int ntag = emiss.length, nterm = emiss[0].length, numTags, w, t;
    int[] temp;
    final Double neginf = Double.NEGATIVE_INFINITY;
    
    for (w = 0; w < nterm; w++) {
      temp = new int[ntag];
      numTags = 0;
      for (t = 0; t < ntag; t++) 
        if (emiss[t][w] != neginf)
          temp[numTags++] = t;
      
      tagdict[w] = Arrays.copyOf(temp, numTags);
    }
  }

  public ClumpedCorpus reTagTrainCC() {
    int[] output = reTagTrain();
    return encoder.clumpedCorpusFromBIOOutput(orig, output);
  }

  public int[] reTagTrain() {
    return tag(orig);
  }
  
  private int[] tag(int[] tokens) {

    int ndata = tokens.length, ntag = trans.length, argmax;
    double eprob, maxval, prob;
    final double neginf = Double.NEGATIVE_INFINITY;
    
    double[][] viterbi = new double[ndata][ntag];
    for (double[] vstep: viterbi)
      Arrays.fill(vstep, Double.NEGATIVE_INFINITY);
    
    int[][] backpointer = new int[ndata][ntag];
    for (int[] bstep: backpointer)
      Arrays.fill(bstep, -1);
    
    for (int j: tagdict[tokens[0]])
      viterbi[0][j] = emiss[j][tokens[0]] + initTag[j];
    
    for (int i = 1; i < ndata; i++) {
      for (int k: tagdict[tokens[i]]) {
        eprob = emiss[k][tokens[i]];
        maxval = neginf;
        argmax = -1;
        
        for (int j: tagdict[tokens[i-1]]) {
          prob = viterbi[i-1][j] + trans[j][k] + eprob;
          if (prob > maxval) {
            argmax = j;
            maxval = prob;
          }
        }
        
        viterbi[i][k] = maxval;
        backpointer[i][k] = argmax;
      }
    }
      
    int[] output = new int[tokens.length];
    for (int i = ndata-1; i > 0; i--)
      output[i-1] = backpointer[i][output[i]];

    return output;
  }

  public void emUpdateFromTrain() {
    // TODO method stub
  }

  public double currPerplex() {
    return perplex;
  }
  
  private abstract static class TransCountUpd {
    double[][] train;
    boolean[][] constr;
    
    TransCountUpd(double[][] _train, boolean[][] _constr) {
      train = _train;
      constr = _constr;
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
      super(_train, null);
    }

    @Override
    public double get(int i, int j, int k) {
      return train[i-1][j] * train[i][k];
    }
  }
  
  private static class UniformTransCountUpd extends TransCountUpd {
    
    UniformTransCountUpd(double[][] _train, boolean[][] _constr) {
      super(_train, _constr);
    }

    @Override
    public double get(int i, int j, int k) {
      // TODO Auto-generated method stub
      return 0;
    }
  }
  
  private static class ByTagTransCountUpd extends TransCountUpd {
    
    ByTagTransCountUpd(double[][] _train, boolean[][] _constr) {
      super(_train, _constr);
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
  
  private static double[][] getEmiss(final int[] tokens, final double[][] train) {
    assert tokens.length == train.length;
    int nterm = arrayMax(tokens) + 1, ntag = train[0].length, i, j, w;
    double sum;
    
    final double[][] emissCount = new double[ntag][nterm];
    for (i = 0; i < tokens.length; i++) 
      for (j = 0; j < ntag; j++)
        emissCount[j][tokens[i]] += train[i][j];
    
    final double[][] emiss = new double[ntag][nterm];
    for (j = 0; j < ntag; j++) {
      sum = 0.;
      for (w = 0; w < nterm; w++) 
        sum += emissCount[j][w];
      sum = log(sum);
      
      for (w = 0; w < nterm; w++)
        emiss[j][w] = log(emissCount[j][w]) - sum;
    }
    return emiss;
  }
  
  private static double[][] getEmiss(final int[] tokens, final int[] tags) {
    assert tokens.length == tags.length;
    
    int nterm = arrayMax(tokens) + 1, ntag = arrayMax(tags) + 1, i, j, w;
    double sum;

    final int[][] emissCount = new int[ntag][nterm];
    for (i = 0; i < tokens.length; i++)
      emissCount[tags[i]][tokens[i]]++;

    double[][] emissCountD = new double[ntag][nterm];
    for (j = 0; j < ntag; j++) 
      for (w = 0; w < nterm; w++)
        emissCountD[j][w] = (double) emissCount[j][w];
    
    double[][] emiss = new double[ntag][nterm];
    for (j = 0; j < ntag; j++) {
      sum = 0.;
      for (w = 0; w < nterm; w++)
        sum += emissCountD[j][w];
      sum = log(sum);

      for (w = 0; w < nterm; w++)
        emiss[j][w] = log(emissCountD[j][w]) - sum;
    }

    return emiss;
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
    
    final double[][] emiss = getEmiss(tokens, train);
    final double[] initTag = getInitTag(train);
    final double[][] trans = getTrans(train, constraints, constrMethod);
    return new HMM(encoder, tokens, emiss, trans, initTag);
  }
  
  public static HMM mleEstimate(
      int[] tokens, int[] tags, BIOEncoder encoder) {
    final double[][] emiss = getEmiss(tokens, tags);
    int ntag = emiss.length;
    final double[] initTag = getInitTag(tags, ntag);
    final double[][] trans = getTrans(tags, ntag);
    return new HMM(encoder, tokens, emiss, trans, initTag);
  }
  
  public static HMM mleEstimate(ClumpedCorpus corpus, BIOEncoder encoder) 
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
