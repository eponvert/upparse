package upparse.corpus;

import java.io.*;
import java.util.*;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public abstract class FileTreeStringIter implements Iterator<String> {

  private int n = 0;
  private String[] lines;
  
  protected final String getLine() {
    return lines[n];
  }
  
  protected final void incLine() {
    n++;
  }
  
  protected final boolean hasAnotherLine() {
    return n < lines.length;
  }
  
  public FileTreeStringIter() { } 
  
  public FileTreeStringIter(final String file) throws IOException { 
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
    int n = 0;
    while (br.readLine() != null) n++;
    br.close();
    br =  new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
    init(br, n);
  }
//    BufferedReader br = new BufferedReader(new FileReader(file));
//    int numLines = 0;
//    while (br.readLine() != null) numLines++;
//    br = new BufferedReader(new FileReader(file));
//    init(br, numLines);
//  }
  
  public void init(final BufferedReader br, final int numLines) throws IOException {
    lines = new String[numLines];
    int i = 0;
    String line;
    while ((line = br.readLine()) != null) lines[i++] = line;
    updateN();
  }

  @Override
  public boolean hasNext() {
    updateN();
    return n < lines.length;
  }

  @Override
  public String next() {
    int prev = n;
    n++;
    updateN();
    StringBuffer sb = new StringBuffer();
    for (int i = prev; i < n; i++)
      if (!skipLine(lines[i]))
        sb.append(lines[i]);
    return sb.toString();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
  
  protected abstract boolean skipLine(String line);

  protected void updateN() {
    while (hasAnotherLine() && 
           (getLine().length() == 0 || getLine().charAt(0) != '(' || 
            skipLine(getLine())))
      incLine();
          
  }
}
