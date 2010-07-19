package upparse;


/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class WSJCorpusTreeIter extends CorpusTreeIter {
  
  private WSJCorpusTreeIter(
      final WSJCorpusTreeStringIter _strIter) {
    super(_strIter);
  }

  public static CorpusTreeIter fromFiles(final String[] files) {
    return new WSJCorpusTreeIter(WSJCorpusTreeStringIter.fromFiles(files));
  }
}
