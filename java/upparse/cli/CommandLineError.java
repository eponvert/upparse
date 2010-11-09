package upparse.cli;

/**
 * Exception indicating command-line options usage error
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class CommandLineError extends Exception {

  public CommandLineError(String string) {
    super(string);
  }
  
  public CommandLineError() {
    super();
  }

  private static final long serialVersionUID = 1L;
}
