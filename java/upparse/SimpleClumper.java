package upparse;

import java.util.*;

/**
 * Simple count oriented clumper
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class SimpleClumper {
  
  final Alpha alpha = new Alpha();
  private final StopSegmentCorpus corpus;
  private final NgramCounts bigramCounts = new NgramCounts();
  private final int stopv;
  private final int[] factor;

  public SimpleClumper(BasicCorpus basicCorpus, int[] factor, String stop) {
    stopv = alpha.getCode(stop);
    corpus = new StopSegmentCorpus(basicCorpus.compiledCorpus(alpha), stopv);
    this.factor = factor;
    
    int[][][] _corpus = corpus.corpus;
    int n, j;
    for (int[][] s: _corpus) {
      for (int[] seg: s) {
        n = seg.length;
        bigramCounts.incr(stopv, seg[0]);
        bigramCounts.incr(seg[n-1], stopv);
        for (j = 0; j < seg.length-1; j++)
          bigramCounts.incr(seg[j], seg[j+1]);
      }
    }
  }
  
  /**
   * Create clumped version of the original training corpus
   */
  public ClumpedCorpus getClumpedCorpus() {
    
    int[][][] _corpus = corpus.corpus;
    int[][] _segments;
    int[] _terms;
    
    double[][] pyr;
    
    int[][][][] clumpedCorpus = new int[_corpus.length][][][];

    int[][] clumpsTmp;
    
    int i, j, k, m, n, pyrI, pyrJ, numClump, index1, index2;
    
    double dontClumpVal, count;
    
    boolean[] toclump;
    
    for (i = 0; i < _corpus.length; i++) {
      _segments = _corpus[i];
      
      clumpedCorpus[i] = new int[_segments.length][][];
      
      for (j = 0; j < _segments.length; j++) {
        _terms = _segments[j];
        
        n = _terms.length;
        m = Math.min(_terms.length-1, factor.length);
        pyr = new double[m][];
        
        for (pyrI = 0; pyrI < m; pyrI++) 
          pyr[pyrI] = new double[n-pyrI-1];

        for (pyrI = m-1; pyrI >= 0; pyrI--) {
          for (pyrJ = 0; pyrJ < n-pyrI-1; pyrJ++) {
            count = bigramCounts.get(_terms[pyrJ], _terms[pyrI+pyrJ+1]);
            pyr[pyrI][pyrJ] = factor[pyrI] * count + sumParents(pyr,pyrI,pyrJ,m,n);
          }
        }
        
        toclump = new boolean[_terms.length-1];
        for (k = 0; k < _terms.length-1; k++) {
          dontClumpVal =
            bigramCounts.get(_terms[k], stopv) + 
            bigramCounts.get(stopv, _terms[k+1]);
          
          toclump[k] = pyr[0][k] >= dontClumpVal; 
        }
        
        clumpsTmp = new int[_terms.length][];
        numClump = 0;
        
        index1 = index2 = 0;
        while (index2 <= toclump.length) {
          while (index2 < toclump.length && toclump[index2]) 
            index2++;
            
          index2++;
          clumpsTmp[numClump++] = Arrays.copyOfRange(_terms, index1, index2);
          index1 = index2;
        }
      
        clumpedCorpus[i][j] = Arrays.copyOf(clumpsTmp, numClump); 
      }
    }
    
    return ClumpedCorpus.fromArrays(clumpedCorpus, alpha);
  }
  
  private static double sumParents(double[][] pyr, int i, int j, int m, int n) {
    assert 0 <= i;
    assert i < m;
    assert 0 <= j;
    assert j < n-i-1;
    
    if (i == m-1) return 0.;
    else if (j == 0) return pyr[i+1][0];
    else if (j == n-i-2) return pyr[i+1][j-1];
    else return pyr[i+1][j-1] + pyr[i+1][j];
  }
}
