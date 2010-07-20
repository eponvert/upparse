package upparse.eval;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public interface Eval {

  void eval(String string, ChunkedSegmentedCorpus output) throws EvalError;

}
