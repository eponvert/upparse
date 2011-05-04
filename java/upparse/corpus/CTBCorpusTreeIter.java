package upparse.corpus;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class CTBCorpusTreeIter extends CorpusTreeIter {

  private CTBCorpusTreeIter(
      final Iterable<String> strIter, final Alpha _alpha) {
    super(strIter, _alpha);
  }
  
  public static CorpusTreeIter fromFiles(
      final String[] files, final Alpha alpha) {
    return new CTBCorpusTreeIter(
        CTBCorpusTreeStringIter.fromFiles(files), alpha);
  }
}
