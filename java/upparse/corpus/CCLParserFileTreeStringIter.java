package upparse.corpus;

import java.io.*;

public class CCLParserFileTreeStringIter extends FileTreeStringIter {
  
  public CCLParserFileTreeStringIter(final String file) throws IOException {
    super(file);
  }
  

  @Override
  protected boolean skipLine(String line) {
    final String _line = line.trim();
    return _line.isEmpty() || _line.charAt(0) == '#';
  }
}
