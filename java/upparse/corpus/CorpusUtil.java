package upparse.corpus;

import java.util.*;

/**
 * Static utilities for processing corpora
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class CorpusUtil {
  
  private CorpusUtil() { }

  public static StopSegmentCorpus wsjStopSegmentCorpus(
      final Alpha alpha,
      final String[] corpusFiles, 
      final int numSent) {
    Iterable<LabeledBracketSet> 
      treeiter = WSJCorpusTreeIter.fromFiles(corpusFiles);
    return 
      treeIterStopSegmentCorpus(alpha, treeiter, KeepStop.wsjKeepStop, numSent);
  }
  
  public static StopSegmentCorpus negraStopSegmentCorpus(
      final Alpha alpha, final String[] corpusFiles, final int numSent) {
    Iterable<LabeledBracketSet>
      treeiter = NegraCorpusTreeIter.fromFiles(corpusFiles);
    return
      treeIterStopSegmentCorpus(
          alpha, treeiter, KeepStop.negraKeepStop, numSent);
  }

  public static StopSegmentCorpus ctbStopSegmentCorpus(
      final Alpha alpha,
      final String[] corpusFiles, 
      final int numSent) {
    Iterable<LabeledBracketSet>
      treeiter = CTBCorpusTreeIter.fromFiles(corpusFiles);
    return
      treeIterStopSegmentCorpus(alpha, treeiter, KeepStop.ctbKeepStop, 0);
  }

  public static StopSegmentCorpus splStopSegmentCorpus(Alpha alpha,
      String[] corpusStr, final int numSent) {
    // TODO Auto-generated method stub
    return null;
  }

  public static StopSegmentCorpus wplStopSegmentCorpus(Alpha alpha,
      String[] corpusStr, final int numSent) {
    // TODO Auto-generated method stub
    return null;
  }

  public static StopSegmentCorpus treeIterStopSegmentCorpus(
      final Alpha alpha,
      final Iterable<LabeledBracketSet> treeiter, 
      final CorpusConstraints cc,
      final int numS) {
    final LabeledBracketSet[] lbs = arrayFromIter(treeiter);
    final int len; 
    if (numS == -1 || numS > lbs.length) len = lbs.length;
    else len = numS;
    int[][][] corpus = new int[len][][];
    int i = 0;
    
    if (len > 0) {
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
        if (i >= len)
          break;
      }
    }
    
    return StopSegmentCorpus.fromArrays(corpus);
  }


  public static UnlabeledBracketSetCorpus wsjUnlabeledBracketSetCorpus(
      Alpha alpha, String[] corpusFiles) {
    return UnlabeledBracketSetCorpus.fromTreeIter(
        WSJCorpusTreeIter.fromFiles(corpusFiles).toUnlabeledIter(
            WSJCorpusStandard.instance));
  }

  public static UnlabeledBracketSetCorpus negraUnlabeledBrackSetCorpus(
      Alpha alpha, String[] corpusFiles) {
    return UnlabeledBracketSetCorpus.fromTreeIter(
        NegraCorpusTreeIter.fromFiles(corpusFiles).toUnlabeledIter(
            NegraCorpusStandard.instance));
  }

  public static UnlabeledBracketSetCorpus ctbUnlabeledBracketSetCorpus(
      Alpha alpha, String[] corpusFiles) {
    return UnlabeledBracketSetCorpus.fromTreeIter(
        CTBCorpusTreeIter.fromFiles(corpusFiles).toUnlabeledIter(
            CTBCorpusStandard.instance));
  }

  private static ChunkedCorpus getChunkedCorpusClumps(Alpha alpha,
      Iterable<UnlabeledBracketSet> iter) {
    UnlabeledBracketSet[] uBraks = arrayFromIter(iter);
    int[][][] arrays = new int[uBraks.length][][];
    int i = 0;
    for (UnlabeledBracketSet u: uBraks) arrays[i++] = u.clumps(alpha);
    return ChunkedCorpus.fromArrays(arrays, alpha);
  }

  private static UnlabeledBracketSet[] arrayFromIter(
      Iterable<UnlabeledBracketSet> iter) {
    List<UnlabeledBracketSet> l = new ArrayList<UnlabeledBracketSet>();
    for (UnlabeledBracketSet s: iter) l.add(s);
    return l.toArray(new UnlabeledBracketSet[0]);
  }

  public static ChunkedCorpus wsjClumpGoldStandard(Alpha alpha,
      String[] corpusFiles) {
    return getChunkedCorpusClumps(alpha, 
        WSJCorpusTreeIter.fromFiles(corpusFiles).toUnlabeledIter(
            WSJCorpusStandard.instance));
  }

  public static ChunkedCorpus negraClumpGoldStandard(Alpha alpha,
      String[] corpusFiles) {
    return getChunkedCorpusClumps(alpha, 
        NegraCorpusTreeIter.fromFiles(corpusFiles).toUnlabeledIter(
            NegraCorpusStandard.instance));
  }

  public static ChunkedCorpus ctbClumpGoldStandard(Alpha alpha,
      String[] corpusFiles) {
    return getChunkedCorpusClumps(alpha, 
        CTBCorpusTreeIter.fromFiles(corpusFiles).toUnlabeledIter(
            CTBCorpusStandard.instance));
  }
  
  private static ChunkedCorpus getChunkedCorpusNPs(Alpha alpha,
      final Iterable<LabeledBracketSet> iter, 
      final String cat, 
      final CorpusConstraints cc) {
    LabeledBracketSet[] lBraks = arrayFromIter(iter);
    int[][][] arrays = new int[lBraks.length][][];
    int i = 0;
    for (LabeledBracketSet l: lBraks)
      arrays[i++] = l.lowestChunksOfType(cat, alpha, cc);
    return ChunkedCorpus.fromArrays(arrays, alpha);
  }

  private static LabeledBracketSet[] arrayFromIter(
      Iterable<LabeledBracketSet> unlabeledIter) {
    List<LabeledBracketSet> l = new ArrayList<LabeledBracketSet>();
    for (LabeledBracketSet u: unlabeledIter) l.add(u);
    return l.toArray(new LabeledBracketSet[0]);
  }

  public static ChunkedCorpus wsjNPsGoldStandard(Alpha alpha,
      String[] corpusFiles) {
    return getChunkedCorpusNPs(alpha,
        WSJCorpusTreeIter.fromFiles(corpusFiles), 
        "NP", 
        WSJCorpusStandard.instance);
  }

  public static ChunkedCorpus negraNPsGoldStandard(Alpha alpha,
      String[] corpusFiles) {
    return getChunkedCorpusNPs(alpha, 
        NegraCorpusTreeIter.fromFiles(corpusFiles), 
        "NP",
        NegraCorpusStandard.instance);
  }

  public static ChunkedCorpus ctbNPsGoldStandard(Alpha alpha,
      String[] corpusFiles) {
    return getChunkedCorpusNPs(alpha, 
        CTBCorpusTreeIter.fromFiles(corpusFiles), 
        "NP",
        CTBCorpusStandard.instance);
  }
}
