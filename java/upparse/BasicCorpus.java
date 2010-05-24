package upparse;

import java.io.*;
import java.util.*;

/**
 * Very simple corpus data structure: arrays of string
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class BasicCorpus {

  private final String[][] corpus;

  /** Create simple corpus from filename 
   * @throws IOException if there's any trouble reading the file */
  public BasicCorpus(String file) throws IOException {
    this(new BufferedReader(new FileReader(new File(file))));
  }

  /** Create simple corpus from input stream 
   * @throws IOException when there's trouble reading the file */
  public BasicCorpus(BufferedReader bufferedReader) throws IOException {
    String s = null;
    List<String[]> corpusTmp = new ArrayList<String[]>();
    while ((s = bufferedReader.readLine()) != null) {
      corpusTmp.add(s.trim().split(" "));
    }
    
    corpus = corpusTmp.toArray(new String[0][]);
  }
  
  /** Iterate over the sentences of the corpus (as string arrays) */
  public Iterable<String[]> sIter() {
    return new Iterable<String[]>() {
      
      @Override
      public Iterator<String[]> iterator() {
        return new Iterator<String[]>() {
          
          int i = 0;
          
          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
          
          @Override
          public String[] next() {
            return corpus[i++];
          }
          
          @Override
          public boolean hasNext() {
            return i < corpus.length;
          }
        };
      }
    };
  }

  /** Returns corpus indexed by alphabet provided */
  public int[][] compiledCorpus(final Alpha a) {
    int[][] compiledCorpus = new int[corpus.length][];

    for (int i = 0; i < corpus.length; i++) {
      compiledCorpus[i] = new int[corpus[i].length];
      for (int j = 0; j < corpus[i].length; j++) 
        compiledCorpus[i][j] = a.getCode(corpus[i][j]);
    }
    
    return compiledCorpus;
  }
}
