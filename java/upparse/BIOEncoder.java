package upparse;

import java.io.*;


/**
 * Utility for encoding a clumped corpus as a BIO-tagged training set 
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public abstract class BIOEncoder {
  
  static final String START = "__start__";
  static final String EOS = "__eos__";
  
  final int stopv;
  final Alpha alpha;
  
  public BIOEncoder(String stop, Alpha alpha) {
    this.alpha = alpha;
    stopv = alpha.getCode(stop);
  }
  
  public static BIOEncoder getBIOEncoder(boolean gp, String stop, Alpha alpha) {
    if (gp)
      return new GrandparentBIOEncoder(stop, alpha);
    else
      return new SimpleBIOEncoder(stop, alpha);
  }
  
  public final int[] tokensFromFile(final String fname) throws IOException {
    return tokensFromClumpedCorpus(
        new BasicCorpus(fname).toBaseChunkedSegmentedCorpus(alpha, stopv));
  }
  
  public final int[] tokensFromClumpedCorpus(
      final ChunkedSegmentedCorpus corpus) {
    
    int[][][][] clumpedCorpus = corpus.getArrays();
    
    // count tokens 
    int n = 1; // for start token
    for (int[][][] s: clumpedCorpus) {
      if (s.length != 0)  {
        for (int[][] seg: s) {
          for (int[] clump: seg) {
            n += clump.length;
          } 
          n++;
        }
      }
      else
        n++;
    }
        
    
    int[] tokens = new int[n];
    int i = 0, eosv = alpha.getCode(EOS);
    tokens[i++] = alpha.getCode(START);
    
    for (int[][][] s: clumpedCorpus) {
      if (s.length != 0) {
        for (int[][] seg: s)  {
          for (int[] clump: seg) {
            System.arraycopy(clump, 0, tokens, i, clump.length);
            i += clump.length;
          }
          tokens[i++] = stopv;
        }
        tokens[i-1] = eosv;
      } else
        tokens[i++] = eosv;
    }
    
    return tokens;
  }

 /** Creating BIO encoded training material for HMM
   * @param n number of tokens in clumpedCorpus
   */
  public abstract int[] bioTrain(ChunkedSegmentedCorpus corpus, int n) 
  throws EncoderError;

  /** Return a clumped corpus from BIO encoded HMM output */ 
  public abstract ChunkedSegmentedCorpus clumpedCorpusFromBIOOutput(
      int[] tokens, int[] output) throws EncoderError;
}
