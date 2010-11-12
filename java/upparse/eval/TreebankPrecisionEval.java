package upparse.eval;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class TreebankPrecisionEval extends TreebankEval {
  
  private TreebankPrecisionEval(
      final String name,
      final UnlabeledBracketSetCorpus gold,
      final boolean checkTerms) {
    super(name, gold, checkTerms);
  }

  public static TreebankPrecisionEval fromUnlabeledBracketSets(
      final String name, 
      final UnlabeledBracketSetCorpus gold, 
      final boolean checkTerms) {
    assert gold != null;
    return new TreebankPrecisionEval(name, gold, checkTerms);
  }

  @Override
  protected UnlabeledBracketSetCorpus makeTreeCorpus(
      ChunkedSegmentedCorpus output) {
    return UnlabeledBracketSetCorpus.fromArrays(output.asChunked());
  }

  public static Eval fromUnlabeledBracketSets(String string,
      UnlabeledBracketSetCorpus goldUnlabeledBracketSets) {
    return fromUnlabeledBracketSets(string, goldUnlabeledBracketSets, false);
  }
}
