package upparse.eval;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class TreebankRBEval extends TreebankEval {
  
  private TreebankRBEval(
      final String name, 
      final UnlabeledBracketSetCorpus gold, 
      final boolean checkTerms) {
    super(name, gold, checkTerms);
  }

  @Override
  protected UnlabeledBracketSetCorpus makeTreeCorpus(
      final ChunkedSegmentedCorpus output) {
    return UnlabeledBracketSetCorpus.fromArrays(output.asRB());
  }

  public static Eval fromUnlabeledBracketSets(String string,
      UnlabeledBracketSetCorpus goldUnlabeledBracketSets, boolean checkTerms) {
    return new TreebankRBEval(string, goldUnlabeledBracketSets, checkTerms);
  }
}
