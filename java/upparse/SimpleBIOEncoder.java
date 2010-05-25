package upparse;

import java.util.*;

/**
 * Utility for encoding clumped corpus and BIO tagged training set for HMM
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class SimpleBIOEncoder extends BIOEncoder {
  
  public static final int 
    STOP_STATE = 0, 
    B_STATE    = 1, 
    I_STATE    = 2, 
    O_STATE    = 3;
  
  public SimpleBIOEncoder(String stop, Alpha alpha) {
    super(stop, alpha);
  }

  @Override
  public int[] bioTrain(ClumpedCorpus corpus, int n) {
    int[][][][] clumpedCorpus = corpus.getArrays();
    int[] train = new int[n];
    int i = 0, j;
    train[i++] = STOP_STATE;
    
    for (int[][][] s: clumpedCorpus) {
      for (int[][] seg: s) {
        for (int[] clump: seg) {
          if (clump.length == 1)
            train[i++] = O_STATE;
          else {
            train[i++] = B_STATE;
            for (j = 1; j < clump.length; j++)
              train[i++] = I_STATE;
          }
          train[i++] = STOP_STATE;
        }
      }
    }
    
    return train;
  }

  @Override
  public ClumpedCorpus clumpedCorpusFromBIOOutput(int[] tokens, int[] output) {
    
    // Count the number of sentences
    int eosv = alpha.getCode(EOS);
    int numS = 0;
    for (int w: tokens)
      if (w == eosv)
        numS++;
    
    int[][][][] clumpedCorpus = new int[numS][][][];
    
    int i = 1, j, sInd = 0, nextEos = 1, numSeg, segInd, nextEoSeg, numClumps, 
    clumpInd;
    
    while (nextEos < tokens.length) {
      
      // process a sentence
      while (tokens[nextEos] != eosv)
        nextEos++;
      
      // count the number of segments
      numSeg = 0;
      j = i;
      while (j <= nextEos) {
        if (tokens[j] == stopv || tokens[j] == eosv) {
          numSeg++;
        }
        j++;
      }
      
      clumpedCorpus[sInd] = new int[numSeg][][];
      segInd = 0;
      
      nextEoSeg = i;
      while (segInd < numSeg) {
        
        while (output[nextEoSeg] != STOP_STATE) 
          nextEoSeg++;
        
        // count the number of clumps
        numClumps = 0;
        j = i;
        while (j < nextEoSeg) {
          if (output[j] == B_STATE || output[j] == O_STATE)
            numClumps++;
          j++;
        }     
        
        clumpedCorpus[sInd][segInd] = new int[numClumps][];
        clumpInd = 0;
        
        while (i < nextEoSeg) {
          if (output[i] == O_STATE) {
            clumpedCorpus[sInd][segInd][clumpInd] = new int[] { tokens[i] };
            i++;
            clumpInd++;
          }

          else if (output[i] == B_STATE) {
            j = i+1;
            while (output[j] == I_STATE) 
              j++;

            clumpedCorpus[sInd][segInd][clumpInd] = 
              Arrays.copyOfRange(tokens, i, j);

            // next clump
            i = j;
            clumpInd++;
          }
        }
        
        // next segment
        segInd++;
        nextEoSeg++;
        i = nextEoSeg;
      }
      
      // next sentence
      sInd++;
      
      // increment everything
      i++;
      nextEos = i;
    }
    
    return ClumpedCorpus.fromArrays(clumpedCorpus, alpha);
  }
}
