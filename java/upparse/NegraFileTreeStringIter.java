package upparse;

import java.io.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class NegraFileTreeStringIter extends FileTreeStringIter {

  public NegraFileTreeStringIter(String file) throws IOException {
    super(file);
  }

  @Override
  protected boolean skipLine(String line) {
    return line.trim().charAt(0) == '%';
  }
}
