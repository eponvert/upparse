package upparse.corpus;

import java.io.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class CTBFileTreeStringIter extends FileTreeStringIter {

  public CTBFileTreeStringIter(String file) throws IOException {
    super(file);
  }

  @Override
  protected boolean skipLine(String line) {
    final boolean skip = line.trim().charAt(0) == '<'; 
    return skip;
  }
}
