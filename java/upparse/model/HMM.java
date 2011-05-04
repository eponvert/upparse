package upparse.model;

import static java.lang.Math.*;
import upparse.corpus.*;
import upparse.util.*;

/**
 * Hidden Markov model
 * 
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class HMM extends SequenceModel {

  EmissionProbs emiss;
  double[][] trans;
  private double[] initTag;

  private HMM(final TagEncoder _encoder, final int[] _tokens,
      final EmissionProbs _emiss, final double[][] _trans,
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
        p = emiss.getProb(i, j);
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

  private static double[] logs(final double[] p) {
    final double[] lp = new double[p.length];
    for (int i = 0; i < p.length; i++)
      lp[i] = log(p[i]);
    return lp;
  }

  static double[] getInitTag(final int[] tag, final int ntag) {
    double[] initTag = new double[ntag];
    double log1 = log(1.), log0 = log(0.);
    for (int j = 0; j < ntag; j++)
      initTag[j] = j == tag[0] ? log1 : log0;
    return initTag;
  }

  private static double[][] getTrans(final double[][] transCount) {
    final int nTag = transCount.length;
    double[][] trans = new double[nTag][nTag];
    for (int j = 0; j < nTag; j++) {
      double sum = 0;
      for (int k = 0; k < nTag; k++)
        sum += transCount[j][k];
      sum = log(sum);

      for (int k = 0; k < nTag; k++)
        trans[j][k] = log(transCount[j][k]) - sum;
    }

    return trans;
  }

  public static HMM mleEstimate(final ChunkedSegmentedCorpus corpus,
      final TagEncoder encoder, final double smoothParam) throws EncoderError {
    return fromCounts(encoder.hardCounts(corpus), encoder,
        encoder.tokensFromClumpedCorpus(corpus), smoothParam);
  }

  public static HMM softCountEstimate(
      final StopSegmentCorpus corpus, 
      final TagEncoder encoder, 
      final double smoothParam) 
  {
    final int[] tokens = encoder.tokensFromStopSegmentCorpus(corpus);
    final double[][][] counts = encoder.softCounts(tokens);
    return fromCounts(counts, encoder, tokens, smoothParam);
  }

  public static HMM uniformEstimate(final StopSegmentCorpus corpus,
      final TagEncoder encoder, final double smoothParam) {
    final int[] tokens = encoder.tokensFromStopSegmentCorpus(corpus);
    final double[] initTag = logs(encoder.getInitTagProb());
    final EmissionProbs emiss = encoder.altUniformEmiss(corpus.getAlpha()
        .size(), smoothParam);
    final double[][] trans = encoder.altUniformTrans();
    return new HMM(encoder, tokens, emiss, trans, initTag);
  }


  public static HMM randomEstimate(StopSegmentCorpus corpus,
      TagEncoder encoder, double smoothParam) {
    final int[] tokens = encoder.tokensFromStopSegmentCorpus(corpus);
    final double[] initTag = logs(encoder.getInitTagProb());
    final EmissionProbs emiss = encoder.randomEmiss(corpus.getAlpha()
        .size(), smoothParam);
    final double[][] trans = encoder.randomTrans();
    return new HMM(encoder, tokens, emiss, trans, initTag);
  }

  public static HMM fromCounts(final double[][][] counts,
      final TagEncoder encoder, final int[] tokens, final double smoothParam) {

    assert counts.length != 0;
    final int nTag = encoder.numTags(), nTerm = counts[0].length;
    assert counts.length == nTag;
    double[][] emissCount = new double[nTag][nTerm];
    double[][] transCount = new double[nTag][nTag];
    for (int t = 0; t < nTag; t++)
      for (int w = 0; w < nTerm; w++)
        for (int _t = 0; _t < nTag; _t++) {
          final double c = counts[t][w][_t];
          transCount[t][_t] += c;
          emissCount[t][w] += c;
        }

    final double[] initTag = logs(encoder.getInitTagProb());
    final EmissionProbs emiss = EmissionProbs.fromCounts(emissCount,
        encoder.isStopPred(), smoothParam);
    final double[][] trans = getTrans(transCount);

    return new HMM(encoder, tokens, emiss, trans, initTag);
  }

  protected double nonLogTrans(int t1, int t2) {
    return exp(trans[t1][t2]);
  }
}
