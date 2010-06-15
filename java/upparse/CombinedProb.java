package upparse;

import static java.lang.Math.*;
import static upparse.Util.*;

/**
 * Combined emission-transition probilities for right-regular grammar
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class CombinedProb {

  private final double[][][] prob;
  private final double[] logSum;
  private final double scaleFactor;
  final HMM backoffHmm;

  private CombinedProb(
      final double[][][] _prob, final double[] _logSum,  
      final HMM _backoffHmm, double _scaleFactor) {
    prob = _prob;
    logSum = _logSum;
    backoffHmm = _backoffHmm;
    scaleFactor = _scaleFactor;
  }

  public int numTags() {
    return prob.length;
  }
  
  public int numTerms() {
    return prob[0].length;
  }
  
  public double arcprob(final int j, final int w, final int k) {
    return w < numTerms() ? prob[j][w][k] : backoffHmm.arcprob(j, w, k) - logSum[j];
  }

  /**
   * @param t The tag
   * @param i The token
   * @return The probability of this tag/token pair at the end of the sequence
   */
  public double lastTok(int t, int i) {
    return backoffHmm.emiss.getProb(t, i);
  }

  public static CombinedProb fromCounts(
      double[][][] countsD, final HMM backoffHmm, double scaleFactor) {
    
    final int ntag = countsD.length, nterm = countsD[0].length;
    final double[][][] prob = new double[ntag][nterm][ntag];
    final double[] logSum = new double[ntag];
    final CombinedProb c = 
      new CombinedProb(prob, logSum, backoffHmm, scaleFactor);
    c.update(countsD);
    return c;
  }

  /**
   * Update the probability distribution using these tag-term-tag counts
   */
  public void update(double[][][] counts) {
    backoffHmm.update(counts); 
    for (int t = 0; t < numTags(); t++) {
      logSum[t] = log(sum(counts[t]) + scaleFactor);
      for (int w = 0; w < numTerms(); w++) {
        for (int _t = 0; _t < numTags(); _t++) {
          prob[t][w][_t] = 
            log(counts[t][w][_t] + scaleFactor * exp(backoffHmm.arcprob(t, w, _t))) 
            - logSum[t];
        }
      }
    }
  }

  public void checkSanity() {
    backoffHmm.checkSanity();
    for (int t1 = 0; t1 < prob.length; t1++) {
      double sum = 0;
      for (int w = 0; w < prob[t1].length; w++) 
        for (int t2 = 0; t2 < prob[t1][w].length; t2++) {
          sum += exp(prob[t1][w][t2]);
          assert !Double.isNaN(sum);
        }
      assert abs(sum-1) < 1e-5;
    }
  }
}
