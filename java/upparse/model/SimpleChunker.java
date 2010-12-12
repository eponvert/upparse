package upparse.model;

import java.util.*;

import upparse.corpus.*;

/**
 * Simple count oriented clumper
 * 
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class SimpleChunker implements Chunker {

  public static final String STOP = "__stop__";
  final Alpha alpha;
  private final NgramCounts bigramCounts;
  private final int stopv;
  private final double[] factor;

  public static SimpleChunker fromStopSegmentCorpus(final Alpha alpha,
      final StopSegmentCorpus corpus, final double[] factor) {
    final NgramCounts bigramCounts = new NgramCounts();
    final int stopv = alpha.getCode(STOP);
    for (int[][] s : corpus.arrayIter()) {
      for (int[] seg : s) {
        if (seg.length > 0) {
          final int last = seg.length - 1;
          bigramCounts.incr(stopv, seg[0]);
          bigramCounts.incr(seg[last], stopv);
          for (int j = 0; j < last; j++) {
            bigramCounts.incr(seg[j], seg[j + 1]);
          }
        }
      }
    }

    return new SimpleChunker(alpha, bigramCounts, stopv, factor);
  }

  private SimpleChunker(final Alpha _alpha, final NgramCounts _bigramCounts,
      final int _stopv, final double[] _factor) {
    alpha = _alpha;
    bigramCounts = _bigramCounts;
    stopv = _stopv;
    factor = _factor;
  }

  @Override
  public ChunkedSegmentedCorpus getChunkedCorpus(StopSegmentCorpus c) {
    int[][][] _corpus = c.getArrays();
    int[][] _segments;
    int[] _terms;

    double[][] pyr;

    int[][][][] clumpedCorpus = new int[_corpus.length][][][];

    int[][] clumpsTmp;

    int i, j, k, m, n, pyrI, pyrJ, numClump, index1, index2, t1, t2;

    double count, doClump, dontClumpL, dontClumpR, sumP;

    boolean[] toclump;

    for (i = 0; i < _corpus.length; i++) {
      _segments = _corpus[i];

      clumpedCorpus[i] = new int[_segments.length][][];

      for (j = 0; j < _segments.length; j++) {

        _terms = _segments[j];
        clumpsTmp = new int[_terms.length][];
        numClump = 0;

        n = _terms.length;
        m = Math.min(_terms.length - 1, factor.length);
        m = Math.max(m, 0);
        pyr = new double[m][];

        for (pyrI = 0; pyrI < m; pyrI++)
          pyr[pyrI] = new double[n - pyrI - 1];

        for (pyrI = m - 1; pyrI >= 0; pyrI--) {
          for (pyrJ = 0; pyrJ < n - pyrI - 1; pyrJ++) {
            count = bigramCounts.get(_terms[pyrJ], _terms[pyrI + pyrJ + 1]);
            sumP = sumParents(pyr, pyrI, pyrJ, m, n);
            pyr[pyrI][pyrJ] = factor[pyrI] * count + sumP;
          }
        }

        toclump = new boolean[Math.max(_terms.length - 1, 0)];
        for (k = 0; k < _terms.length - 1; k++) {
          doClump = pyr[0][k];
          t1 = _terms[k];
          t2 = _terms[k + 1];
          dontClumpR = bigramCounts.get(t1, stopv);
          dontClumpL = bigramCounts.get(stopv, t2);
          toclump[k] = doClump >= dontClumpL + dontClumpR;
        }

        index1 = index2 = 0;
        if (toclump.length > 0) {
          while (index2 <= toclump.length) {
            while (index2 < toclump.length && toclump[index2])
              index2++;

            index2++;
            clumpsTmp[numClump++] = Arrays.copyOfRange(_terms, index1, index2);
            index1 = index2;
          }
        }

        clumpedCorpus[i][j] = Arrays.copyOf(clumpsTmp, numClump);
      }
    }

    return ChunkedSegmentedCorpus.fromArrays(clumpedCorpus, alpha);
  }

  private static double sumParents(double[][] pyr, int i, int j, int m, int n) {
    assert 0 <= i;
    assert i < m;
    assert 0 <= j;
    assert j < n - i - 1;

    if (i == m - 1)
      return 0.;
    else if (j == 0)
      return pyr[i + 1][0];
    else if (j == n - i - 2)
      return pyr[i + 1][j - 1];
    else
      return pyr[i + 1][j - 1] + pyr[i + 1][j];
  }
}
