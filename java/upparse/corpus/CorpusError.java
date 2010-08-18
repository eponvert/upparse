package upparse.corpus;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class CorpusError extends Exception {
  private static final long serialVersionUID = -4442353664126538807L;

  public CorpusError(String msg) {
    super(msg);
  }
}
