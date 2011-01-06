package upparse.corpus;

import java.util.*;

import upparse.util.*;

/**
 * Utility for encoding clumped corpus and BIO tagged training set for HMM
 * 
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class SimpleBILOEncoder extends TagEncoder {

  public static final int STOP_STATE = 0, B_STATE = 1, I_STATE = 2,
      L_STATE = 3, O_STATE = 4;

  private static final int[] NON_STOP_TAGS = new int[] { B_STATE, I_STATE,
      L_STATE, O_STATE };

  private static double[] INIT_TAG_PROB = new double[5];

  private static boolean[][] CONSTRAINTS = new boolean[5][5];
  static {
    CONSTRAINTS[STOP_STATE][STOP_STATE] = false;
    CONSTRAINTS[STOP_STATE][B_STATE] = false;
    CONSTRAINTS[STOP_STATE][I_STATE] = true;
    CONSTRAINTS[STOP_STATE][L_STATE] = true;
    CONSTRAINTS[STOP_STATE][O_STATE] = false;
    CONSTRAINTS[B_STATE][STOP_STATE] = true;
    CONSTRAINTS[B_STATE][B_STATE] = true;
    CONSTRAINTS[B_STATE][I_STATE] = false;
    CONSTRAINTS[B_STATE][L_STATE] = false;
    CONSTRAINTS[B_STATE][O_STATE] = true;
    CONSTRAINTS[I_STATE][STOP_STATE] = true;
    CONSTRAINTS[I_STATE][B_STATE] = true;
    CONSTRAINTS[I_STATE][I_STATE] = false;
    CONSTRAINTS[I_STATE][L_STATE] = false;
    CONSTRAINTS[I_STATE][O_STATE] = true;
    CONSTRAINTS[L_STATE][STOP_STATE] = false;
    CONSTRAINTS[L_STATE][B_STATE] = false;
    CONSTRAINTS[L_STATE][I_STATE] = true;
    CONSTRAINTS[L_STATE][L_STATE] = true;
    CONSTRAINTS[L_STATE][O_STATE] = false;
    CONSTRAINTS[O_STATE][STOP_STATE] = false;
    CONSTRAINTS[O_STATE][B_STATE] = false;
    CONSTRAINTS[O_STATE][I_STATE] = true;
    CONSTRAINTS[O_STATE][L_STATE] = true;
    CONSTRAINTS[O_STATE][O_STATE] = false;

    INIT_TAG_PROB[STOP_STATE] = 1;
  }

  public SimpleBILOEncoder(String stop, Alpha alpha) {
    super(stop, alpha);
  }

  @Override
  public int[] bioTrain(ChunkedSegmentedCorpus corpus, int n)
      throws EncoderError {
    int[][][][] clumpedCorpus = corpus.getArrays();
    int[] train = new int[n];
    int i = 0, j;
    train[i++] = STOP_STATE;

    for (int[][][] s : clumpedCorpus) {
      if (s.length != 0) {
        for (int[][] seg : s) {
          for (int[] clump : seg) {
            if (clump.length == 1)
              train[i++] = O_STATE;
            else {
              train[i++] = B_STATE;
              for (j = 1; j < clump.length - 1; j++)
                train[i++] = I_STATE;
              train[i++] = L_STATE;
            }
          }
          train[i++] = STOP_STATE;
        }
      } else
        train[i++] = STOP_STATE;
    }

    return train;
  }

  @Override
  public ChunkedSegmentedCorpus clumpedCorpusFromBIOOutput(int[] tokens,
      int[] output) throws EncoderError {

    assert tokens.length == output.length;

    // Count the number of sentences
    int numS = -1; // don't count first __eos__
    for (int w : tokens)
      if (isEos(w))
        numS++;

    int[][][][] clumpedCorpus = new int[numS][][][];

    int i = 1, j, sInd = 0, nextEos = 1, numSeg, segInd, nextEoSeg, numClumps, clumpInd;

    assert output[i] != I_STATE && output[i] != L_STATE;

    // while (nextEos < tokens.length) {
    while (sInd < numS) {

      // process a sentence
      while (nextEos < tokens.length && !isEos(tokens[nextEos]))
        nextEos++;

      // count the number of segments
      numSeg = 0;
      j = i;
      while (j < tokens.length && j <= nextEos) {
        if (isStopOrEos(tokens[j]))
          numSeg++;
        j++;
      }

      clumpedCorpus[sInd] = new int[numSeg][][];
      segInd = 0;

      nextEoSeg = i;
      while (segInd < numSeg) {

        while (output[nextEoSeg] != STOP_STATE)
          nextEoSeg++;

        assert nextEoSeg <= nextEos;

        assert output[nextEoSeg] == STOP_STATE;
        assert nextEoSeg >= output.length - 1
            || output[nextEoSeg + 1] != I_STATE;

        // count the number of clumps
        numClumps = 0;
        j = i;
        while (j < nextEoSeg) {
          if (output[j] == B_STATE || output[j] == O_STATE)
            numClumps++;
          j++;
        }

        clumpedCorpus[sInd][segInd] = new int[numClumps][];
        clumpInd = 0;

        boolean firstIn = true;
        while (i < nextEoSeg) {
          if (output[i] == O_STATE) {
            clumpedCorpus[sInd][segInd][clumpInd] = new int[] { tokens[i++] };
            clumpInd++;
            assert output[i] != I_STATE && output[i] != L_STATE;
            firstIn = false;
          }

          else if (output[i] == B_STATE) {
            j = i + 1;
            assert output[j] == I_STATE || output[j] == L_STATE;
            while (output[j] == I_STATE)
              j++;
            assert output[j] == L_STATE;
            j++;
            clumpedCorpus[sInd][segInd][clumpInd] = Arrays.copyOfRange(tokens,
                i, j);

            // next clump
            i = j;
            clumpInd++;
            assert output[i] != I_STATE && output[i] != L_STATE;
            firstIn = false;
          }

          else {
            assert firstIn;
            assert output[i] != STOP_STATE : "output[i] should not be STOP";
            assert output[i] != I_STATE : "output[i] should not be I";
            assert output[i] != L_STATE: "output[i] should not be L";
            throw new EncoderError(String.format("Unexpected tag: %d",
                output[i]));
          }
        }

        assert i == nextEoSeg : String.format("i = %d nextEoSeg = %d", i,
            nextEoSeg);

        // next segment
        segInd++;
        nextEoSeg++;
        i = nextEoSeg;
      }

      // next sentence
      sInd++;

      // increment everything
      assert i == nextEos + 1 : String.format("(%d) i = %d nextEos = %d", sInd,
          i, nextEos);
      nextEos = i;
    }

    return ChunkedSegmentedCorpus.fromArrays(clumpedCorpus, alpha);
  }

  @Override
  public Ipredicate isStopPred() {
    return new Ipredicate() {
      @Override
      public boolean pred(final int t) {
        return t == STOP_STATE;
      }
    };
  }

  @Override
  public double[][] softTrain(final int[] train) {
    final double[][] counts = new double[train.length][numTags()];

    for (int i = 0; i < train.length; i++) {
      if (isStopOrEos(train[i]))
        counts[i][STOP_STATE] = 1;
      else {
        assert i > 0;
        assert i + 1 < train.length;

        if (isStopOrEos(train[i - 1]) && isStopOrEos(train[i + 1]))
          counts[i][O_STATE] = 1;

        else if (isStopOrEos(train[i - 1]))
          counts[i][B_STATE] = counts[i][O_STATE] = .5;

        else if (isStopOrEos(train[i + 1]))
          counts[i][O_STATE] = counts[i][L_STATE] = .5;

        else {
          counts[i][B_STATE] = .25;
          counts[i][I_STATE] = .25; 
          counts[i][O_STATE] = .25; 
          counts[i][L_STATE] = .25;
        }
      }
    }
    return counts;
  }

  @Override
  public boolean[][] constraints() {
    return CONSTRAINTS;
  }

  @Override
  public int numTags() {
    return 5;
  }

  @Override
  public double[] getInitTagProb() {
    return INIT_TAG_PROB;
  }

  @Override
  public int[] allNonStopTags() {
    return NON_STOP_TAGS;
  }

  @Override
  public int[] allStopTags() {
    return new int[] { STOP_STATE };
  }
}
