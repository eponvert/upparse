package upparse;

import java.io.*;
import java.util.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class ClumpedCorpus {
  
  private final int[][][][] corpus;
  final Alpha alpha;
  
  private ClumpedCorpus(final int[][][][] _corpus, final Alpha _alpha) {
    corpus = _corpus;
    alpha = _alpha;
  }

  public static ClumpedCorpus fromArrays(int[][][][] clumpedCorpus, Alpha alpha) {
    return new ClumpedCorpus(clumpedCorpus, alpha);
  }

  public int[][][][] getArrays() {
    return corpus;
  }

  /**
   * Iterate over the sentences of the original training corpus, returning
   * strings representing clumped sentences
   */
  public Iterable<String> strIter() {
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
            return clumps2str(corpus[i++]);
          }
          
          @Override
          public boolean hasNext() {
            return i < corpus.length;
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

  public void printTo(PrintWriter output) {
    for (String s:strIter())
      output.println(s);
  }
}
