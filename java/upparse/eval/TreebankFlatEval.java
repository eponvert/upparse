package upparse.eval;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class TreebankFlatEval extends TreebankEvalFromChunkerOutput {
  
  private TreebankFlatEval(
      final OutputType type, 
      final UnlabeledBracketSetCorpus gold) { 
    super(type, gold);
  }

  public static Eval fromUnlabeledBracketSets(
      final OutputType type,
      final UnlabeledBracketSetCorpus unlabeledBracketSetCorpus) { 
    return new TreebankFlatEval(type, unlabeledBracketSetCorpus);
  }

  @Override
  protected UnlabeledBracketSetCorpus makeTreeCorpus(
      final ChunkedSegmentedCorpus output) {
    return UnlabeledBracketSetCorpus.fromArrays(output.asFlat());
  }
}
