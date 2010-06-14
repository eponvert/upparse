package upparse;

import static java.lang.Math.*;

/**
 * Class for calculating emission probabilities
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class EmissionProbs {
  
  private final double[][] emiss;
  private double defaultProb;
  private double nvocab;

  public EmissionProbs(final double[][] _emiss) {
    emiss = _emiss;
    nvocab = emiss[0].length;
    defaultProb = log(1./nvocab);
  }

  public int numTags() {
    return emiss.length;
  }

  public int numTerms() {
    return emiss[0].length;
  }

  /**
   * @param t The tag index
   * @param w The term index
   * @return The probability state t emits w
   */
  public double getProb(final int t, final int w) {
    if (w >= numTerms())
      
      // TODO alter assumption that stop state == 0
      if (t == 0)
        return Double.NEGATIVE_INFINITY;
      else 
        return defaultProb;
    
    return emiss[t][w];
  }

  public void update(double[][] emissCount) {
    final int 
      ntag = numTags(), 
      nterm = numTerms();
    
    final double 
      stopStateSum = log(Util.sum(emissCount[0])),
      neginf = Double.NEGATIVE_INFINITY;

    int nvocabI = 0;
    for (int w = 0; w < nterm; w++) {
      if (emissCount[0][w] == 0) nvocabI++;
      emiss[0][w] = log(emissCount[0][w]) - stopStateSum;
      assert !Double.isNaN(emiss[0][w]);
    }
    
    nvocab = (double) nvocabI;
    defaultProb = log(1/nvocab);
    
    for (int j = 1; j < ntag; j++) { 
      final double sum = log(Util.sum(emissCount[j]) + nvocab);
      for (int w = 0; w < nterm; w++) {
        if (emiss[0][w] > neginf) emiss[j][w] = neginf;
        else emiss[j][w] = log(emissCount[j][w] + 1.) - sum;
        assert !Double.isNaN(emiss[j][w]);
      }
    }
  }

  public static EmissionProbs fromCounts(final double[][] emissCount) {
    final int m = emissCount.length, n = emissCount[0].length;
    final EmissionProbs e = new EmissionProbs(new double[m][n]);
    e.update(emissCount);
    return e;
  }
}
