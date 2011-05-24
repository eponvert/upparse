package upparse.corpus;

import java.io.*;
import java.util.*;

import upparse.util.*;

/**
 * Simple data structure for corpus with sentences split by phrasal punctuation
 * 
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class StopSegmentCorpus {

  int[][][] corpus;
  private final Alpha alpha;

  private StopSegmentCorpus(final Alpha _alpha, final int[][][] _corpus) {
    alpha = _alpha;
    corpus = _corpus;
  }

  public static StopSegmentCorpus fromArrays(final Alpha alpha,
      final int[][][] corpus) {
    return new StopSegmentCorpus(alpha, corpus);
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

  /** Create a sub-corpus of setences whose length is lte to num */
  public StopSegmentCorpus filterLen(final int num) {
    boolean[] filt = new boolean[corpus.length];
    int i = 0;
    int n = 0;
    for (int[][] s : corpus) {
      int len = 0;
      for (int[] seg : s)
        len += seg.length;
      if (len <= num) {
        n++;
        filt[i] = true;
      }
      i++;
    }

    int[][][] _corpus = new int[n][][];
    i = 0;
    for (int j = 0; j < corpus.length; j++)
      if (filt[j])
        _corpus[i++] = corpus[j];

    return fromArrays(getAlpha(), _corpus);
  }

  public Iterable<int[][]> arrayIter() {
    return new Iterable<int[][]>() {

      @Override
      public Iterator<int[][]> iterator() {
        return new Iterator<int[][]>() {
          int i = 0;

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }

          @Override
          public int[][] next() {
            return corpus[i++];
          }

          @Override
          public boolean hasNext() {
            return i < corpus.length;
          }
        };
      }
    };
  }

  public int[][][] getArrays() {
    return corpus;
  }

  public void writeTo(String output) throws IOException {
    BufferedWriter bw = new BufferedWriter(new FileWriter(output));
    for (int[][] sent : corpus) {
      bw.write("__stop__");
      for (int[] seg : sent) {
        for (int w : seg) {
          bw.write(" ");
          bw.write(getAlpha().getString(w));
        }
        bw.write(" __stop__");
      }
      bw.write("\n");
    }
    bw.close();
  }

  public Alpha getAlpha() {
    return alpha;
  }

  public int size() {
    return corpus.length;
  }

  /** Reverse sequence of words in corpus */
  public void reverse() {
    corpus = Util.reverse(corpus);
  }

  public void writeTokenizedPlaintextTo(String output) throws IOException {
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), "UTF8"));
    for (int[][] sent : corpus) {
      final int n = sent.length;
      for (int i = 0; i < n; i++) {
        final int[] seg = sent[i];
        for (int w : seg) {
          bw.write(getAlpha().getString(w));
          bw.write(" ");
        }
        if (i != n - 1)
          bw.write(", ");
      }
      bw.write(".\n");
    }
    bw.close();
  }
}
