package upparse.eval;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class TreebankPrecisionEval extends TreebankEval {
  
  private TreebankPrecisionEval(
      final OutputType treebankprec,
      final UnlabeledBracketSetCorpus gold,
      final boolean checkTerms) {
    super(treebankprec, gold, checkTerms);
  }

  public static TreebankPrecisionEval fromUnlabeledBracketSets(
      final OutputType treebankprec, 
      final UnlabeledBracketSetCorpus gold, 
      final boolean checkTerms) {
    assert gold != null;
    return new TreebankPrecisionEval(treebankprec, gold, checkTerms);
  }

  @Override
  protected UnlabeledBracketSetCorpus makeTreeCorpus(
      ChunkedSegmentedCorpus output) {
    return UnlabeledBracketSetCorpus.fromArrays(output.asChunked());
  }

  public static Eval fromUnlabeledBracketSets(OutputType treebankprec,
      UnlabeledBracketSetCorpus goldUnlabeledBracketSets) {
    return fromUnlabeledBracketSets(treebankprec, goldUnlabeledBracketSets, false);
  }
}
