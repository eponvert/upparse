package upparse;

/** Error from right-regular grammar parsing
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class RRGError extends SequenceModelError {
  private static final long serialVersionUID = 1L;

  public RRGError(String e) {
    super(e);
  }
}
