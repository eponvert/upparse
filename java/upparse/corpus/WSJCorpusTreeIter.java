package upparse.corpus;


/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class WSJCorpusTreeIter extends CorpusTreeIter {
  
  private WSJCorpusTreeIter(
      final WSJCorpusTreeStringIter _strIter, Alpha alpha) {
    super(_strIter, alpha);
  }

  public static CorpusTreeIter fromFiles(final String[] files, Alpha alpha) {
    return new WSJCorpusTreeIter(
        WSJCorpusTreeStringIter.fromFiles(files), alpha);
  }
}
