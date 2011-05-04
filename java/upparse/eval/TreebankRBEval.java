package upparse.eval;

import upparse.corpus.*;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class TreebankRBEval extends TreebankEvalFromChunkerOutput {
  
  private TreebankRBEval(
      final OutputType type, 
      final UnlabeledBracketSetCorpus gold) { 
    super(type, gold);
  }

  @Override
  protected UnlabeledBracketSetCorpus makeTreeCorpus(
      final ChunkedSegmentedCorpus output) {
    return UnlabeledBracketSetCorpus.fromArrays(output.asRB());
  }

  public static Eval fromUnlabeledBracketSets(OutputType type,
      UnlabeledBracketSetCorpus goldUnlabeledBracketSets) {
    return new TreebankRBEval(type, goldUnlabeledBracketSets);
  }
}
