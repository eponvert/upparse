package upparse;

import java.io.*;

/**
 * Iterable of the tree strings associated with WSJ corpus files
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class WSJCorpusTreeStringIter extends CorpusTreeStringIter {
  
  private WSJCorpusTreeStringIter(final String[] _files) {
    super(_files);
  }
  
  public static WSJCorpusTreeStringIter fromFiles(final String[] files) {
    return new WSJCorpusTreeStringIter(files);
  }

  @Override
  protected FileTreeStringIter newFileTreeStringIter(final String file) 
  throws IOException {
    return new WSJFileTreeStringIter(file);
  }
}
