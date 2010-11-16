package upparse.corpus;

import upparse.model.*;
import upparse.util.*;

/**
 * Utility for encoding a clumped corpus as a BIO-tagged training set 
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public abstract class TagEncoder {
  
  public static enum EncoderType { 
    BIO_GP_NOSTOP, BIO_GP, BIO;
    public static String encoderTypeHelp() {
      return "  BIO            Basic BIO encoding\n" +
             "  BIO_GP         BIO encoding with 2nd order tagset\n" +
             "  BIO_GP_NOSTOP  BIO encoding with 2nd order tagset (except on STOP)";
    } 
  }

  private static final String EOS = "__eos__";
  
  private final int stopv;
  private final int eosv;
  final Alpha alpha;
  
  public TagEncoder(String stop, Alpha alpha) {
    this.alpha = alpha;
    stopv = alpha.getCode(stop);
    eosv = alpha.getCode(EOS);
  }
  
  protected boolean isStop(int w) { return w == stopv; }
  protected boolean isEos(int w) { return w == eosv; }
  protected boolean isStopOrEos(int w) { return isStop(w) || isEos(w); }
  protected int getEos() { return eosv; }
  
  public static TagEncoder getBIOEncoder(
      final EncoderType gp, final String stop, final Alpha alpha) throws EncoderError {
    switch (gp) {
      case BIO_GP_NOSTOP:
        return new GrandparentWithStopBIOEncoder(stop, alpha);
        
      case BIO_GP:
        return new GrandparentBIOEncoder(stop, alpha);
        
      case BIO:
        return new SimpleBIOEncoder(stop, alpha);
        
      default:
        throw new EncoderError("Unexpected GP option " + gp);
    }
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
    tokens[i++] = eosv;
    
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
  
  /** Predicate for whether a tag is a stop tag */
  public abstract Ipredicate isStopPred();
  
  /** Create soft training for sequence models */
  public abstract double[][] softTrain(final int[] train);
  
  /** @return the number of tags used */
  public abstract int numTags();

  /**
   * @return constraints on tag-pairs
   */
  public abstract boolean[][] constraints();

  public abstract double[] getInitTagProb();
  
  public double[][][] softCounts(final StopSegmentCorpus corpus) {
    return softCounts(tokensFromStopSegmentCorpus(corpus));
  }
  
  public int[] tokensFromStopSegmentCorpus(final StopSegmentCorpus corpus) {
    int[][][] clumpedCorpus = corpus.corpus;
    
    // count tokens 
    int n = 1; // for start token
    for (int[][] s: clumpedCorpus) {
      if (s.length != 0)  {
        for (int[] seg: s) {
          n += seg.length + 1;
        }
      }
      else
        n++;
    }
    
    int[] tokens = new int[n];
    int i = 0, eosv = alpha.getCode(EOS);
    tokens[i++] = eosv;
    
    for (int[][] s: clumpedCorpus) {
      if (s.length != 0) {
        for (int[] seg: s)  {
          System.arraycopy(seg, 0, tokens, i, seg.length);
          i += seg.length;
          tokens[i++] = stopv;
        }
        tokens[i-1] = eosv;
      } else
        tokens[i++] = eosv;
    }
    
    return tokens;
  }

  public double[][][] softCounts(final int[] train) {
    final boolean[][] constraints = constraints();
    final double[][] tags = softTrain(train);
    final int nVocab = MaxVals.arrayMax(train) + 1;
    final int nTag = numTags();
    final double[][][] counts = new double[nTag][nVocab][nTag];
    
    for (int i = 0; i < train.length - 1; i++) {
      double numOK = 0, numTotal = 0;
      for (int t = 0; t < nTag; t++)
        for (int _t = 0; _t < nTag; _t++)
          if (tags[i][t] != 0 && tags[i+1][_t] != 0) {
            numTotal += 1.0;
            if (!constraints[t][_t])
              numOK += 1.0;
          }
      
      final double ratio = numTotal / numOK;
      
      for (int t = 0; t < nTag; t++) { 
        for (int _t = 0; _t < nTag; _t++) {
          if (!constraints[t][_t])
            counts[t][train[i]][_t] += ratio * tags[i][t] * tags[i+1][_t];
        }
      }
          
    }
    return counts;
  }
  
  public double[][][] hardCounts(final ChunkedSegmentedCorpus corpus) 
  throws EncoderError {
    final int[] 
      tokens = tokensFromClumpedCorpus(corpus),
      tags = bioTrain(corpus, tokens.length);
 
    final int 
      nTerm = MaxVals.arrayMax(tokens) + 1,
      nTag = numTags();
    
    final int[][][] counts = new int[nTag][nTerm][nTag];
    for (int t = 0; t < tokens.length - 1; t++)
      counts[tags[t]][tokens[t]][tags[t+1]]++;
    
    final double[][][] countsD = new double[nTag][nTerm][nTag];
    for (int t = 0; t < nTag; t++)
      for (int w = 0; w < nTerm; w++)
        for (int _t = 0; _t < nTag; _t++)
          countsD[t][w][_t] = (double) counts[t][w][_t];

    return countsD;
  }

  public abstract int[] allNonStopTags();
}
