package upparse;

import java.io.*;
import java.util.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class WSJFileTreeStringIter implements Iterator<String> {

  private int n = 0;
  private final String[] lines;
  
  public WSJFileTreeStringIter(String file) throws IOException {
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
  
  private void updateN() {
    while (n < lines.length && 
        (lines[n].length() == 0 || lines[n].charAt(0) != '(')) 
      n++;
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
      sb.append(lines[i]);
    return sb.toString();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
