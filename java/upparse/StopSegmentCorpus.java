package upparse;

import java.io.*;
import java.util.*;

/**
 * Simple data structure for corpus with sentences split by phrasal punctuation
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class StopSegmentCorpus {
  
  final int[][][] corpus;
  int stopv;

  /**
   * @param compiledCorpus
   */
  public StopSegmentCorpus(int[][] compiledCorpus, int stop) {
    corpus = new int[compiledCorpus.length][][];
    stopv = stop;
    int[] seg;
    int pos1, pos2;
    int[] stopIndices;
    for (int i = 0; i < corpus.length; i++) {
      stopIndices = getStopIndices(compiledCorpus[i]);
      List<int[]> segments = new ArrayList<int[]>();
      pos1 = 0;
      for (int j = 0; j < stopIndices.length; j++) {
        pos2 = stopIndices[j];
        
        if (pos2-pos1 > 0) {
          seg = Arrays.copyOfRange(compiledCorpus[i], pos1, pos2);
          segments.add(seg);
        }
        
        pos1 = pos2+1;
      }
      
      corpus[i] = segments.toArray(new int[0][]);
    }
  }
  
  public ChunkedSegmentedCorpus toBaseChunkedSegmentedCorpus(Alpha alpha) {
    int[][][][] arrays = new int[corpus.length][][][];
    for (int i = 0; i < corpus.length; i++) {
      arrays[i] = new int[corpus[i].length][][];
      for (int j = 0; j < corpus[i].length; j++) {
        arrays[i][j] = new int[corpus[i][j].length][];
        for (int k = 0; k < corpus[i][j].length; k++) {
          arrays[i][j][k] = new int[] { corpus[i][j][k] };
        }
      }
    }
    
    return ChunkedSegmentedCorpus.fromArrays(arrays, alpha); 
  }

  private int[] getStopIndices(int[] s) {
    int[] indicesTmp = new int[s.length];
    int nIndices = 0;
    for (int i = 0; i < s.length; i++) 
      if (s[i] == stopv)
        indicesTmp[nIndices++] = i;
      
    int[] indices = new int[nIndices];
    System.arraycopy(indicesTmp, 0, indices, 0, nIndices);
    return indices;
  }
  
  public static StopSegmentCorpus fromFile(
      final String fname, final Alpha alpha, final String stopv, final int numS) 
  throws IOException {
    return fromFile(fname, alpha, alpha.getCode(stopv), numS);
  }
  
  public static StopSegmentCorpus fromFile(
      final String fname, final Alpha alpha, final int stopv, final int numS) 
  throws IOException {
    return new StopSegmentCorpus(
        new BasicCorpus(fname, numS).compiledCorpus(alpha), stopv);
  }
}
