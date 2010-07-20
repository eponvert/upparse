package upparse.corpus;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class WSJCorpusStandard implements CorpusConstraints {
  
  private WSJCorpusStandard() { }
  
  private static String[] RM_POS = new String[] { 
    "-NONE-", ",", ".", ":", "``", "''", "-LRB-", "-RRB-", "$", "#" };

  public static final CorpusConstraints instance = new WSJCorpusStandard();

  @Override
  public String getToken(String token, String pos) {
    for (String p: RM_POS) 
      if (p.equals(pos))
        return "";
    return token.toLowerCase();
  }

  @Override
  public String wrap(final String s) {
    return s;
  }
}
