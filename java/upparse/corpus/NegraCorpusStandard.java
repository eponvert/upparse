package upparse.corpus;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class NegraCorpusStandard implements CorpusConstraints {
  
  private NegraCorpusStandard() { }
  
  private static String[] RM_POS = new String[] { 
    "$,", "$.", "$.-CD", "$.-CJ", "$*LRB*", "$*LRB*-NMC", "$*LRB*-PNC", 
    "$*LRB*-UC", "$.-NK", "$,-NMC", "$.-NMC", "$.-PNC", "$.-UC", "--" };
  
  public static final CorpusConstraints instance = new NegraCorpusStandard();

  @Override
  public String getToken(String token, String pos) {
    if (pos.startsWith("*"))
      return "";
    
    for (String p: RM_POS)
      if (p.equals(pos))
        return "";
    
    return token.toLowerCase();
  }

  @Override
  public String wrap(String s) {
    return s;
  }
}
