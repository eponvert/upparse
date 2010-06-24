package upparse;

import java.io.*;
import java.util.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class ChunkedSegmentedCorpus {
  
  private final int[][][][] corpus;
  final Alpha alpha;
  
  private ChunkedSegmentedCorpus(final int[][][][] _corpus, final Alpha _alpha) {
    corpus = _corpus;
    alpha = _alpha;
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

  public void writeTo(String fname) throws IOException {
    ChunkedCorpus.fromChunkedSegmentedCorpus(this).writeTo(fname);
  }

  public static ChunkedSegmentedCorpus fromArrays(
      int[][][][] clumpedCorpus, Alpha alpha) {
    return new ChunkedSegmentedCorpus(clumpedCorpus, alpha);
  }

  public ChunkedCorpus toChunkedCorpus() {
    return ChunkedCorpus.fromChunkedSegmentedCorpus(this);
  }

  /**
   * @param fname
   * @param goldStandardTrain
   * @param stopv 
   * @return
   * @throws IOException 
   */
  public static ChunkedSegmentedCorpus fromFiles(
        final String fname,
        final String goldStandardTrain, 
        final String stopv,
        final int numS) throws IOException {
    Alpha alpha = new Alpha();
    int[][][] chunks = 
      ChunkedCorpus.fromFile(goldStandardTrain, alpha).getArrays();
    int[][][] segments = 
      StopSegmentCorpus.fromFile(fname, alpha, stopv, numS).corpus;
    
    assert segments.length == chunks.length;
    
    int[][][][] chunkedSegments = new int[segments.length][][][];
    for (int sent = 0; sent < chunkedSegments.length; sent++) {
      int[][] segRepr = indices(segments[sent]);
      int[][] chnkRepr = indices(chunks[sent]);
      
      int chunkI = 0;
      chunkedSegments[sent] = new int[segRepr.length][][];
      
      for (int segI = 0; segI < segRepr.length; segI++) {
        int preNumChunks = 0;
        if (last(chnkRepr[chunkI]) < segRepr[segI][0]) {
          int last = chnkRepr[chunkI].length-1;
          assert chnkRepr[chunkI][last] >= segRepr[segI][0];
          while (chnkRepr[chunkI][last-preNumChunks] >= segRepr[segI][0])
            preNumChunks++;
          chunkI++;
        }
        
        assert last(chnkRepr[chunkI]) > segRepr[segI][0];
        
        int numChunks = 0;
        while (last(chnkRepr[chunkI+numChunks]) <= last(segRepr[segI])) 
          numChunks++;
        
        int postNumChunks = 0;
        while (chnkRepr[chunkI+numChunks][postNumChunks] <= 
               last(segRepr[segI])) {
          postNumChunks++;
        }

        if (preNumChunks != 0) {
          int start = chnkRepr[chunkI-1].length-preNumChunks-1;
          for (int i = 0; i < preNumChunks; i++) 
            chunkedSegments[sent][segI][i] = 
              new int[] { chunks[sent][chunkI-1][start+i] };
        }
        
        chunkedSegments[sent][segI] = new int[numChunks][];
        for (int i = 0; i < numChunks; i++) 
          chunkedSegments[sent][segI][preNumChunks+i] = chunks[sent][chunkI+i];
        
        chunkI += numChunks;
        
        for (int j = 0; j < postNumChunks; j++) {
          chunkedSegments[sent][segI][preNumChunks+numChunks+j] = 
            new int[] { chunks[sent][chunkI][j] };
        }
      }
    }
    return fromArrays(chunkedSegments, alpha); 
  }
  
  private static int[] last(int[][] a) {
    return a[a.length-1];
  }
  
  private static int last(int[] a) {
    return a[a.length-1];
  }
  
  private static int[][] indices(final int[][] chunks) {
    int w = 0;
    int[][] r = new int[chunks.length][];
    for (int i = 0; i < r.length; i++) { 
      r[i] = new int[chunks[i].length];
      for (int j = 0; j < r[i].length; j++) r[i][j] = w++;
    }
    return r;
  }
}
