package upparse;

import static java.lang.Math.*;
import static upparse.Util.*;

/**
 * Combined emission-transition probilities for right-regular grammar
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class CombinedProb {

  private final double[][][] prob;
  private final double[][] lastTok;
  private double nvocab;
  private final double[][] defaultProb;
  private final double smoothFactor;

  private CombinedProb(
      final double[][][] _prob, 
      final double[][] _lastTok,
      final double[][] _defaultProb,
      final double _smoothFactor) {
    prob = _prob;
    lastTok = _lastTok;
    defaultProb = _defaultProb;
    smoothFactor = _smoothFactor;
  }

  public int numTags() {
    return prob.length;
  }
  
  public int numTerms() {
    return prob[0].length;
  }
  
  public double getProb(final int j, final int w, final int k) {
    return w < numTerms() ? prob[j][w][k] : defaultProb[j][k];
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
      double[][][] countsD, double smoothFactor) {
    
    final int ntag = countsD.length, nterm = countsD[0].length;
    final double[][][] prob = new double[ntag][nterm][ntag];
    final double[][] lastTok = new double[ntag][nterm];
    final double[][] defaultProb = new double[ntag][ntag];
    final CombinedProb c = 
      new CombinedProb(prob, lastTok, defaultProb, smoothFactor);
    c.update(countsD);
    return c;
  }

  /**
   * Update the probability distribution using these tag-term-tag counts
   */
  public void update(double[][][] counts) {

    final int ntag = counts.length, nterm = counts[0].length;
    
    // We're going to estimate last token probabilities basically as
    // emission probabilities on the whole data-set.
    // TODO assuming stop state is 0
    final double
      stopStateSum = log(sum(counts[0])),
      neginf = Double.NEGATIVE_INFINITY;
    
    int nvocabI = 0;
    for (int w = 0; w < nterm; w++) {
      final double count = sum(counts[0][w]);
      if (count == 0) nvocabI++;
      lastTok[0][w] = log(count) - stopStateSum;
      for (int k = 0; k < ntag; k++) {
        prob[0][w][k] = log(counts[0][w][k]) - stopStateSum;
      }
      assert !Double.isNaN(lastTok[0][w]);
    }
    
    nvocab = (double) nvocabI;
    
    // Get transition probabilities for smoothing
    final double[][] trans = new double[ntag][ntag];
    for (int j = 0; j < ntag; j++) 
      for (int w = 0; w < nterm; w++) 
        for (int k = 0; k < ntag; k++) 
          trans[j][k] += counts[j][w][k];
    
    for (int j = 0; j < ntag; j++) {
      final double sum = sum(trans[j]);
      assert sum != 0;
      for (int k = 0; k < ntag; k++) trans[j][k] = trans[j][k]/sum;
    }
    
    for (int j = 1; j < ntag; j++) {
      final double sum = log(sum(counts[j]) + smoothFactor * nvocab);
      for (int w = 0; w < nterm; w++) {
        
        if (lastTok[0][w] > neginf) {
          lastTok[j][w] = neginf;
          for (int k = 0; k < ntag; k++) {
            prob[j][w][k] = neginf;
          }
        } else {
          for (int k = 0; k < ntag; k++) {
            lastTok[j][w] = log(sum(counts[j][w]) + smoothFactor) - sum;
            assert !Double.isNaN(counts[j][w][k]);
            prob[j][w][k] = 
              log(counts[j][w][k] + smoothFactor * trans[j][k]) - sum;
            assert !Double.isNaN(prob[j][w][k]);
          }
        }
      }
    }
    
    double lognvocab = log(nvocab);
    for (int j = 0; j < ntag; j++) 
      for (int k = 0; k < ntag; k++) 
        defaultProb[j][k] = log(trans[j][k]) - lognvocab;
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
