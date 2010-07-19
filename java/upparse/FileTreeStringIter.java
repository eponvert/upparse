package upparse;

import java.io.*;
import java.util.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public abstract class FileTreeStringIter implements Iterator<String> {

  private int n = 0;
  private final String[] lines;
  
  protected final String getLine() {
    return lines[n];
  }
  
  protected final void incLine() {
    n++;
  }
  
  protected final boolean hasAnotherLine() {
    return n < lines.length;
  }
  
  public FileTreeStringIter(final String file) throws IOException { 
    BufferedReader br = new BufferedReader(new FileReader(file));
    int numLines = 0;
    while (br.readLine() != null) numLines++;
    lines = new String[numLines];
    br = new BufferedReader(new FileReader(file));
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
