package upparse;

import java.io.*;
import java.util.*;

import static java.lang.Math.*;

/**
 * Class for evaluating chunker output
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class ChunkingEval {
  
  private final ChunkedCorpus goldCorpus;
  private final String evalName;
  private final List<Experiment> experiments = new ArrayList<Experiment>();
  private final boolean checkTerms;

  private ChunkingEval(
      final String name, 
      final ChunkedCorpus chunkedCorpus, 
      final boolean _checkTerms) {
    evalName = name;
    goldCorpus = chunkedCorpus;
    checkTerms = _checkTerms;
  }

  public static ChunkingEval fromCorpusFile(
      final String filename, 
      final Alpha alpha,
      final boolean checkTerms) 
  throws IOException {
    final String name = new File(filename).getName();
    return new ChunkingEval(
        name, ChunkedCorpus.fromFile(filename, alpha), checkTerms);
  }

  public void eval(String string, ChunkedCorpus outputCorpus) throws EvalError {
    experiments.add(new Experiment(string, outputCorpus));
  }
  
  public void writeSummary(final String evalType) throws EvalError {
    writeSummary(evalType, System.out);
  }

  public void writeSummary(String evalType, PrintStream out) throws EvalError {
    for (Experiment experiment: experiments) {
      if (evalType.equals("PR"))
        experiment.writeSummary(out);
      
      else if (evalType.equals("PRLcsv"))
        experiment.writeSummaryWithLenCSV(out);
      
      else if (evalType.equals("PRL")) 
        experiment.writeSummaryWithLen(out);
      
      else if (evalType.equals("PRC"))
        experiment.writeSummaryWithCounts(out);
      
      else if (evalType.equals("PRCL"))
        experiment.writeSummaryWithCountsAndLen(out);
      
      else 
        throw new EvalError("Unknown eval type:: " + evalType);
    }
  }
  
  private static class ChunkSet {
    final int index[][];
    final Chunk[] chunks;
    
    ChunkSet(final int n, final Chunk[] _chunks) {
      final int _n = n + 1; 
      chunks = _chunks;
      index = new int[_n][_n];
      int i = 1;
      for (Chunk c: chunks) 
        index[c.start][c.end] = i++;
    }
    
    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(String.format("ChunkSet(%d,[", index.length));
      for (int i = 0; i < chunks.length; i++) {
        sb.append(chunks[i].toString());
        if (i != chunks.length-1)
          sb.append(",");
      }
      sb.append("])");
      return sb.toString();
    }



    ChunkSet difference(final ChunkSet o) {
      int nToRemove = 0;
      List<Chunk> chunkList = new ArrayList<Chunk>(Arrays.asList(chunks));
      int[] toRemove = new int[chunks.length]; 
      for (Chunk c: o.chunks)
        if (contains(c))  
          toRemove[nToRemove++] = indexOf(c);
      
      toRemove = Arrays.copyOf(toRemove, nToRemove);
      Arrays.sort(toRemove);
      
      for (int i = nToRemove-1; i >= 0; i--)
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
      
      for (Chunk c: lister.chunks)
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
      start = _start; end = _end;
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
      if (o == null) throw new NullPointerException();
      if (end < o.start) return -1;
      if (o.end < start) return 1;
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
  
  private class Experiment {
    
    private static final int 
      TP = 0, 
      FP = 1, 
      FN = 2, 
      
      NO_OVERLAP = 0,
      TP_SUB = 1,
      TP_SUP = 2,
      CROSSING = 3,
      NA = 4,
      
      MAXLEN = 5;
    
    private final int[][][] counts = new int[3][5][MAXLEN+1];
    private final String expName;
    
    private final int[] len = new int[3];

    Experiment(String name, ChunkedCorpus outputCorpus) throws EvalError {
      expName = name;
      
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
        throw new EvalError(
            String.format("Different corpus len: Gold = %d Output = %d",
                goldIndices.length, outpIndices.length));
      }
      
      for (int i = 0; i < goldIndices.length; i++) {
        gold = goldIndices[i];
        outp = outpIndices[i];
        
        terms = termsFromSent(gold);
        _terms = termsFromSent(outp);
        
        if (checkTerms)
          assert Arrays.equals(terms, _terms):
            String.format(
                "Terms do not match:\nGold: %s\nOutp: %s\n[%d]",
                termStr(terms),
                termStr(termsFromSent(outp)),
                i);
        else 
          assert terms.length == _terms.length;
              
        
        final ChunkSet 
          goldChunks = new ChunkSet(terms.length, chunks(gold)),
          outpChunks = new ChunkSet(terms.length, chunks(outp)),
          truePos = goldChunks.intersection(outpChunks),
          falsePos = outpChunks.difference(truePos),
          falseNeg = goldChunks.difference(truePos);

        
        int closestI, errorType;
        Chunk closest;
        
        for (Chunk c: truePos.chunks) {
          counts[TP][NA][lenNorm(c)]++;
          len[TP] += c.end - c.start;
        }
        
        for (Chunk c: falsePos.chunks) {
          closestI = Arrays.binarySearch(truePos.chunks, c);
          
          if (closestI < 0) 
            errorType = NO_OVERLAP;
            
          else {
            closest = truePos.chunks[closestI];

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
        
        for (Chunk c: falseNeg.chunks) {
          closestI = Arrays.binarySearch(falsePos.chunks, c);

          if (closestI < 0) 
            errorType = NO_OVERLAP;
            
          else {
            closest = falsePos.chunks[closestI];
            
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
        sb.append(goldCorpus.alpha.getString(terms[i]));
        if (i != terms.length-1)
          sb.append(" ");
      }
        
      return sb.toString();
    }

    private int lenNorm(Chunk c) {
      return min(MAXLEN, c.end - c.start);
    }

    private int[] termsFromSent(int[][] gold) {
      int n = 0;
      for (int[] c: gold) 
        n += c.length;
      
      int[] terms = new int[n];
      int i = 0;
      
      for (int[] c: gold) 
        for (int t: c)
          terms[i++] = t;
      
      return terms;
    }

    /** @return A sorted array of chunks len > 1 in the sentence */
    final Chunk[] chunks(int[][] sent) {
      int nChunks = 0;
      for (int[] chunk: sent)
        if (chunk.length > 1) nChunks++;
      
      Chunk[] ind = new Chunk[nChunks];
      
      int i = 0, start = 0, end;
      for (int[] chunk: sent) {
        if (chunk.length == 1)
          start++;
        else {
          end = start + chunk.length;
          ind[i++] = new Chunk(start,end);
          start = end;
        }
      }
      return ind;
    }
    
    public void writeSummaryWithLenCSV(PrintStream out) {
      int 
      tp = sum(counts[TP]), 
      fp = sum(counts[FP]), 
      fn = sum(counts[FN]),
      goldCount = tp + fn,
      predCount = tp + fp,
      goldLen = len[TP] + len[FN],
      predLen = len[TP] + len[FP];
    
    double
      goldCountF = (double) goldCount,
      predCountF = (double) predCount,
      goldLenF = (double) goldLen,
      predLenF = (double) predLen,
      goldLenAvg = goldLenF / goldCountF,
      predLenAvg = predLenF / predCountF,
      tpF = (double) tp, 
      fpF = (double) fp, 
      fnF = (double) fn,
      prec = 100 * tpF / (tpF + fpF),
      rec = 100 * tpF / (tpF + fnF),
      f = 2 * prec * rec / (prec + rec);
      
    out.println(String.format(
      "%.1f,%.1f,%.1f,%.2f,%.2f", prec, rec, f, goldLenAvg, predLenAvg));
    }

    public void writeSummary(PrintStream out) {
      int 
        tp = sum(counts[TP]), 
        fp = sum(counts[FP]), 
        fn = sum(counts[FN]);
      
      double
        tpF = (double) tp, 
        fpF = (double) fp, 
        fnF = (double) fn,
        prec = 100 * tpF / (tpF + fpF),
        rec = 100 * tpF / (tpF + fnF),
        f = 2 * prec * rec / (prec + rec);
      
      out.println(String.format("%25s %10s : %.1f / %.1f / %.1f ",
          evalName, expName, prec, rec, f));
    }

    public void writeSummaryWithLen(PrintStream out) {
      int 
        tp = sum(counts[TP]), 
        fp = sum(counts[FP]), 
        fn = sum(counts[FN]),
        goldCount = tp + fn,
        predCount = tp + fp,
        goldLen = len[TP] + len[FN],
        predLen = len[TP] + len[FP];
      
      double
        goldCountF = (double) goldCount,
        predCountF = (double) predCount,
        goldLenF = (double) goldLen,
        predLenF = (double) predLen,
        goldLenAvg = goldLenF / goldCountF,
        predLenAvg = predLenF / predCountF,
        tpF = (double) tp, 
        fpF = (double) fp, 
        fnF = (double) fn,
        prec = 100 * tpF / (tpF + fpF),
        rec = 100 * tpF / (tpF + fnF),
        f = 2 * prec * rec / (prec + rec);
        
      out.println(String.format(
        "%25s %10s : %.1f / %.1f / %.1f [G = %.2f, P = %.2f]", 
        evalName, expName, prec, rec, f, goldLenAvg, predLenAvg));
    }


    public void writeSummaryWithCounts(PrintStream out) {
      int 
      tp = sum(counts[TP]), 
      fp = sum(counts[FP]), 
      fn = sum(counts[FN]);
    
    double
      tpF = (double) tp, 
      fpF = (double) fp, 
      fnF = (double) fn,
      prec = 100 * tpF / (tpF + fpF),
      rec = 100 * tpF / (tpF + fnF),
      f = 2 * prec * rec / (prec + rec);
    
    out.println(String.format(
        "%25s %10s : %.1f / %.1f / %.1f ( %6d / %6d / %6d )", 
        evalName, expName, prec, rec, f, tp, fp, fn));
    }

    public void writeSummaryWithCountsAndLen(PrintStream out) {
      int 
      tp = sum(counts[TP]), 
      fp = sum(counts[FP]), 
      fn = sum(counts[FN]),
      goldCount = tp + fn,
      predCount = tp + fp,
      goldLen = len[TP] + len[FN],
      predLen = len[TP] + len[FP];
    
    double
      goldCountF = (double) goldCount,
      predCountF = (double) predCount,
      goldLenF = (double) goldLen,
      predLenF = (double) predLen,
      goldLenAvg = goldLenF / goldCountF,
      predLenAvg = predLenF / predCountF,
      tpF = (double) tp, 
      fpF = (double) fp, 
      fnF = (double) fn,
      prec = 100 * tpF / (tpF + fpF),
      rec = 100 * tpF / (tpF + fnF),
      f = 2 * prec * rec / (prec + rec);
      
    out.println(String.format(
      "%25s %10s : %.1f / %.1f / %.1f ( %6d / %6d / %6d ) [G = %.2f, P = %.2f]", 
      evalName, expName, prec, rec, f, tp, fp, fn, goldLenAvg, predLenAvg));
    }
    private int sum(int[][] is) {
      int s = 0;
      for (int[] a: is) s += sum(a);
      return s;
    }

    private int sum(int[] a) {
      int s = 0;
      for (int n: a) s += n;
      return s;
    }
  }
}
