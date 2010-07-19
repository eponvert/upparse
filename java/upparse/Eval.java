package upparse;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public interface Eval {

  void eval(String string, ChunkedSegmentedCorpus output) throws EvalError;

}
