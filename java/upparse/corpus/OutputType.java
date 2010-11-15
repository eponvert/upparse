package upparse.corpus;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public enum OutputType {
  CLUMP, NPS, TREEBANKRB, TREEBANKPREC, TREEBANKFLAT, NONE;

  public static String outputTypesHelp() {
    return 
    "Evaluation types:\n" +
    "  CLUMP\n" + 
    "  NPS\n"+
    "  TREEBANKPREC\n"+
    "  TREEBANKFLAT\n"+
    "  TREEBANKRB\n"+
    "  NONE";
  }
}
