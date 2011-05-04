package upparse.corpus;

import java.io.*;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class CTBCorpusTreeStringIter extends CorpusTreeStringIter {

  private CTBCorpusTreeStringIter(final String[] files) {
    super(files);
  }
  
  public static CTBCorpusTreeStringIter fromFiles(final String[] files) {
    return new CTBCorpusTreeStringIter(files);
  }

  @Override
  protected FileTreeStringIter newFileTreeStringIter(final String file)
      throws IOException {
    return new CTBFileTreeStringIter(file);
  }
}
