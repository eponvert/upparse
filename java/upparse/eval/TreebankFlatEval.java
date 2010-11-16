package upparse.eval;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class TreebankFlatEval extends TreebankEval {
  
  private TreebankFlatEval(
      final OutputType type, 
      final UnlabeledBracketSetCorpus gold, 
      final boolean checkTerms) {
    super(type, gold, checkTerms);
  }

  public static Eval fromUnlabeledBracketSets(
      final OutputType type,
      final UnlabeledBracketSetCorpus unlabeledBracketSetCorpus, 
      final boolean checkTerms) {
    return new TreebankFlatEval(type, unlabeledBracketSetCorpus, checkTerms);
  }

  @Override
  protected UnlabeledBracketSetCorpus makeTreeCorpus(
      final ChunkedSegmentedCorpus output) {
    return UnlabeledBracketSetCorpus.fromArrays(output.asFlat());
  }

  public static Eval fromUnlabeledBracketSets(OutputType type,
      UnlabeledBracketSetCorpus goldUnlabeledBracketSets) {
    return fromUnlabeledBracketSets(type, goldUnlabeledBracketSets, false);
  }
}
