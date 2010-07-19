package upparse;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class CTBCorpusStandard implements CorpusConstraints {
  
  private CTBCorpusStandard() {  }
  
  public static final CorpusConstraints instance = new CTBCorpusStandard();
  
  private static String[] RM_POS = new String[] { "-NONE-", "PU" };

  @Override
  public String getToken(String token, String pos) {
    for (String p: RM_POS)
      if (p.equals(pos))
        return "";
    return token;
  }

  @Override
  public String wrap(String s) {
    return s;
  }
}
