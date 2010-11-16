package upparse.eval;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class NullEval extends Eval {
  
  private static NullEval INSTANCE = new NullEval();

  private NullEval() {
    super("null");
  }

  @Override
  public void eval(String string, ChunkedSegmentedCorpus output)
      throws EvalError { }

  public static Eval instance() { return INSTANCE; }
}
