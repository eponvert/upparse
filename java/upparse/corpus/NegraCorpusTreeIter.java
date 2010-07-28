package upparse.corpus;


/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class NegraCorpusTreeIter extends CorpusTreeIter {

  private NegraCorpusTreeIter(
      final NegraCorpusTreeStringIter strIter, final Alpha _alpha) {
    super(strIter, _alpha);
  }

  public static NegraCorpusTreeIter fromFiles(
      final String[] files, final Alpha alpha) {
    return new NegraCorpusTreeIter(
        NegraCorpusTreeStringIter.fromFiles(files), alpha);
  }
}
