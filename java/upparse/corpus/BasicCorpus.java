package upparse.corpus;

import java.io.*;

/**
 * Very simple corpus data structure: arrays of string
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class BasicCorpus {

  private final String[][] corpus;

  /** Create simple corpus from filename 
   * @param trainSents The number of sentences to keep
   * @throws IOException if there's any trouble reading the file */
  public BasicCorpus(String file, int trainSents) throws IOException {
    // count the number of sentences
    BufferedReader br = new BufferedReader(new FileReader(new File(file)));
    int numS = 0;
    while (br.readLine() != null) numS++;
    if (trainSents > 0)
      numS = Math.min(numS, trainSents);
    br.close();
    
    corpus = new String[numS][];
    
    br = new BufferedReader(new FileReader(new File(file)));
    int i = 0;
    for (String s = br.readLine(); s != null && i < numS; s = br.readLine()) {
      s = s.trim();
      if (s.equals(""))
        corpus[i++] = new String[0];
      else
        corpus[i++] = s.split(" ");
    }
    br.close();
    
    assert i == numS;
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
