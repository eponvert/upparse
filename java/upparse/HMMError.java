package upparse;

/**
 * Exceptions for problems creating and using HMM
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class HMMError extends SequenceModelError {
  private static final long serialVersionUID = 1L;

  public HMMError(String errStr) {
    super(errStr);
  }
}
