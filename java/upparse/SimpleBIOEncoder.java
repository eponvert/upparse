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
  public int[] bioTrain(ChunkedSegmentedCorpus corpus, int n) 
  throws EncoderError {
    int[][][][] clumpedCorpus = corpus.getArrays();
    int[] train = new int[n];
    int i = 0, j;
    train[i++] = STOP_STATE;
    
    for (int[][][] s: clumpedCorpus) {
      if (s.length != 0) {
        for (int[][] seg: s) {
          for (int[] clump: seg) {
            if (clump.length == 1)
              train[i++] = O_STATE;
            else {
              train[i++] = B_STATE;
              for (j = 1; j < clump.length; j++)
                train[i++] = I_STATE;
            }
          }
          train[i++] = STOP_STATE;
        }
      }
      else 
        train[i++] = STOP_STATE;
    }
    
    return train;
  }

  @Override
  public ChunkedSegmentedCorpus clumpedCorpusFromBIOOutput(int[] tokens, int[] output) 
  throws EncoderError {
    
    assert tokens.length == output.length;
    
    // Count the number of sentences
    int eosv = alpha.getCode(EOS);
    int numS = -1; // don't count first __eos__
    for (int w: tokens)
      if (w == eosv)
        numS++;
    
    int[][][][] clumpedCorpus = new int[numS][][][];
    
    int i = 1, j, sInd = 0, nextEos = 1, numSeg, segInd, nextEoSeg, numClumps, 
    clumpInd;
    
    assert output[i] != I_STATE;
    
    //while (nextEos < tokens.length) {
    while (sInd < numS) {
      
      // process a sentence
      while (nextEos < tokens.length && tokens[nextEos] != eosv) nextEos++;
      
      // count the number of segments
      numSeg = 0;
      j = i;
      while (j < tokens.length && j <= nextEos) {
        if (tokens[j] == stopv || tokens[j] == eosv) {
          numSeg++;
        }
        j++;
      }
      
      clumpedCorpus[sInd] = new int[numSeg][][];
      segInd = 0;
      
      nextEoSeg = i;
      while (segInd < numSeg) {
        
        while (output[nextEoSeg] != STOP_STATE) nextEoSeg++;
        
        assert nextEoSeg <= nextEos;
        
        assert output[nextEoSeg] == STOP_STATE;
        assert nextEoSeg >= output.length-1 || output[nextEoSeg+1] != I_STATE;
        
        // count the number of clumps
        numClumps = 0;
        j = i;
        while (j < nextEoSeg) {
          if (output[j] == B_STATE || output[j] == O_STATE) numClumps++;
          j++;
        }     
        
        clumpedCorpus[sInd][segInd] = new int[numClumps][];
        clumpInd = 0;
        
        boolean firstIn = true;
        while (i < nextEoSeg) {
          if (output[i] == O_STATE) {
            clumpedCorpus[sInd][segInd][clumpInd] = new int[] { tokens[i++] };
            clumpInd++;
            assert output[i] != I_STATE;
            firstIn = false;
          }

          else if (output[i] == B_STATE) {
            j = i+1;
            assert output[j] == I_STATE;
            while (output[j] == I_STATE) j++;
            assert output[j] != I_STATE;
            clumpedCorpus[sInd][segInd][clumpInd] = 
              Arrays.copyOfRange(tokens, i, j);

            // next clump
            i = j;
            clumpInd++;
            assert output[i] != I_STATE;    
            firstIn = false;
          }
          
          else {
            assert firstIn;
            assert output[i] != STOP_STATE: "output[i] should not be STOP";
            assert output[i] != I_STATE: "output[i] should not be I";
            throw new EncoderError(
                String.format("Unexpected tag: %d", output[i]));
          }
        }
        
        assert i == nextEoSeg: 
          String.format("i = %d nextEoSeg = %d", i, nextEoSeg);
        
        // next segment
        segInd++;
        nextEoSeg++;
        i = nextEoSeg;
      }
      
      // next sentence
      sInd++;
      
      // increment everything
      assert i == nextEos+1: String.format("(%d) i = %d nextEos = %d", sInd, i, nextEos);
      nextEos = i;
    }
    
    return ChunkedSegmentedCorpus.fromArrays(clumpedCorpus, alpha);
  }
}
