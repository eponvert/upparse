package upparse;

import java.io.*;
import java.util.*;

/**
 * Iterable of the tree strings associated with WSJ corpus files
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class WSJCorpusTreeStringIter implements Iterable<String> {
  
  final private String[] files;

  public WSJCorpusTreeStringIter(final String[] _files) {
    files = _files;
  }

  @Override
  public Iterator<String> iterator() {
    return new Iterator<String>() {
      
      int i = 0;
      WSJFileTreeStringIter fileStrIter = null;
      
      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
      
      @Override
      public String next() {
        updateFileStrIter();
        return fileStrIter.next();
      }
      
      @Override
      public boolean hasNext() {
        if (noFiles()) return false;
        updateFileStrIter();
        return fileStrIter.hasNext();
      }
      
      private boolean noFiles() {
        return fileStrIter == null && i >= files.length;
      }
      
      private void updateFileStrIter() {
        try {
          if (fileStrIter == null)
            fileStrIter = new WSJFileTreeStringIter(files[i++]);
          
          while (!fileStrIter.hasNext() && i < files.length)
            fileStrIter = new WSJFileTreeStringIter(files[i++]);

        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }        
    };
  }
}
