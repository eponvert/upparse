package upparse;

import java.io.*;

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
            if (k != corpus[i][j].length - 1) bw.write(" ");
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
}
