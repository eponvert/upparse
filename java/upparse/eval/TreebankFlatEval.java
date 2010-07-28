package upparse.eval;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class TreebankFlatEval extends TreebankEval {
  
  private TreebankFlatEval(
      final String name, 
      final UnlabeledBracketSetCorpus gold, 
      final boolean checkTerms) {
    super(name, gold, checkTerms);
  }

  public static Eval fromUnlabeledBracketSets(
      final String name,
      final UnlabeledBracketSetCorpus unlabeledBracketSetCorpus, 
      final boolean checkTerms) {
    return new TreebankFlatEval(name, unlabeledBracketSetCorpus, checkTerms);
  }

  @Override
  protected UnlabeledBracketSetCorpus makeTreeCorpus(
      final ChunkedSegmentedCorpus output) {
    return UnlabeledBracketSetCorpus.fromArrays(output.asFlat());
  }
}
