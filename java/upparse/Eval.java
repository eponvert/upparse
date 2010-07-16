package upparse;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public interface Eval {

  /**
   * @param string
   * @param outputCorpus
   * @throws EvalError
   */
  void eval(String string, ChunkedCorpus outputCorpus) throws EvalError;

}
