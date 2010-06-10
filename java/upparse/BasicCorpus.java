package upparse;

import java.io.*;

/**
 * Very simple corpus data structure: arrays of string
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class BasicCorpus {

  private final String[][] corpus;

  /** Create simple corpus from filename 
   * @throws IOException if there's any trouble reading the file */
  public BasicCorpus(String file) throws IOException {
    // count the number of sentences
    BufferedReader br = new BufferedReader(new FileReader(new File(file)));
    int numS = 0;
    while (br.readLine() != null) numS++;
    br.close();
    
    corpus = new String[numS][];
    
    br = new BufferedReader(new FileReader(new File(file)));
    int i = 0;
    for (String s = br.readLine(); s != null; s = br.readLine()) {
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

  public StopSegmentCorpus toStopSegmentCorpus(Alpha alpha, int stopv) {
    return new StopSegmentCorpus(compiledCorpus(alpha), stopv);
  }

  public ChunkedSegmentedCorpus toBaseChunkedSegmentedCorpus(final Alpha alpha,
      final int stopv) {
    return toStopSegmentCorpus(alpha, stopv)
      .toBaseChunkedSegmentedCorpus(alpha);
  }
}
