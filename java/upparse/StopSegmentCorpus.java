package upparse;

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
        
        if (pos2-pos1 > 1) {
          seg = Arrays.copyOfRange(compiledCorpus[i], pos1, pos2);
          segments.add(seg);
        }
        
        pos1 = pos2+1;
      }
      
      corpus[i] = segments.toArray(new int[0][]);
    }
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
}
