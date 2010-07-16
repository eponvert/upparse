package upparse;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class WSJKeepStop extends KeepStop {
  
  private WSJKeepStop() { }
  
  public static CorpusConstraints instance = new WSJKeepStop();

  @Override
  public String getToken(String token, String pos) {
    if (isStoppingPunc(token))
      return STOP;
    
    else 
      return WSJCorpusStandard.instance.getToken(token, pos);
  }
}
