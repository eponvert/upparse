package upparse;


/**
 * Simple data structure for corpus with sentences split by phrasal punctuation
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class StopSegmentCorpus {
  
  final int[][][] corpus;
  
  private StopSegmentCorpus(final int[][][] _corpus) {
    corpus = _corpus;
  }
  
  public static StopSegmentCorpus fromArrays(int[][][] corpus) {
    return new StopSegmentCorpus(corpus);
  }
  
  public ChunkedSegmentedCorpus toBaseChunkedSegmentedCorpus(Alpha alpha) {
    int[][][][] arrays = new int[corpus.length][][][];
    for (int i = 0; i < corpus.length; i++) {
      arrays[i] = new int[corpus[i].length][][];
      for (int j = 0; j < corpus[i].length; j++) {
        arrays[i][j] = new int[corpus[i][j].length][];
        for (int k = 0; k < corpus[i][j].length; k++) {
          arrays[i][j][k] = new int[] { corpus[i][j][k] };
        }
      }
    }
    
    return ChunkedSegmentedCorpus.fromArrays(arrays, alpha); 
  }

  /** Create a sub-corpus of setences whose length is lte to num */
  public StopSegmentCorpus filterLen(int num) {
    boolean[] filt = new boolean[corpus.length];
    int i = 0;
    int n = 0;
    for (int[][] s: corpus) {
      int len = 0;
      for (int[] seg: s)
        len += seg.length;
      if (len <= num) {
        n++;
        filt[i++] = true;
      }
    }
    
    int[][][] _corpus = new int[n][][];
    i = 0;
    for (int j = 0; j < corpus.length; j++)
      if (filt[j])
        _corpus[i++] = corpus[j];
    
    return fromArrays(_corpus);
  }
}
