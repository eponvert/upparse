package upparse.corpus;

import java.io.*;

public class CCLParserTreeStringIter extends CorpusTreeStringIter {

  private CCLParserTreeStringIter(String[] _files) {
    super(_files);
  }

  @Override
  protected FileTreeStringIter newFileTreeStringIter(String file)
      throws IOException {
    return new CCLParserFileTreeStringIter(file);
  }

  public static Iterable<String> fromFiles(String[] files) {
    return new CCLParserTreeStringIter(files);
  }
}
