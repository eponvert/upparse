package upparse.corpus;

public abstract class GetString {
  public abstract String getString(int sent, int chunk, int term);

  public static GetString altGetString(
      final String[][] outputText,
      final int[][][] corpus,
      final Alpha alpha) {
    if (outputText == null)
      return new GetString() {
      @Override
      public String getString(int sent, int chunk, int term) {
        return alpha.getString(corpus[sent][chunk][term]);
      }
    };
    else {  
      final String[][][] newStr = new String[corpus.length][][];
      for (int i = 0; i < corpus.length; i++) {
        int w = 0;
        newStr[i] = new String[sumlen(corpus[i])][];
        for (int j = 0; j < corpus[i].length; j++) {
          newStr[i][j] = new String[corpus[i][j].length];
          for (int k = 0; k < corpus[i][j].length; k++) {
            newStr[i][j][k] = outputText[i][w++];
          }
        }
      }
      
      return new GetString() {
        @Override
        public String getString(int sent, int chunk, int term) {
          return newStr[sent][chunk][term];
        }
      };
    }
  }
  
  private static int sumlen(int[][] a) { 
    int n = 0;
    for (int[] b: a)
      n += b.length;
    return n;
  }
}

