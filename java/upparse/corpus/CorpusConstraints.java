package upparse.corpus;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public interface CorpusConstraints {
  /**
   * Checks the token and part of speech, and returns the transformed token
   * (e.g. lowercase) or empty string if this token is to be removed from the
   * structure 
   * @param token The original token
   * @param pos The original part of speech label
   * @return The new token
   */
  String getToken(String token, String pos);

  String wrap(String s);

}
