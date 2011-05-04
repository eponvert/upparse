package upparse.corpus;

import java.io.*;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class CTBFileTreeStringIter extends FileTreeStringIter {

  public CTBFileTreeStringIter(String file) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
    int n = 0;
    while (br.readLine() != null) n++;
    br.close();
    br =  new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
    init(br, n);
  }

  @Override
  protected boolean skipLine(String line) {
    if (line.trim().length() == 0)
      return true;
    
    final boolean skip = line.trim().charAt(0) == '<'; 
    return skip;
  }
}
