package upparse.eval;

import static java.lang.Math.*;

import java.util.*;

import upparse.corpus.*;

/**
 * Class for evaluating chunker output
 * 
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class ChunkingEval extends Eval {

  private final ChunkedCorpus goldCorpus;
  private final boolean checkTerms;

  private ChunkingEval(final OutputType _type,
      final ChunkedCorpus chunkedCorpus, final boolean _checkTerms) {
    super("Chunker-" + _type.toString());
    goldCorpus = chunkedCorpus;
    checkTerms = _checkTerms;
  }

  @Override
  public void eval(String string, ChunkedSegmentedCorpus outputCorpus)
      throws EvalError {
    addExperiment(new ChunkingExperiment(string, outputCorpus.toChunkedCorpus()));
  }

  public UnlabeledExperimentEval newChunkingExperiment(final String s,
      final ChunkedCorpus c) throws EvalError {
    return new ChunkingExperiment(s, c);
  }

  private static class ChunkSet {
    final int index[][];
    final Chunk[] chunks;

    ChunkSet(final int n, final Chunk[] _chunks) {
      final int _n = n + 1;
      chunks = _chunks;
      index = new int[_n][_n];
      int i = 1;
      for (Chunk c : chunks)
        index[c.start][c.end] = i++;
    }

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(String.format("ChunkSet(%d,[", index.length));
      for (int i = 0; i < chunks.length; i++) {
        sb.append(chunks[i].toString());
        if (i != chunks.length - 1)
          sb.append(",");
      }
      sb.append("])");
      return sb.toString();
    }

    ChunkSet difference(final ChunkSet o) {
      int nToRemove = 0;
      List<Chunk> chunkList = new ArrayList<Chunk>(Arrays.asList(chunks));
      int[] toRemove = new int[chunks.length];
      for (Chunk c : o.chunks)
        if (contains(c))
          toRemove[nToRemove++] = indexOf(c);

      toRemove = Arrays.copyOf(toRemove, nToRemove);
      Arrays.sort(toRemove);

      for (int i = nToRemove - 1; i >= 0; i--)
        chunkList.remove(toRemove[i]);

      return new ChunkSet(index.length, chunkList.toArray(new Chunk[0]));
    }

    ChunkSet intersection(ChunkSet o) {
      List<Chunk> shared = new ArrayList<Chunk>();
      final ChunkSet checker, lister;

      if (o.chunks.length > chunks.length) {
        lister = this;
        checker = o;
      } else {
        lister = o;
        checker = this;
      }

      for (Chunk c : lister.chunks)
        if (checker.contains(c))
          shared.add(c);

      return new ChunkSet(index.length, shared.toArray(new Chunk[0]));
    }

    private int indexOf(Chunk c) {
      return index[c.start][c.end] - 1;
    }

    private boolean contains(Chunk c) {
      return index[c.start][c.end] != 0;
    }
  }

  private static class Chunk implements Comparable<Chunk> {
    // provide actual sentence index if we ever want to put these in a set
    // or hashtable
    final int start, end;

    Chunk(int _start, int _end) {
      start = _start;
      end = _end;
    }

    @Override
    public boolean equals(Object obj) {
      Chunk c = (Chunk) obj;
      return start == c.start && end == c.end;
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(new int[] { start, end });
    }

    @Override
    public int compareTo(Chunk o) {
      if (o == null)
        throw new NullPointerException();
      if (end < o.start)
        return -1;
      if (o.end < start)
        return 1;
      return 0;
    }

    @Override
    public String toString() {
      return String.format("Chunk(%d,%d)", start, end);
    }

    public boolean contains(Chunk c) {
      return start <= c.start && c.end <= end;
    }
  }

  private class ChunkingExperiment extends UnlabeledExperimentEval {

    private static final int TP = 0, FP = 1, FN = 2,

    NO_OVERLAP = 0, TP_SUB = 1, TP_SUP = 2, CROSSING = 3, NA = 4,

    MAXLEN = 5;

    private final int[][][] counts = new int[3][5][MAXLEN + 1];

    private final int[] len = new int[3];

    ChunkingExperiment(String name, ChunkedCorpus outputCorpus)
        throws EvalError {
      super(name);

      int[][][] goldIndices = goldCorpus.getArrays();
      int[][][] outpIndices = outputCorpus.getArrays();

      int[] terms, _terms;
      int[][] gold, outp;

      if (goldIndices.length != outpIndices.length) {
        for (int i = 0; i < Math.min(goldIndices.length, outpIndices.length); i++) {
          int[] g = termsFromSent(goldIndices[i]);
          int[] o = termsFromSent(outpIndices[i]);
          if (checkTerms)
            assert Arrays.equals(g, o);
          else
            assert g.length == o.length;
        }
        throw new EvalError(String.format(
            "Different corpus len: Gold = %d Output = %d", goldIndices.length,
            outpIndices.length));
      }

      for (int i = 0; i < goldIndices.length; i++) {
        gold = goldIndices[i];
        outp = outpIndices[i];

        terms = termsFromSent(gold);
        _terms = termsFromSent(outp);

        if (checkTerms)
          assert Arrays.equals(terms, _terms) : String.format(
              "Terms do not match:\nGold: %s\nOutp: %s\n[%d]", termStr(terms),
              termStr(termsFromSent(outp)), i);
        // else
        // terms.length == _terms.length:
        // String.format(
        // "Terms do not match:\nGold: %s\nOutp: %s\n[%d]",
        // termStr(terms),
        // termStr(termsFromSent(outp)),
        // i);

        final ChunkSet goldChunks = new ChunkSet(terms.length, chunks(gold)), outpChunks = new ChunkSet(
            terms.length, chunks(outp)), tp = goldChunks
            .intersection(outpChunks), fp = outpChunks.difference(tp), fn = goldChunks
            .difference(tp);

        int closestI, errorType;
        Chunk closest;

        for (Chunk c : tp.chunks) {
          counts[TP][NA][lenNorm(c)]++;
          len[TP] += c.end - c.start;
        }

        for (Chunk c : fp.chunks) {
          closestI = Arrays.binarySearch(tp.chunks, c);

          if (closestI < 0)
            errorType = NO_OVERLAP;

          else {
            closest = tp.chunks[closestI];

            if (closest.contains(c))
              errorType = TP_SUP;
            else if (c.contains(closest))
              errorType = TP_SUB;
            else
              errorType = CROSSING;

          }

          counts[FP][errorType][lenNorm(c)]++;
          len[FP] += c.end - c.start;
        }

        for (Chunk c : fn.chunks) {
          closestI = Arrays.binarySearch(fp.chunks, c);

          if (closestI < 0)
            errorType = NO_OVERLAP;

          else {
            closest = fp.chunks[closestI];

            if (closest.contains(c))
              errorType = TP_SUB;

            else if (c.contains(closest))
              errorType = TP_SUP;

            else
              errorType = CROSSING;
          }

          counts[FN][errorType][lenNorm(c)]++;
          len[FN] += c.end - c.start;
        }
      }
    }

    private String termStr(int[] terms) {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < terms.length; i++) {
        sb.append(goldCorpus.getString(terms[i]));
        if (i != terms.length - 1)
          sb.append(" ");
      }

      return sb.toString();
    }

    private int lenNorm(Chunk c) {
      return min(MAXLEN, c.end - c.start);
    }

    private int[] termsFromSent(int[][] goldIndices) {
      int n = 0;
      for (int[] c : goldIndices)
        n += c.length;

      int[] terms = new int[n];
      int i = 0;

      for (int[] c : goldIndices)
        for (int t : c)
          terms[i++] = t;

      return terms;
    }

    /** @return A sorted array of chunks len > 1 in the sentence */
    final Chunk[] chunks(int[][] gold) {
      int nChunks = 0;
      for (int[] chunk : gold)
        if (chunk.length > 1)
          nChunks++;

      Chunk[] ind = new Chunk[nChunks];

      int i = 0, start = 0, end;
      for (int[] chunk : gold) {
        if (chunk.length == 1)
          start++;
        else {
          end = start + chunk.length;
          ind[i++] = new Chunk(start, end);
          start = end;
        }
      }
      return ind;
    }

    @Override
    public int getTPlen() {
      return len[TP];
    }

    @Override
    public int getFNlen() {
      return len[FN];
    }

    @Override
    public int[][] getFNcounts() {
      return counts[FN];
    }

    @Override
    public int[][] getFPcounts() {
      return counts[FP];
    }

    @Override
    public int getFPlen() {
      return len[FP];
    }

    @Override
    public String getEvalName() {
      return ChunkingEval.this.getEvalName();
    }

    @Override
    public int[][] getTPcounts() {
      return counts[TP];
    }
  }

  public static ChunkingEval fromChunkedCorpus(final OutputType type,
      final ChunkedCorpus gold, final boolean checkTerms) {
    return new ChunkingEval(type, gold, checkTerms);
  }

  public static ChunkingEval fromChunkedCorpus(final OutputType type,
      final ChunkedCorpus clumpGoldStandard) {
    return fromChunkedCorpus(type, clumpGoldStandard, false);
  }
}
