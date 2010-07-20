package upparse.corpus;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class NoTransform implements CorpusConstraints {

  public static final CorpusConstraints instance = new NoTransform();

  @Override
  public String getToken(final String token, final String pos) {
    return token;
  }

  @Override
  public String wrap(final String s) {
    return s;
  }
}
