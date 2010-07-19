package upparse;

import java.io.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class NegraCorpusTreeStringIter extends CorpusTreeStringIter {

  private NegraCorpusTreeStringIter(final String[] files) {
    super(files);
  }
  
  public static NegraCorpusTreeStringIter fromFiles(final String[] files) {
    return new NegraCorpusTreeStringIter(files);
  }

  @Override
  protected FileTreeStringIter newFileTreeStringIter(final String file)
      throws IOException {
    return new NegraFileTreeStringIter(file);
  }
}
