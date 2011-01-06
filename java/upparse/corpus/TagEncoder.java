package upparse.corpus;

import java.util.*;

import upparse.model.*;
import upparse.util.*;

/**
 * Utility for encoding a clumped corpus as a BIO-tagged training set
 * 
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public abstract class TagEncoder {

  public static enum EncoderType {
    BIO_GP_NOSTOP, BIO_GP, BIO, BILO;
    public static String encoderTypeHelp() {
      return "  BIO            Basic BIO encoding\n"
          + "  BILO           Basic BILO encoding\n"
          + "  BIO_GP         BIO encoding with 2nd order tagset\n"
          + "  BIO_GP_NOSTOP  BIO encoding with 2nd order tagset (except on STOP)";
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

  protected boolean isStop(int w) {
    return w == stopv;
  }

  protected boolean isEos(int w) {
    return w == eosv;
  }

  protected boolean isStopOrEos(int w) {
    return isStop(w) || isEos(w);
  }

  protected int getEos() {
    return eosv;
  }

  public static TagEncoder getBIOEncoder(final EncoderType gp,
      final String stop, final Alpha alpha) throws EncoderError {
    switch (gp) {
      case BIO_GP_NOSTOP:
        return new GrandparentWithStopBIOEncoder(stop, alpha);

      case BIO_GP:
        return new GrandparentBIOEncoder(stop, alpha);

      case BIO:
        return new SimpleBIOEncoder(stop, alpha);
        
      case BILO:
        return new SimpleBILOEncoder(stop, alpha);

      default:
        throw new EncoderError("Unexpected GP option " + gp);
    }
  }

  public final int[] tokensFromClumpedCorpus(final ChunkedSegmentedCorpus corpus) {

    int[][][][] clumpedCorpus = corpus.getArrays();

    // count tokens
    int n = 1; // for start token
    for (int[][][] s : clumpedCorpus) {
      if (s.length != 0) {
        for (int[][] seg : s) {
          for (int[] clump : seg) {
            n += clump.length;
          }
          n++;
        }
      } else
        n++;
    }

    int[] tokens = new int[n];
    int i = 0, eosv = alpha.getCode(EOS);
    tokens[i++] = eosv;

    for (int[][][] s : clumpedCorpus) {
      if (s.length != 0) {
        for (int[][] seg : s) {
          for (int[] clump : seg) {
            System.arraycopy(clump, 0, tokens, i, clump.length);
            i += clump.length;
          }
          tokens[i++] = stopv;
        }
        tokens[i - 1] = eosv;
      } else
        tokens[i++] = eosv;
    }

    return tokens;
  }

  /**
   * Creating BIO encoded training material for HMM
   * 
   * @param n
   *          number of tokens in clumpedCorpus
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
    for (int[][] s : clumpedCorpus) {
      if (s.length != 0) {
        for (int[] seg : s) {
          n += seg.length + 1;
        }
      } else
        n++;
    }

    int[] tokens = new int[n];
    int i = 0, eosv = alpha.getCode(EOS);
    tokens[i++] = eosv;

    for (int[][] s : clumpedCorpus) {
      if (s.length != 0) {
        for (int[] seg : s) {
          System.arraycopy(seg, 0, tokens, i, seg.length);
          i += seg.length;
          tokens[i++] = stopv;
        }
        tokens[i - 1] = eosv;
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
          if (tags[i][t] != 0 && tags[i + 1][_t] != 0) {
            numTotal += 1.0;
            if (!constraints[t][_t])
              numOK += 1.0;
          }

      final double ratio = numTotal / numOK;

      for (int t = 0; t < nTag; t++) {
        for (int _t = 0; _t < nTag; _t++) {
          if (!constraints[t][_t])
            counts[t][train[i]][_t] += ratio * tags[i][t] * tags[i + 1][_t];
        }
      }

    }
    return counts;
  }

  public double[][][] hardCounts(final ChunkedSegmentedCorpus corpus)
      throws EncoderError {
    final int[] tokens = tokensFromClumpedCorpus(corpus), tags = bioTrain(
        corpus, tokens.length);

    final int nTerm = MaxVals.arrayMax(tokens) + 1, nTag = numTags();

    final int[][][] counts = new int[nTag][nTerm][nTag];
    for (int t = 0; t < tokens.length - 1; t++)
      counts[tags[t]][tokens[t]][tags[t + 1]]++;

    final double[][][] countsD = new double[nTag][nTerm][nTag];
    for (int t = 0; t < nTag; t++)
      for (int w = 0; w < nTerm; w++)
        for (int _t = 0; _t < nTag; _t++)
          countsD[t][w][_t] = (double) counts[t][w][_t];

    return countsD;
  }

  public abstract int[] allNonStopTags();

  public abstract int[] allStopTags();

  /**
   * Create uniform emissions probabilities subject to the constraints imposed
   * by this encoding scheme
   * 
   * @param smoothParam
   * @return A sort-of uniform set of emissions probabilities
   */
  public EmissionProbs altUniformEmiss(final int numVocab,
      final double smoothParam) {
    final double[][] prob = altUniformEmissP(numVocab);
    return EmissionProbs.directFromProbs(prob, isStopPred(), smoothParam);
  }
  
  private double[][] altUniformEmissP(final int numVocab) {
    double[][] prob = new double[numTags()][numVocab];
    final int[] stopTags = allStopTags(), nonStopTags = allNonStopTags();
    final double stopP = Math.log(1 / 2.);
    final double nonStopP = Math.log(1 / ((double) numVocab - 2));

    for (int v = 0; v < numVocab; v++) {
      if (isStopOrEos(v)) {
        for (int t : stopTags)
          prob[t][v] = stopP;
        for (int t : nonStopTags)
          prob[t][v] = Double.NEGATIVE_INFINITY;
      } else {
        for (int t : nonStopTags)
          prob[t][v] = nonStopP;
        for (int t : stopTags)
          prob[t][v] = Double.NEGATIVE_INFINITY;
      }
    }
    return prob;
  }
  
  private double[][] randomEmiss(int size) {
    final double[][] prob = altUniformEmissP(size);
    int ntag = prob.length;
    
    // create array of random probs
    final double[][] randProb = new double[ntag][size];
    Random r = new Random();
    
    // set all non-zero probs to randoms
    for (int t = 0; t < ntag; t++) { 
      for (int v = 0; v < size; v++) {
        if (prob[t][v] != Double.NEGATIVE_INFINITY)
          randProb[t][v] = r.nextDouble();
        else
          randProb[t][v] = 0;
      }

      // normalize and log
      double sum = Util.sum(randProb[t]);
      for (int v = 0; v < size; v++)
        randProb[t][v] = Math.log(randProb[t][v] / sum);
    }
    return randProb;
  }


  public EmissionProbs randomEmiss(int size, double smoothParam) {
    final double[][] randProb = randomEmiss(size);
    return EmissionProbs.directFromProbs(randProb, isStopPred(), smoothParam);
  }
  
  private double[][][] combineForJoint(final double[][] emiss, final double[][] trans) {
    final int ntag = numTags();
    final int numVocab = emiss[0].length;
    final double[][][] pr = new double[ntag][ntag][numVocab];
    for (int q = 0; q < ntag; q++)
      for (int r = 0; r < ntag; r++)
        for (int v = 0; v < numVocab; v++)
          pr[q][r][v] = emiss[q][v] + trans[q][r];
    return pr;
  }

  public double[][][] altUniformJoint(final int numVocab) {
    final double[][] emiss = altUniformEmissP(numVocab);
    final double[][] trans = altUniformTrans();
    return combineForJoint(emiss, trans);
  }

  public double[][][] randomJoint(final int size) {
    final double[][] emiss = randomEmiss(size);
    final double[][] trans = randomTrans();
    return combineForJoint(emiss, trans);
  }
  
  /**
   * Create uniform transition probabilities subject to the constraints imposed
   * by this encoding scheme
   * 
   * @return A sort-of uniform set of transition probabilities
   */
  public double[][] altUniformTrans() {
    int ntag = numTags();
    double[][] probs = new double[ntag][ntag];
    boolean[][] constraints = constraints();
    for (int q = 0; q < ntag; q++) {
      for (int r = 0; r < ntag; r++)
        probs[q][r] = constraints[q][r] ? 0 : 1;

      double sum = Util.sum(probs[q]);
      for (int r = 0; r < ntag; r++)
        probs[q][r] = Math.log(probs[q][r] / sum);
    }
    return probs;
  }

  public double[][] randomTrans() {
    int ntag = numTags();
    double[][] probs = altUniformTrans();
    double[][] rprobs = new double[ntag][ntag];
    Random rand = new Random();
    for (int q = 0; q < ntag; q++) {
      for (int r = 0; r < ntag; r++) 
        if (probs[q][r] != Double.NEGATIVE_INFINITY) 
          rprobs[q][r] = rand.nextDouble();
        else
          rprobs[q][r] = 0;
      
      double sum = Util.sum(rprobs[q]);
      for (int r = 0; r < ntag; r++)
        rprobs[q][r] = Math.log(rprobs[q][r] / sum);
    }
    
    return rprobs;
  }
}
