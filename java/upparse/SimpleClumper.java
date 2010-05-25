package upparse;

import java.util.*;

/**
 * Simple count oriented clumper
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class SimpleClumper {
  
  private final Alpha alpha = new Alpha();
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
   * Iterate over the sentences of the original training corpus, returning
   * strings representing clumped sentences
   */
  public Iterable<String> clumpedCorpusStr() {
    final int[][][][] clumpedCorpus = getClumpedCorpus();
    
    return new Iterable<String>() {
      
      @Override
      public Iterator<String> iterator() {
        
        return new Iterator<String>() {
          
          int i = 0;
          
          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
          
          @Override
          public String next() {
            return clumps2str(clumpedCorpus[i++]);
          }
          
          @Override
          public boolean hasNext() {
            return i < clumpedCorpus.length;
          }
        };
      }
    };
  }
  
  /** Returns string representation of clumped sentence */
  public String clumps2str(int[][][] clumps) {
    StringBuffer sb = new StringBuffer();
    
    int lasti = clumps.length-1, lastj, lastk;
    for (int i = 0; i < clumps.length; i++) {
      lastj = clumps[i].length - 1;
      for (int j = 0; j <= lastj; j++) {
        lastk = clumps[i][j].length - 1;
        
        if (lastk == 0)
          sb.append(alpha.getString(clumps[i][j][0]));
        
        else {
          sb.append("(");

          for (int k = 0; k <= lastk; k++) {
            sb.append(alpha.getString(clumps[i][j][k]));
            if (k != lastk)
              sb.append(" ");
          }
          
          sb.append(")");
        }
        
        if (j != lastj)
          sb.append(" ");
      }
      
      if (i != lasti)
        sb.append(" ");
    }
    
    return sb.toString();
  }

  /**
   * Create clumped version of the original training corpus
   */
  public int[][][][] getClumpedCorpus() {
    
    int[][][] _corpus = corpus.corpus;
    int[][] _segments;
    int[] _terms;
    
    double[][] pyr;
    
    int[][][][] clumpedCorpus = new int[_corpus.length][][][];

    int[][] clumpsTmp;
    
    int i, j, k, m, n, pyrI, pyrJ, numClump, index1, index2;
    
    double dontClumpVal;
    
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
          for (pyrJ = 0; pyrJ < pyrI; pyrJ++) {
            pyr[pyrI][pyrJ] = 
              factor[i] * bigramCounts.get(_terms[j], _terms[i+j+1]) 
              + sumParents(pyr, pyrI, pyrJ, m, n);
          }
        }
        
        toclump = new boolean[_terms.length-1];
        for (k = 0; k < _terms.length-1; k++) {
          dontClumpVal =
            bigramCounts.get(_terms[k], stopv) + 
            bigramCounts.get(stopv, _terms[k+1]);
          
          toclump[k] = pyr[0][k] < dontClumpVal; 
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
    
    return clumpedCorpus;
  }
  
  private static double sumParents(double[][] pyr, int i, int j, int m, int n) {
    assert 0 <= i;
    assert i < m;
    assert 0 <= j;
    assert j < n-i-1;
    
    if (i == m-1) return 0.;
    else if (j == 0) return pyr[i+1][0];
    else if (j == n-i-1) return pyr[i+1][j-1];
    else return pyr[i+1][j-1] + pyr[i+1][j];
  }
}
