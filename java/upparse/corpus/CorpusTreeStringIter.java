package upparse.corpus;

import java.io.*;
import java.util.*;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public abstract class CorpusTreeStringIter implements Iterable<String> {

  final private String[] files;


  protected CorpusTreeStringIter(String[] _files) {
    files = _files;
  }
  
  @Override
  public Iterator<String> iterator() {
    return new Iterator<String>() {
      
      int i = 0;
      FileTreeStringIter fileStrIter = null;
      
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
            fileStrIter = newFileTreeStringIter(files[i++]);
          
          while (!fileStrIter.hasNext() && i < files.length)
            fileStrIter = newFileTreeStringIter(files[i++]);

        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }        
    };
  }

  protected abstract FileTreeStringIter newFileTreeStringIter(String file)
      throws IOException;
}
