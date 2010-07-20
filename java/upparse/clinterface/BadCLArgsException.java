package upparse.clinterface;

/**
 * Exception indicating command-line options usage error
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class BadCLArgsException extends Exception {

  public BadCLArgsException(String string) {
    super(string);
  }
  
  public BadCLArgsException() {
    super();
  }

  private static final long serialVersionUID = 1L;
}
