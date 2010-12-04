package upparse.eval;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public abstract class TreebankEvalFromChunkerOutput extends Eval {

  private final TreebankEval treebankEval;

  protected TreebankEvalFromChunkerOutput(
      final OutputType type, 
      final UnlabeledBracketSetCorpus _gold) {
    this("Chunker-" + type, _gold);
  }
  
  private TreebankEvalFromChunkerOutput(
      final String name, final UnlabeledBracketSetCorpus _gold) {
    super(name);
    assert _gold != null;
    treebankEval = new TreebankEval(name, _gold);
  }

  protected abstract UnlabeledBracketSetCorpus makeTreeCorpus(
      ChunkedSegmentedCorpus output);
  
  @Override
  public void eval(String string, ChunkedSegmentedCorpus output)
      throws EvalError {
    addExperiment(treebankEval.getExperiment(string, makeTreeCorpus(output)));
  }
}
