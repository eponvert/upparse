package upparse.model;


/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class ChunkerError extends Exception {
  private static final long serialVersionUID = 4008234820976511784L;

  public ChunkerError(Exception e) {
    super(e);
  }
}
