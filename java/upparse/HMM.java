package upparse;

import static java.lang.Math.*;
import static upparse.MaxVals.*;

/**
 * Hidden Markov model 
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class HMM extends SequenceModel {

  EmissionProbs emiss;
  double[][] trans;
  private double[] initTag;

  private HMM(
      final BIOEncoder _encoder, 
      final int[] _tokens,
      final EmissionProbs _emiss, 
      final double[][] _trans, 
      final double[] _initTag) {
    super(_encoder, _tokens, new int[_emiss.numTerms()][]);
    emiss = _emiss;
    trans = _trans;
    initTag = _initTag;
    updateTagDict();
    checkSanity();
  }

  @Override
  public double initTagProb(int j) {
    return initTag[j];
  }

  @Override
  public double arcprob(int j, int w, int k) {
    final double tprob = trans[j][k], eprob = emiss.getProb(j, w);
    return tprob + eprob;
  }

  @Override
  public void update(final double[][][] counts) {
    final int ntag = counts.length;
    final int nterm = counts[0].length;
    
    assert ntag == numTags();
    assert nterm == numTerms();
    
    // update emission counts
    final double[][] emissCnt = new double[ntag][nterm];
    for (int t = 0; t < ntag; t++)
      for (int w = 0; w < nterm; w++)
        for (int _t = 0; _t < ntag; _t++) 
          emissCnt[t][w] += counts[t][w][_t];
    emiss.update(emissCnt);
    
    // update transition counts
    final double[][] transCount = new double[ntag][ntag];
    for (int t = 0; t < ntag; t++)
      for (int w = 0; w < nterm; w++)
        for (int _t = 0; _t < ntag; _t++)
          transCount[t][_t] += counts[t][w][_t];

    for (int j = 0; j < ntag; j++) {
      final double sum = log(Util.sum(transCount[j]));
      for (int k = 0; k < ntag; k++) {
        trans[j][k] = log(transCount[j][k]) - sum;
        assert !Double.isNaN(trans[j][k]);
      }
    }
  }

  @Override
  public int numTags() {
    return emiss.numTags();
  }

  @Override
  public int numTerms() {
    return emiss.numTerms();
  }

  @Override
  public double termProb(int tag, int term) {
    return emiss.getProb(tag, term);
  }

  @Override
  public void checkSanity() {
    for (int i = 0; i < emiss.numTags(); i++) {
      double s = 0, p; 
      for (int j = 0; j < emiss.numTerms(); j++) {
        p = emiss.getProb(i,j);
        assert !Double.isNaN(p);
        s += Math.exp(p);
      }
      assert abs(s - 1) < 1e-5;
    }

    for (int i = 0; i < trans.length; i++) {
      double s = 0.;
      for (int j = 0; j < trans[i].length; j++)
        s += exp(trans[i][j]);
      assert abs(s - 1) < 1e-5;
    }
    
    super.checkSanity();
  }

  private static EmissionProbs getEmiss(final int[] tokens, final int[] tags) {
    assert tokens.length == tags.length;

    final int nterm = arrayMax(tokens) + 1, ntag = arrayMax(tags) + 1;
    
    int i, j, w;

    final int[][] emissCount = new int[ntag][nterm];
    for (i = 0; i < tokens.length; i++)
      emissCount[tags[i]][tokens[i]]++;

    double[][] emissCountD = new double[ntag][nterm];
    for (j = 0; j < ntag; j++) 
      for (w = 0; w < nterm; w++)
        emissCountD[j][w] = (double) emissCount[j][w];
    
    return EmissionProbs.fromCounts(emissCountD);
  }

  static double[] getInitTag(final int[] tag, final int ntag) {
    double[] initTag = new double[ntag];
    double log1 = log(1.), log0 = log(0.);
    for (int j = 0; j < ntag; j++)
      initTag[j] = j == tag[0] ? log1 : log0;
    return initTag;
  }

  private static double[][] getTrans(final int[] tags, final int ntag) {

    int[][] transCount = new int[ntag][ntag];
    int i, j, k;
    double sum;

    for (i = 1; i < tags.length; i++)
      transCount[tags[i-1]][tags[i]]++;

    double[][] transCountD = new double[ntag][ntag];
    for (j = 0; j < ntag; j++)
      for (k = 0; k < ntag; k++)
        transCountD[j][k] = (double) transCount[j][k];

    double[][] trans = new double[ntag][ntag];
    for (j = 0; j < ntag; j++) {
      sum = 0.;
      for (k = 0; k < ntag; k++) 
        sum += transCountD[j][k];
      sum = log(sum);

      for (k = 0; k < ntag; k++)
        trans[j][k] = log(transCount[j][k]) - sum;
    }

    return trans;
  }

  public static HMM mleEstimate(
      final int[] tokens, 
      final int[] tags, 
      final BIOEncoder encoder) {
    final EmissionProbs emiss = getEmiss(tokens, tags);
    int ntag = emiss.numTags();
    final double[] initTag = getInitTag(tags, ntag);
    final double[][] trans = getTrans(tags, ntag);
    return new HMM(encoder, tokens, emiss, trans, initTag);
  }

  public static HMM mleEstimate(
      final ChunkedSegmentedCorpus corpus, final BIOEncoder encoder) 
  throws HMMError, EncoderError {
    int[] tokens = encoder.tokensFromClumpedCorpus(corpus);
    int[] bioTrain = encoder.bioTrain(corpus, tokens.length);
    return mleEstimate(tokens, bioTrain, encoder);
  }
}
