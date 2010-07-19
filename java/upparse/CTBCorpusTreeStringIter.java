package upparse;

import java.io.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
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
