package upparse.corpus;

import java.io.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class WSJFileTreeStringIter extends FileTreeStringIter {

  public WSJFileTreeStringIter(String file) throws IOException {
    super(file);
  }
  
  @Override
  protected boolean skipLine(String line) {
    return false;
  }
}
