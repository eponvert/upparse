package upparse.eval;

/**
 * Exception to be thrown if there is a problem with experiment eval
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class EvalError extends Exception {
  private static final long serialVersionUID = 1L;

  public EvalError(String errorStr) {
    super(errorStr);
  }
}
