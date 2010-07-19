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
    Iterable<LabeledBracketSet> 
      treeiter = WSJCorpusTreeIter.fromFiles(corpusFiles);
    return 
      treeIterStopSegmentCorpus(alpha, treeiter, KeepStop.wsjKeepStop, 0);
  }
  
  public static StopSegmentCorpus negraStopSegmentCorpus(
      final Alpha alpha, final String[] corpusFiles) {
    Iterable<LabeledBracketSet>
      treeiter = NegraCorpusTreeIter.fromFiles(corpusFiles);
    return
      treeIterStopSegmentCorpus(alpha, treeiter, KeepStop.negraKeepStop, 0);
  }

  public static StopSegmentCorpus ctbStopSegmentCorpus(
      final Alpha alpha,
      final String[] corpusFiles) {
    Iterable<LabeledBracketSet>
      treeiter = CTBCorpusTreeIter.fromFiles(corpusFiles);
    return
      treeIterStopSegmentCorpus(alpha, treeiter, KeepStop.ctbKeepStop, 0);
  }

  public static StopSegmentCorpus splStopSegmentCorpus(Alpha alpha,
      String[] corpusStr) {
    // TODO Auto-generated method stub
    return null;
  }

  public static StopSegmentCorpus wplStopSegmentCorpus(Alpha alpha,
      String[] corpusStr) {
    // TODO Auto-generated method stub
    return null;
  }

  private static LabeledBracketSet[] lbsArrayFromIter(
      Iterable<LabeledBracketSet> iter) {
    List<LabeledBracketSet> ls = new ArrayList<LabeledBracketSet>();
    for (LabeledBracketSet l: iter) ls.add(l);
    return ls.toArray(new LabeledBracketSet[0]);
  }
  
  public static StopSegmentCorpus treeIterStopSegmentCorpus(
      final Alpha alpha,
      final Iterable<LabeledBracketSet> treeiter, 
      final CorpusConstraints cc,
      final int numS) {
    LabeledBracketSet[] lbs = lbsArrayFromIter(treeiter);
    int[][][] corpus = new int[lbs.length][][];
    int i = 0;
    
    for (LabeledBracketSet s: lbs) {
      final String str = s.tokenString(cc);
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


  public static UnlabeledBracketSetCorpus wsjUnlabeledBracketSetCorpus(
      Alpha alpha, String[] corpusFiles) {
    return UnlabeledBracketSetCorpus.fromTreeIter(
        WSJCorpusTreeIter.fromFiles(corpusFiles).toUnlabeledIter());
  }

  public static UnlabeledBracketSetCorpus negraUnlabeledBrackSetCorpus(
      Alpha alpha, String[] corpusFiles) {
    return UnlabeledBracketSetCorpus.fromTreeIter(
        NegraCorpusTreeIter.fromFiles(corpusFiles).toUnlabeledIter());
  }

  public static UnlabeledBracketSetCorpus ctbUnlabeledBracketSetCorpus(
      Alpha alpha, String[] corpusFiles) {
    return UnlabeledBracketSetCorpus.fromTreeIter(
        CTBCorpusTreeIter.fromFiles(corpusFiles).toUnlabeledIter());
  }

  private static ChunkedCorpus getChunkedCorpusClumps(Alpha alpha,
      Iterable<UnlabeledBracketSet> unlabeledIter) {
    // TODO Auto-generated method stub
    return null;
  }

  public static ChunkedCorpus wsjClumpGoldStandard(Alpha alpha,
      String[] corpusFiles) {
    return getChunkedCorpusClumps(alpha, 
        WSJCorpusTreeIter.fromFiles(corpusFiles).toUnlabeledIter());
  }

  public static ChunkedCorpus negraClumpGoldStandard(Alpha alpha,
      String[] corpusFiles) {
    return getChunkedCorpusClumps(alpha, 
        NegraCorpusTreeIter.fromFiles(corpusFiles).toUnlabeledIter());
  }

  public static ChunkedCorpus ctbClumpGoldStandard(Alpha alpha,
      String[] corpusFiles) {
    return getChunkedCorpusClumps(alpha, 
        CTBCorpusTreeIter.fromFiles(corpusFiles).toUnlabeledIter());
  }
  
  private static ChunkedCorpus getChunkedCorpusNPs(Alpha alpha,
      Iterable<LabeledBracketSet> unlabeledIter, String string) {
    // TODO Auto-generated method stub
    return null;
  }

  public static ChunkedCorpus wsjNPsGoldStandard(Alpha alpha,
      String[] corpusFiles) {
    return getChunkedCorpusNPs(alpha,
        WSJCorpusTreeIter.fromFiles(corpusFiles), "NP");
  }

  public static ChunkedCorpus negraNPsGoldStandard(Alpha alpha,
      String[] corpusFiles) {
    return getChunkedCorpusNPs(alpha, 
        NegraCorpusTreeIter.fromFiles(corpusFiles), "NP");
  }

  public static ChunkedCorpus ctbNPsGoldStandard(Alpha alpha,
      String[] corpusFiles) {
    return getChunkedCorpusNPs(alpha, 
        CTBCorpusTreeIter.fromFiles(corpusFiles), "NP");
  }
}
