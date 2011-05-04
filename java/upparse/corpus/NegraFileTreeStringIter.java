package upparse.corpus;

import java.io.*;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class NegraFileTreeStringIter extends FileTreeStringIter {

  public NegraFileTreeStringIter(String file) throws IOException {
    super(file);
  }

  @Override
  protected boolean skipLine(String line) {
    final String _line = line.trim();
    return _line.isEmpty() || _line.charAt(0) == '%';
  }
}
