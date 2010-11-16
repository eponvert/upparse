package upparse.eval;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class TreebankRBEval extends TreebankEval {
  
  private TreebankRBEval(
      final OutputType type, 
      final UnlabeledBracketSetCorpus gold, 
      final boolean checkTerms) {
    super(type, gold, checkTerms);
  }

  @Override
  protected UnlabeledBracketSetCorpus makeTreeCorpus(
      final ChunkedSegmentedCorpus output) {
    return UnlabeledBracketSetCorpus.fromArrays(output.asRB());
  }

  public static Eval fromUnlabeledBracketSets(OutputType type,
      UnlabeledBracketSetCorpus goldUnlabeledBracketSets, boolean checkTerms) {
    return new TreebankRBEval(type, goldUnlabeledBracketSets, checkTerms);
  }

  public static Eval fromUnlabeledBracketSets(OutputType type,
      UnlabeledBracketSetCorpus goldUnlabeledBracketSets) {
    return fromUnlabeledBracketSets(type, goldUnlabeledBracketSets, false);
  }
}
