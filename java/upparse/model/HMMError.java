package upparse.model;

/**
 * Exceptions for problems creating and using HMM
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class HMMError extends SequenceModelError {
  private static final long serialVersionUID = 1L;

  public HMMError(String errStr) {
    super(errStr);
  }
}
