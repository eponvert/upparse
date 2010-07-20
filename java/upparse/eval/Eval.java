package upparse.eval;

import java.io.*;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public interface Eval {

  void eval(String string, ChunkedSegmentedCorpus output) throws EvalError;

  public String getName();

  void writeSummary(String evalType, PrintStream out, boolean onlyLast)
      throws EvalError;

}
