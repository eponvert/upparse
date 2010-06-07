package upparse;

import static java.lang.Math.*;

/**
 * Combined emission-transition probilities for right-regular grammar
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class CombinedProb {

  private final double[][][] prob;
  private final double[][] lastTok;
  public final int origLastSeenToken;
  public final int origLastSeenTag;

  private CombinedProb(
      final double[][][] _prob, 
      final double[][] _lastTok, 
      final int _origLastSeenToken, 
      final int _origLastSeenTag) {
    prob = _prob;
    lastTok = _lastTok;
    origLastSeenToken = _origLastSeenToken;
    origLastSeenTag = _origLastSeenTag;
  }

  public int numTags() {
    return prob.length;
  }
  
  public int numTerms() {
    return prob[0].length;
  }
  
  public double getProb(final int j, final int w, final int k) {
    return prob[j][w][k];
  }

  /**
   * @param t The tag
   * @param i The token
   * @return The probability of this tag/token pair at the end of the sequence
   */
  public double lastTok(int t, int i) {
    return lastTok[t][i];
  }

  public static CombinedProb fromCounts(
      double[][][] countsD, int lastSeenToken, int lastSeenTag) {
    
    final int ntag = countsD.length, nterm = countsD[0].length;
    final double[][][] prob = new double[ntag][nterm][ntag];
    final double[][] lastTok = new double[ntag][nterm];
    
    CombinedProb c = 
      new CombinedProb(prob, lastTok, lastSeenToken, lastSeenTag);
    
    c.update(countsD);
    return c;
  }

  private static double sum(double[][] ds) {
    double s = 0;
    for (double[] a: ds)
      for (double b: a)
        s += b;
    return s;
  }

  /**
   * Update the probability distribution using these tag-term-tag counts
   */
  public void update(double[][][] counts) {
    
    // TODO implement smoothing
    final int ntag = counts.length, nterm = counts[0].length;
    
    for (int j = 0; j < ntag; j++) {
      final double sum = log(sum(counts[j]));
      assert !Double.isNaN(sum);
      assert sum != Double.NEGATIVE_INFINITY;
      for (int w = 0; w < nterm; w++) {
        for (int k = 0; k < ntag; k++) {
          assert !Double.isNaN(counts[j][w][k]);
          prob[j][w][k] = log(counts[j][w][k]) - sum;
          assert !Double.isNaN(prob[j][w][k]);
        }
      }
    }
    
    final double neginf = Double.NEGATIVE_INFINITY;
    final double uniformprob = -log(nterm);
    for (int t = 0; t < ntag; t++)
      for (int w = 0; w < nterm; w++)
        lastTok[t][w] = uniformprob;
    
    for (int w = 0; w < nterm; w++)
      lastTok[origLastSeenTag][w] = neginf;
    
    lastTok[origLastSeenTag][origLastSeenToken] = 0;
  }

  public void checkSanity() {
    for (int t1 = 0; t1 < prob.length; t1++) {
      double sum = 0;
      for (int w = 0; w < prob[t1].length; w++) 
        for (int t2 = 0; t2 < prob[t1][w].length; t2++) {
          sum += exp(prob[t1][w][t2]);
          assert !Double.isNaN(sum);
        }
      assert abs(sum-1) < 1e-5;
    }
    
    for (int t = 0; t < lastTok.length; t++) {
      double sum = 0;
      for (int w = 0; w < lastTok[t].length; w++) 
        sum += exp(lastTok[t][w]);
      assert abs(sum-1) < 1e-5;
    }
  }
}
