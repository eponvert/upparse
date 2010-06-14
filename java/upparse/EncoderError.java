package upparse;

/** Error with {@link BIOEncoder}
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class EncoderError extends Exception {
  private static final long serialVersionUID = 1L;

  public EncoderError(String e) {
    super(e);
  }
}
