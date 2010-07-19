package upparse;


/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class NegraCorpusTreeIter extends CorpusTreeIter {

  private NegraCorpusTreeIter(final NegraCorpusTreeStringIter strIter) {
    super(strIter);
  }

  public static NegraCorpusTreeIter fromFiles(final String[] files) {
    return new NegraCorpusTreeIter(NegraCorpusTreeStringIter.fromFiles(files));
  }
}
