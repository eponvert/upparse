package upparse;

import java.util.*;

/**
 * Static utilities for processing corpora
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class CorpusUtil {
  
  private CorpusUtil() { }

  public static StopSegmentCorpus wsjStopSegmentCorpus(
      final Alpha alpha,
      final String[] corpusFiles) {
    return wsjStopSegmentCorpus(alpha, corpusFiles, 0);
  }
  
  public static StopSegmentCorpus wsjStopSegmentCorpus(
      final Alpha alpha,
      final String[] corpusFiles,
      final int numS) {
    WSJCorpusTreeIter treeiter = WSJCorpusTreeIter.fromFiles(corpusFiles);
    return 
    treeIterStopSegmentCorpus(alpha, treeiter, WSJKeepStop.instance, numS);
  }
  
  public static StopSegmentCorpus treeIterStopSegmentCorpus(
      final Alpha alpha,
      final WSJCorpusTreeIter treeiter, 
      final CorpusConstraints cc,
      final int numS) {
    int n = 0;
    for (
        Iterator<LabeledBracketSet> it = treeiter.iterator() ; 
        it.hasNext() ;  
        it.next())
      n++;
    int[][][] corpus = new int[n][][];
    int i = 0;
    
    for (LabeledBracketSet s: treeiter) {
      final String str = s.tokenString(WSJKeepStop.instance);
      final String[] segments = str.split(KeepStop.STOP); 
      int m = 0;
      for (String seg: segments) if (seg.trim().length() > 0) m++;
      corpus[i] = new int[m][];
      int j = 0;
      for (String seg: segments) {
        if (seg.trim().length() > 0) {
          String[] tokens = seg.trim().split(" +");
          corpus[i][j] = new int[tokens.length];
          for (int k = 0; k < tokens.length; k++)
            corpus[i][j][k] = alpha.getCode(tokens[k]);
          j++;
        }
      }
      i++;
    }
    
    return StopSegmentCorpus.fromArrays(corpus);
  }
  
  public static String[] filesFromGlob(final String glob) {
    return null;
  }
}
