package upparse.corpus;

import java.io.*;
import java.util.*;

/**
 * Static utilities for processing corpora
 * 
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class CorpusUtil {

  private CorpusUtil() {
  }

  public static StopSegmentCorpus wsjStopSegmentCorpus(final Alpha alpha,
      final String[] corpusFiles, final int numSent, final boolean noSeg) {
    final Iterable<LabeledBracketSet> treeiter = WSJCorpusTreeIter.fromFiles(
        corpusFiles, alpha);
    return treeIterStopSegmentCorpus(alpha, treeiter, KeepStop.wsjKeepStop,
        numSent, noSeg);
  }

  public static StopSegmentCorpus negraStopSegmentCorpus(final Alpha alpha,
      final String[] corpusFiles, final int numSent, final boolean noSeg) {
    final Iterable<LabeledBracketSet> treeiter = NegraCorpusTreeIter.fromFiles(
        corpusFiles, alpha);
    return treeIterStopSegmentCorpus(alpha, treeiter, KeepStop.negraKeepStop,
        numSent, noSeg);
  }

  public static StopSegmentCorpus ctbStopSegmentCorpus(final Alpha alpha,
      final String[] corpusFiles, final int numSent, final boolean noSeg) {
    final Iterable<LabeledBracketSet> treeiter = CTBCorpusTreeIter.fromFiles(
        corpusFiles, alpha);
    return treeIterStopSegmentCorpus(alpha, treeiter, KeepStop.ctbKeepStop,
        numSent, noSeg);
  }

  private static int[] list2array(final List<Integer> segment) {
    final int[] segArray = new int[segment.size()];
    for (int i = 0; i < segArray.length; i++)
      segArray[i] = segment.get(i);
    return segArray;
  }

  private static int[][] lists2array(List<int[]> segments) {
    final int[][] array = new int[segments.size()][];
    for (int i = 0; i < segments.size(); i++)
      array[i] = segments.get(i);
    return array;
  }

  public static StopSegmentCorpus splStopSegmentCorpus(final Alpha alpha,
      final String[] corpusStr, final int numSent, final boolean noSeg,
      final PrintStream statusStream) throws CorpusError {

    // count the number of sentences:
    int s = 0;
    for (final String file : corpusStr) {

      BufferedReader br;
      try {
        br = new BufferedReader(new FileReader(new File(file)));
        try {
          while (br.readLine() != null)
            s++;
        } catch (final IOException e) {
          throw new CorpusError(e.getMessage());
        } finally {
          br.close();
        }
      } catch (final IOException e1) {
        throw new CorpusError(e1.getMessage());
      }
    }
    statusStream
        .format("Creating StopSegmentCorpus: counted %d sentences\n", s);
    final int[][][] corpus = new int[s][][];

    s = 0;
    for (final String file : corpusStr) {
      BufferedReader br;
      try {
        br = new BufferedReader(new FileReader(new File(file)));
        String line;
        try {
          while ((line = br.readLine()) != null) {
            if (line.trim().equals("")) {
              corpus[s++] = new int[0][];
            } else {
              final List<int[]> segments = new ArrayList<int[]>();
              List<Integer> segment = new ArrayList<Integer>();
              for (final String word : line.split(" ")) {
                if (KeepStop.isStoppingPunc(word) && !noSeg
                    && !segment.isEmpty()) {
                  segments.add(list2array(segment));
                  segment = new ArrayList<Integer>();
                } else {
                  segment.add(alpha.getCode(word));
                }
              }
              if (!segment.isEmpty())
                segments.add(list2array(segment));

              corpus[s++] = lists2array(segments);
            }
          }
        } catch (final IOException e) {
          throw new CorpusError(e.getMessage());
        } finally {
          br.close();
        }
      } catch (final IOException e1) {
        throw new CorpusError(e1.getMessage());
      }

    }
    return StopSegmentCorpus.fromArrays(alpha, corpus);
  }

  public static StopSegmentCorpus wplStopSegmentCorpus(final Alpha alpha,
      final String[] corpusStr, final int numSent) {
    // TODO Auto-generated method stub
    return null;
  }

  public static StopSegmentCorpus treeIterStopSegmentCorpus(final Alpha alpha,
      final Iterable<LabeledBracketSet> treeiter, final CorpusConstraints cc,
      final int numS, final boolean noSeg) {
    final LabeledBracketSet[] lbs = lbsArrayFromIter(treeiter);
    final int len;
    if (numS == -1 || numS > lbs.length)
      len = lbs.length;
    else
      len = numS;
    final int[][][] corpus = new int[len][][];
    int i = 0;

    if (len > 0) {
      for (final LabeledBracketSet s : lbs) {
        final String str = s.tokenString(cc);
        final String[] segments = str.split(KeepStop.STOP);
        int m = 0;
        for (final String seg : segments)
          if (seg.trim().length() > 0)
            m++;
        corpus[i] = new int[m][];
        int j = 0;
        for (final String seg : segments) {
          if (seg.trim().length() > 0) {
            final String[] tokens = seg.trim().split(" +");
            corpus[i][j] = new int[tokens.length];
            for (int k = 0; k < tokens.length; k++)
              corpus[i][j][k] = alpha.getCode(tokens[k]);
            j++;
          }
        }

        if (noSeg) {
          final int l = sentLen(corpus[i]);
          final int[][] newsent = new int[1][l];
          int x = 0;
          for (final int[] seg : corpus[i])
            for (final int w : seg)
              newsent[0][x++] = w;
          corpus[i] = newsent;
        }

        i++;
        if (i >= len)
          break;
      }
    }

    return StopSegmentCorpus.fromArrays(alpha, corpus);
  }

  private static int sentLen(final int[][] sent) {
    int s = 0;
    for (final int[] sg : sent)
      s += sg.length;
    return s;
  }

  public static UnlabeledBracketSetCorpus wsjUnlabeledBracketSetCorpus(
      final Alpha alpha, final String[] corpusFiles) {
    return UnlabeledBracketSetCorpus.fromTreeIter(WSJCorpusTreeIter.fromFiles(
        corpusFiles, alpha).toUnlabeledIter(WSJCorpusStandard.instance));
  }

  public static UnlabeledBracketSetCorpus negraUnlabeledBrackSetCorpus(
      final Alpha alpha, final String[] corpusFiles) {
    return UnlabeledBracketSetCorpus.fromTreeIter(NegraCorpusTreeIter
        .fromFiles(corpusFiles, alpha).toUnlabeledIter(
            NegraCorpusStandard.instance));
  }

  public static UnlabeledBracketSetCorpus ctbUnlabeledBracketSetCorpus(
      final Alpha alpha, final String[] corpusFiles) {
    return UnlabeledBracketSetCorpus.fromTreeIter(CTBCorpusTreeIter.fromFiles(
        corpusFiles, alpha).toUnlabeledIter(CTBCorpusStandard.instance));
  }

  public static UnlabeledBracketSetCorpus cclpUnlabeledBracketSetCorpus(
      final Alpha alpha, final String[] files) {
    return UnlabeledBracketSetCorpus.fromTreeIter(CCLParserCorpusTreeIter
        .fromFiles(files, alpha));
  }

  public static ChunkedCorpus getChunkedCorpusClumps(final Alpha alpha,
      final Iterable<UnlabeledBracketSet> iter) {
    final UnlabeledBracketSet[] uBraks = ubsArrayFromIter(iter);
    final int[][][] arrays = new int[uBraks.length][][];
    int i = 0;
    for (final UnlabeledBracketSet u : uBraks)
      arrays[i++] = u.clumps();
    return ChunkedCorpus.fromArrays(arrays, alpha);
  }

  private static UnlabeledBracketSet[] ubsArrayFromIter(
      final Iterable<UnlabeledBracketSet> iter) {
    final List<UnlabeledBracketSet> l = new ArrayList<UnlabeledBracketSet>();
    for (final UnlabeledBracketSet s : iter)
      l.add(s);
    return l.toArray(new UnlabeledBracketSet[0]);
  }

  public static ChunkedCorpus wsjClumpGoldStandard(final Alpha alpha,
      final String[] corpusFiles) {
    return getChunkedCorpusClumps(
        alpha,
        WSJCorpusTreeIter.fromFiles(corpusFiles, alpha).toUnlabeledIter(
            WSJCorpusStandard.instance));
  }

  public static ChunkedCorpus negraClumpGoldStandard(final Alpha alpha,
      final String[] corpusFiles) {
    return getChunkedCorpusClumps(
        alpha,
        NegraCorpusTreeIter.fromFiles(corpusFiles, alpha).toUnlabeledIter(
            NegraCorpusStandard.instance));
  }

  public static ChunkedCorpus ctbClumpGoldStandard(final Alpha alpha,
      final String[] corpusFiles) {
    return getChunkedCorpusClumps(
        alpha,
        CTBCorpusTreeIter.fromFiles(corpusFiles, alpha).toUnlabeledIter(
            CTBCorpusStandard.instance));
  }

  private static ChunkedCorpus getChunkedCorpusNPs(final Alpha alpha,
      final Iterable<LabeledBracketSet> iter, final String cat,
      final CorpusConstraints cc) {
    final LabeledBracketSet[] lBraks = lbsArrayFromIter(iter);
    final int[][][] arrays = new int[lBraks.length][][];
    int i = 0;
    for (final LabeledBracketSet l : lBraks)
      arrays[i++] = l.lowestChunksOfType(cat, alpha, cc);
    return ChunkedCorpus.fromArrays(arrays, alpha);
  }

  private static LabeledBracketSet[] lbsArrayFromIter(
      final Iterable<LabeledBracketSet> unlabeledIter) {
    final List<LabeledBracketSet> l = new ArrayList<LabeledBracketSet>();
    for (final LabeledBracketSet u : unlabeledIter)
      l.add(u);
    return l.toArray(new LabeledBracketSet[0]);
  }

  public static ChunkedCorpus wsjNPsGoldStandard(final Alpha alpha,
      final String[] corpusFiles) {
    return getChunkedCorpusNPs(alpha,
        WSJCorpusTreeIter.fromFiles(corpusFiles, alpha), "NP",
        WSJCorpusStandard.instance);
  }

  public static ChunkedCorpus negraNPsGoldStandard(final Alpha alpha,
      final String[] corpusFiles) {
    return getChunkedCorpusNPs(alpha,
        NegraCorpusTreeIter.fromFiles(corpusFiles, alpha), "NP",
        NegraCorpusStandard.instance);
  }

  public static ChunkedCorpus ctbNPsGoldStandard(final Alpha alpha,
      final String[] corpusFiles) {
    return getChunkedCorpusNPs(alpha,
        CTBCorpusTreeIter.fromFiles(corpusFiles, alpha), "NP",
        CTBCorpusStandard.instance);
  }

  /**
   * @param corpusStr
   * @param fileType
   * @param numSent
   * @param filterByLength
   * @param noSeg
   * @return
   * @throws IOException
   */
  public static StopSegmentCorpus stopSegmentCorpus(final Alpha alpha,
      final String[] corpusStr, final CorpusType fileType, final int numSent,
      final int filterByLength, final boolean noSeg,
      final PrintStream statusStream) throws CorpusError {
    final StopSegmentCorpus corpus;
    switch (fileType) {
      case WSJ:
        corpus = CorpusUtil.wsjStopSegmentCorpus(alpha, corpusStr, numSent,
            noSeg);
        break;

      case NEGRA:
        corpus = CorpusUtil.negraStopSegmentCorpus(alpha, corpusStr, numSent,
            noSeg);
        break;

      case CTB:
        corpus = CorpusUtil.ctbStopSegmentCorpus(alpha, corpusStr, numSent,
            noSeg);
        break;

      case SPL:
        corpus = CorpusUtil.splStopSegmentCorpus(alpha, corpusStr, numSent,
            noSeg, statusStream);
        break;

      case WPL:
        corpus = CorpusUtil.wplStopSegmentCorpus(alpha, corpusStr, numSent);
        break;

      default:
        throw new CorpusError("Unexpected file-type: " + fileType);
    }

    return filterByLength > 0 ? corpus.filterLen(filterByLength) : corpus;
  }

  public static ChunkedCorpus npsGoldStandard(final CorpusType testFileType,
      final Alpha alpha, final String[] corpusFiles, final int filterLength)
      throws CorpusError {
    final ChunkedCorpus corpus;
    switch (testFileType) {
      case WSJ:
        corpus = CorpusUtil.wsjNPsGoldStandard(alpha, corpusFiles);
        break;

      case NEGRA:
        corpus = CorpusUtil.negraNPsGoldStandard(alpha, corpusFiles);
        break;

      case CTB:
        corpus = CorpusUtil.ctbNPsGoldStandard(alpha, corpusFiles);
        break;

      default:
        throw new CorpusError("Unexpected file type for NPs gold standard: "
            + testFileType);
    }

    return filterLength > 0 ? corpus.filterBySentenceLength(filterLength)
        : corpus;
  }

  public static UnlabeledBracketSetCorpus goldUnlabeledBracketSets(
      final CorpusType testFileType, final Alpha alpha,
      final String[] corpusFiles, final int filterLength) throws CorpusError {
    final UnlabeledBracketSetCorpus corpus;
    switch (testFileType) {
      case WSJ:
        corpus = CorpusUtil.wsjUnlabeledBracketSetCorpus(alpha, corpusFiles);
        break;

      case NEGRA:
        corpus = CorpusUtil.negraUnlabeledBrackSetCorpus(alpha, corpusFiles);
        break;

      case CTB:
        corpus = CorpusUtil.ctbUnlabeledBracketSetCorpus(alpha, corpusFiles);
        break;

      default:
        throw new CorpusError(
            "Unexpected file type for unlabeled bracket sets: " + testFileType);
    }

    return filterLength > 0 ? corpus.filterBySentenceLength(filterLength)
        : corpus;
  }
}
