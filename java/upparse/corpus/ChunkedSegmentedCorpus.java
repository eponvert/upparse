package upparse.corpus;

import java.io.*;
import java.util.*;

import upparse.util.*;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class ChunkedSegmentedCorpus implements Corpus {

  /** Utilities to convert ChunkedSegmentedCorpus into a set of unlabeled 
   * bracket sets */
  public interface BracketConv {
    Collection<UnlabeledBracket> conv(int[][][] s);
  }
  
  private int[][][][] corpus;
  final Alpha alpha;
  
  private ChunkedSegmentedCorpus(final int[][][][] _corpus, final Alpha _alpha) {
    corpus = _corpus;
    alpha = _alpha;
  }

  public int[][][][] getArrays() {
    return corpus;
  }

  /** Utility to convert this corpus into sets of just the chunks */
  private static BracketConv CHUNKCONV = new BracketConv() {
    @Override
    public Collection<UnlabeledBracket> conv(int[][][] s) {
      List<UnlabeledBracket> b = new ArrayList<UnlabeledBracket>();
      int m = 0;
      for (int[][] seg: s)
        for (int[] chunk: seg) {
          if (chunk.length > 1)
            b.add(new UnlabeledBracket(m, m + chunk.length));
          m += chunk.length;
        }
      
      return b;
    }
  };

  public UnlabeledBracketSet[] asChunked() {
    return conv(CHUNKCONV, false);
  }

  /** Utility to convert this corpus into a right-branching baseline */ 
  private static BracketConv RBCONV = new BracketConv() {
    @Override
    public Collection<UnlabeledBracket> conv(int[][][] s) {
      List<UnlabeledBracket> b = new ArrayList<UnlabeledBracket>();
      int m = 0;
      for (int[][] seg: s) { 
        for (int i = 0; i < seg.length; i++) {
          if (i < seg.length - 1) {
            final int len = segLen(Arrays.copyOfRange(seg, i, seg.length)); 
            if (len > 1) 
              b.add(new UnlabeledBracket(m, m + len));
          }
          if (seg[i].length > 1)
            b.add(new UnlabeledBracket(m, m + seg[i].length));
          m += seg[i].length;
        }
      }
      
      final UnlabeledBracket fullSpan = new UnlabeledBracket(0, m);
      if (!b.contains(fullSpan)) b.add(fullSpan);
      
      return b;
    }
  };
  
  public UnlabeledBracketSet[] asRB() {
    return conv(RBCONV);
  }
  
  private static BracketConv FLATCONV = new BracketConv() {
    @Override
    public Collection<UnlabeledBracket> conv(int[][][] s) {
      List<UnlabeledBracket> b = new ArrayList<UnlabeledBracket>();
      int m = 0;
      for (int[][] seg: s) {
        final int seglen = segLen(seg);
        if (seglen > 1) {
          b.add(new UnlabeledBracket(m, m + seglen));
          if (seg.length > 1) {
            for (int[] chunk: seg) {
              if (chunk.length > 1)
                b.add(new UnlabeledBracket(m, m + chunk.length));
              m += chunk.length;
            }
          }
        }
      }
      return b;
    }
  };

  public UnlabeledBracketSet[] asFlat() {
    return conv(FLATCONV);
  }
  
  
  private UnlabeledBracketSet[] conv(BracketConv b, boolean countRoot) {
    UnlabeledBracketSet[] outputUB = 
      new UnlabeledBracketSet[nSentences()];
    
    for (int i = 0; i < nSentences(); i++) {
      final Collection<UnlabeledBracket> brackets = b.conv(corpus[i]);
      outputUB[i] = new UnlabeledBracketSet(tokens(i), brackets, alpha, countRoot);
    }
    
    return outputUB;
  }

  
  private UnlabeledBracketSet[] conv(BracketConv b) {
    return conv(b, true);
  }

  /**
   * Iterate over the sentences of the original training corpus, returning
   * strings representing clumped sentences
   */
  public Iterable<String> strIter() {
    return new Iterable<String>() {
      
      @Override
      public Iterator<String> iterator() {
        
        return new Iterator<String>() {
          
          int i = 0;
          
          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
          
          @Override
          public String next() {
            return clumps2str(corpus[i++]);
          }
          
          @Override
          public boolean hasNext() {
            return i < corpus.length;
          }
        };
      }
    };
  }
  
  /** Returns string representation of clumped sentence */
  public String clumps2str(int[][][] clumps) {
    StringBuffer sb = new StringBuffer();
    
    int lasti = clumps.length-1, lastj, lastk;
    for (int i = 0; i < clumps.length; i++) {
      lastj = clumps[i].length - 1;
      for (int j = 0; j <= lastj; j++) {
        lastk = clumps[i][j].length - 1;
        
        if (lastk == 0)
          sb.append(alpha.getString(clumps[i][j][0]));
        
        else {
          sb.append("(");

          for (int k = 0; k <= lastk; k++) {
            sb.append(alpha.getString(clumps[i][j][k]));
            if (k != lastk)
              sb.append(" ");
          }
          
          sb.append(")");
        }
        
        if (j != lastj)
          sb.append(" ");
      }
      
      if (i != lasti)
        sb.append(" ");
    }
    
    return sb.toString();
  }

  public void writeTo(BufferedWriter output, String[][] outputText) throws IOException {
    ChunkedCorpus.fromChunkedSegmentedCorpus(this).writeTo(output, outputText);
  }
  

  private void writeToUnderscore(BufferedWriter output, String[][] outputText) 
  throws IOException {
    ChunkedCorpus
      .fromChunkedSegmentedCorpus(this).writeToUnderscore(output, outputText);
  }
  
  private void writeToUnderscoreCCL(BufferedWriter bw, String[][] outputText) 
  throws IOException {
    GetString getString = 
      GetString.altGetString(outputText, toChunkedCorpus().getArrays(), alpha);

    for (int sent = 0; sent < corpus.length; sent++) {
      int chunkI = 0;
      for (int[][] seg: corpus[sent]) {
        for (int[] chunk: seg) {
          for (int wrd = 0; wrd < chunk.length; wrd++) {
            bw.write(getString.getString(sent, chunkI, wrd));
            if (wrd == chunk.length - 1)
              bw.write(' ');
            else
              bw.write('_');
          }
          chunkI++;
        }
        bw.write(" ; ");
      }
      bw.write("\n");
    }
    bw.close();
  }
  

  private void writeToWithPunc(BufferedWriter bw, String[][] textOutput) 
  throws IOException {
    GetString getString = GetString.altGetString(
        textOutput, toChunkedCorpus().getArrays(), alpha);
    
    for (int sent = 0; sent < corpus.length; sent++) {
      for (int seg = 0; seg < corpus[sent].length; seg++) {
        int chunkI = 0;
        for (int[] chunk: corpus[sent][seg]) {
          if (chunk.length > 1) 
            bw.write("(");
          for (int wrd = 0; wrd < chunk.length; wrd++) {
            bw.write(getString.getString(sent, chunkI, wrd));
            if (wrd < chunk.length - 1)
              bw.write(' ');
          }
          bw.write(chunk.length > 1 ? ") " : " ");
          chunkI++;
        }
        bw.write(" ; ");
      }
      bw.write('\n');
    }
    bw.close();
  }


  public static ChunkedSegmentedCorpus fromArrays(
      int[][][][] clumpedCorpus, Alpha alpha) {
    return new ChunkedSegmentedCorpus(clumpedCorpus, alpha);
  }

  public ChunkedCorpus toChunkedCorpus() {
    return ChunkedCorpus.fromChunkedSegmentedCorpus(this);
  }

  @Override
  public int nSentences() {
    return corpus.length;
  }

  public int[] tokens(int i) {
    int n = 0;
    for (int[][] seg: corpus[i]) n += segLen(seg);
    
    final int[] tokens = new int[n];
    int j = 0;
    for (int[][] seg: corpus[i])
      for (int[] chunk: seg)
        for (int w: chunk)
          tokens[j++] = w;
    
    return tokens;
  }

  public Collection<UnlabeledBracket> chunkBrackets(int i) {
    final List<UnlabeledBracket> b = new ArrayList<UnlabeledBracket>();
    int curr = 0;
    for (int[][] seg: corpus[i]) {
      b.add(new UnlabeledBracket(curr, curr + segLen(seg)));
      for (int[] chunk: seg) {
        final int next = curr + chunk.length;
        b.add(new UnlabeledBracket(curr, next));
        curr = next;
      }
    }
    return b;
  }

  private static int segLen(int[][] seg) {
    int n = 0;
    for (int[] chunk: seg) n += chunk.length;
    return n;
  }


  public void writeTo(
      BufferedWriter output, OutputType outputType, String[][] outputText) 
  throws IOException, CorpusError {
    switch (outputType) {
      case CLUMP:
      case NPS:
      case PPS:
        writeTo(output, outputText);
        break;
        
      case PUNC:
        writeToWithPunc(output, outputText);
        break;
       
      case UNDERSCORE:
        writeToUnderscore(output, outputText);
        break;
        
      case UNDERSCORE4CCL:
        writeToUnderscoreCCL(output, outputText);
        break;
        
      case TREEBANKRB:
        UnlabeledBracketSetCorpus.fromArrays(asRB()).writeTo(output, outputText);
        break;
        
      case TREEBANKPREC:
        UnlabeledBracketSetCorpus
          .fromArrays(asChunked()).writeTo(output, outputText);
        break;
        
      case TREEBANKFLAT:
        UnlabeledBracketSetCorpus.fromArrays(asFlat())
          .writeTo(output, outputText);
        break;
        
      default:
        throw new CorpusError("Unexpected output type: " + outputType);
    }
  }

  public void writeTo(
      String output, OutputType outputType, String[][] outputText) 
  throws IOException, CorpusError {
    BufferedWriter bw = Util.bufferedWriter(output);
    writeTo(bw, outputType, outputText);
  }

  public ChunkedSegmentedCorpus filter(int filterLen) {
    List<int[][][]> newCorpusConstr = new ArrayList<int[][][]>();
    for (int i = 0; i < corpus.length; i++)
      if (tokens(i).length <= filterLen)
        newCorpusConstr.add(corpus[i]);
    int[][][][] newCorpus = newCorpusConstr.toArray(new int[0][][][]);
    return new ChunkedSegmentedCorpus(newCorpus, alpha);
  }

  public void reverse() {
    corpus = Util.reverse(corpus);
  }
}
