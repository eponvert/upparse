package upparse.corpus;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class CTBCorpusTreeIter extends CorpusTreeIter {

  private CTBCorpusTreeIter(Iterable<String> strIter) {
    super(strIter);
  }
  
  public static CorpusTreeIter fromFiles(final String[] files) {
    return new CTBCorpusTreeIter(CTBCorpusTreeStringIter.fromFiles(files));
  }
}
