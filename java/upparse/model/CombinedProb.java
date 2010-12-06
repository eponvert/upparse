package upparse.model;

import static java.lang.Math.*;
import static upparse.util.Util.*;

/**
 * Combined emission-transition probilities for right-regular grammar
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class CombinedProb {

  private final double[][][] prob;
  final HMM backoffHmm;
  final double[][] oovProb;
  private final double param;

  private CombinedProb(
      final double[][][] _prob,
      final double[][] _oovP,
      final HMM _backoffHmm,
      final double smoothParam) {
    prob = _prob;
    oovProb = _oovP;
    backoffHmm = _backoffHmm;
    param = smoothParam;
  }

  public int numTags() {
    return prob.length;
  }
  
  public int numTerms() {
    return prob[0][0].length;
  }
  
  public double arcprob(final int t1, final int w, final int t2) {
    if (w >= numTerms()) 
      return log(oovProb[t1][t2] * backoffHmm.nonLogTrans(t1,t2));
    
    else
      return log(prob[t1][t2][w] * backoffHmm.nonLogTrans(t1, t2));
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
      double[][][] countsD, final HMM backoffHmm, final double smooth) {
    
    final int ntag = countsD.length, nterm = countsD[0].length;
    final double[][][] prob = new double[ntag][ntag][nterm];
    final double[][] oovP = new double[ntag][ntag];
    final CombinedProb c = new CombinedProb(prob, oovP, backoffHmm, smooth);
    c.update(countsD);
    return c;
  }

  /**
   * Update the probability distribution using these tag-term-tag counts
   */
  public void update(double[][][] counts) {
    backoffHmm.update(counts);
    double[][][] _counts = switcheroo(counts);
    final int n = numTags();
    final double v = param * numTerms();
    for (int s = 0; s < n; s++) {
      for (int t = 0; t < n; t++) {
        double sum = sum(_counts[s][t]);
        oovProb[s][t] = param/sum;
        for (int w = 0; w < numTerms(); w++)
          prob[s][t][w] = (_counts[s][t][w] + param) / (sum + v);
      }
    }
  }

  private static double[][][] switcheroo(final double[][][] counts) {
    int ntag = counts.length;
    int nterm = counts[0].length;
    final double[][][] switched = new double[ntag][ntag][nterm];
    for (int t1 = 0; t1 < ntag; t1++)
      for (int t2 = 0; t2 < ntag; t2++)
        for (int w = 0; w < nterm; w++)
          switched[t1][t2][w] = counts[t1][w][t2];
    return switched;
  }

  public void checkSanity() {
    backoffHmm.checkSanity();
    int ntag = numTags(), nterm = numTerms();
    for (int t1 = 0; t1 < ntag; t1++) {
      for (int t2 = 0; t2 < ntag; t2++) {
        double s = sum(prob[t1][t2]);
        assert abs(s-1) < 1e-5 : s;
      }
    }
    for (int t1 = 0; t1 < ntag; t1++) {
      double sum = 0;
      for (int w = 0; w < nterm; w++) 
        for (int t2 = 0; t2 < ntag; t2++) {
          sum += exp(arcprob(t1, w, t2));
          assert !Double.isNaN(sum);
        }
      assert abs(sum-1) < 1e-5 : sum;
    }
  }

  /**
   * Creates a combined probability store directly from an array of log-
   * probabilities
   * @param prob The array of NxNxV log probabilities, where N = number of tags 
   * and V = vocabulary size
   * @param hmm The HMM used for transition probabilities
   * @param smooth The smoothing parameter for these probabilities
   * @return A new combined probability store
   */
  public static CombinedProb fromProb(
      double[][][] prob, HMM hmm, double smooth) {
    // convert the log probs to non-log
    double[][][] nlProb = 
      new double[prob.length][prob[0][0].length][prob[0].length];
    for (int x1 = 0; x1 < prob.length; x1++) {
      for (int x2 = 0; x2 < prob[x1].length; x2++) {
        for (int x3 = 0; x3 < prob[x1][x2].length; x3++) {
          nlProb[x1][x3][x2] = Math.exp(prob[x1][x2][x3]);
        }
      }
    }

    // Now, the fromCounts method should actually work.
    // This is stupid: to do this I need to switch around the probs, 
    // even though they're about to be switched back around.
    return fromCounts(nlProb, hmm, smooth);
  }
}
