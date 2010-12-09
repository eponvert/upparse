package upparse.corpus;

import java.io.*;
import java.util.*;

/**
 * Stucture representing a corpus -- which has clumps, but may not include 
 * sentence segments (i.e. seperated at punctuation)
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class ChunkedCorpus {
  
  private final int[][][] corpus;
  final Alpha alpha;

  private ChunkedCorpus(final int[][][] _corpus, final Alpha _alpha) {
    corpus = _corpus;
    alpha = _alpha;
  }
  
  public int[][][] getArrays() {
    return corpus;
  }
  
  public void writeTo(String filename) throws IOException {
    BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
    int i, j, k;
    for (i = 0; i < corpus.length; i++) {
      for (j = 0; j < corpus[i].length; j++) {
        if (corpus[i][j].length == 1) 
          bw.write(alpha.getString(corpus[i][j][0]));
        
        else {
          bw.write("(");
          for (k = 0; k < corpus[i][j].length; k++) {
            bw.write(alpha.getString(corpus[i][j][k]));
            if (k < corpus[i][j].length - 1) bw.write(" ");
          }
          bw.write(")");
        }
        
        if (j != corpus[i].length - 1) bw.write(" ");
      }
      bw.write("\n");
    }
    
    bw.close();
  }
        
  public static ChunkedCorpus fromChunkedSegmentedCorpus(
      ChunkedSegmentedCorpus c) {
    int[][][][] csArr = c.getArrays();
    int[][][] arr = new int[csArr.length][][];
    
    int numClumps, i, j;
    for (i = 0; i < arr.length; i++) {
      numClumps = 0;
      for (int[][] seg: csArr[i]) 
        numClumps += seg.length;
      
      arr[i] = new int[numClumps][];
      
      j = 0;
      for (int[][] seg: csArr[i])
        for (int[] clump: seg)
          arr[i][j++] = clump;
    }
    
    return new ChunkedCorpus(arr, c.alpha);
  }

  public static ChunkedCorpus fromFile(String filename, Alpha alpha) 
  throws IOException {
    int numS = 0;
    BufferedReader br = new BufferedReader(new FileReader(filename));
    while (br.readLine() != null) numS++;
    br.close();
    
    int[][][] corpus = new int[numS][][];
    
    br = new BufferedReader(new FileReader(filename));
    String s;
    String[] pieces;
    int i = 0, j, k, nClump, cStart, cLen;
    boolean inClump;
    int[][] clumpS;
    
    while ((s = br.readLine()) != null) {
      pieces = s.replace("(", "( ").replace(")", " )").split("  *");
      if (pieces.length == 1 && pieces[0].length() == 0) {
        pieces = new String[0];
      }
      inClump = false;
      nClump = 0;
      for (String piece: pieces) { 
        if (piece.equals("(")) {
          assert !inClump;
          inClump = true;
          nClump++;
        }
        
        else if (piece.equals(")")) {
          assert inClump;
          inClump = false;
        }
        
        else {
          if (!inClump) 
            nClump++;
        }
      }
      
      assert !inClump;
      
      clumpS = new int[nClump][]; 
      j = 0;
      k = 0;
      while (k < pieces.length) {
        if (pieces[k].equals("(")) {
          cStart = k + 1;
          cLen = 0;
          while (!pieces[cStart+cLen].equals(")")) cLen++;
          clumpS[j]= new int[cLen];
          
          for (k = cStart; k < cStart+cLen; k++)
            clumpS[j][k-cStart] = alpha.getCode(pieces[k]);
          
          assert pieces[k].equals(")"): 
            String.format("%d %s", k, pieces[k]);
          k++;
          j++;
        } else {
          clumpS[j++] = new int[] { alpha.getCode(pieces[k++]) };
        }
      }
      
      corpus[i++] = clumpS;
    }
    return new ChunkedCorpus(corpus, alpha);
  }

  public ChunkedCorpus filterBySentenceLength(int n) {
    List<Integer> sentences = new ArrayList<Integer>();
    for (int i = 0; i < corpus.length; i++)
      if (sentenceLen(corpus[i]) <= n)
        sentences.add(i);

    final int[][][] newCorpus = new int[sentences.size()][][];
    int i = 0;
    for (Integer j: sentences)
      newCorpus[i++] = corpus[j];
    
    return new ChunkedCorpus(newCorpus, alpha);
  }
  
  private static int sentenceLen(final int[][] sentence) {
    int len = 0;
    for (int[] chunk: sentence)
      len += chunk.length;
    return len;
  }

  public String getString(int i) {
    return alpha.getString(i);
  }

  public static ChunkedCorpus fromArrays(int[][][] arrays, Alpha alpha) {
    return new ChunkedCorpus(arrays, alpha);
  }
  public UnlabeledBracketSet[] toUnlabeledBracketSets() {
    UnlabeledBracketSet[] outputUB = 
      new UnlabeledBracketSet[corpus.length];
    
    for (int i = 0; i < corpus.length; i++) 
      outputUB[i] = new UnlabeledBracketSet(tokens(i), conv(i), alpha, false);
    
    return outputUB;
  }

  private Collection<UnlabeledBracket> conv(int i) {
    final int[][] chunks = corpus[i];
    List<UnlabeledBracket> b = new ArrayList<UnlabeledBracket>();
    int m = 0;
    for (int[] chunk: chunks) {
      int chunklen = chunk.length;
      m += chunklen;
      if (chunklen > 1)
        b.add(new UnlabeledBracket(m - chunklen, m));
    }
    return b;
  }

  private int[] tokens(int i) {
    final int[][] chunks = corpus[i];
    int n = 0;
    for (int[] a: chunks) n += a.length;
    int[] tokens = new int[n];
    int j = 0;
    for (int[] a: chunks) for (int c: a) tokens[j++] = c;
    return tokens;
  }


  public UnlabeledBracketSetCorpus toUnlabeledBracketSetCorpus() {
    return UnlabeledBracketSetCorpus.fromArrays(toUnlabeledBracketSets());
  }
}
