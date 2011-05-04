package upparse.corpus;

import upparse.util.*;

/**
 * Utility to encode clumped corpus as a BIO encoded training set with 
 * "grandparent" tags, for emulating a second-order HMM
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class GrandparentWithStopBIOEncoder extends TagEncoder {
  
  private final TagEncoder simpleEncoder;
  
  public static final int
    STOP_STATE      = 0,
    STOP_B_STATE    = 1,
    STOP_O_STATE    = 2,
    B_I_STATE       = 3,
    I_B_STATE       = 4,
    I_I_STATE       = 5,
    I_O_STATE       = 6,
    O_B_STATE       = 7,
    O_O_STATE       = 8;
  
  private static final boolean[][] CONSTRAINTS = new boolean[9][9];

  private static final double ATHIRD = 1.0/3.0;

  private static final double ASIXTH = 1.0/6.0;
  
  private static final double[] INIT_TAG_PROB = new double[9];
  
  private static final int[] NON_STOP_TAGS = new int[] {
    STOP_B_STATE, STOP_O_STATE, B_I_STATE, I_B_STATE, I_I_STATE, I_O_STATE, 
    O_B_STATE, O_O_STATE };
  
  private static final int[] STOP_TAGS = new int[] { STOP_STATE }; 
  
  static {
    INIT_TAG_PROB[STOP_STATE] = 1;
    
    for (int i = 0; i < CONSTRAINTS.length; i++)
      for (int j = 0; j < CONSTRAINTS.length; j++)
        CONSTRAINTS[i][j] = true;
    
    int[]
        stop1states = new int[] { STOP_B_STATE, STOP_O_STATE, STOP_STATE },
        b1states = new int[] { B_I_STATE },
        i1states = new int[] { I_B_STATE, I_I_STATE, I_O_STATE, STOP_STATE },
        o1states = new int[] { O_B_STATE, O_O_STATE, STOP_STATE },
        stop2states = new int[] { STOP_STATE },
        b2states = new int[] { STOP_B_STATE, I_B_STATE, O_B_STATE },
        i2states = new int[] { B_I_STATE, I_I_STATE },
        o2states = new int[] { STOP_O_STATE, I_O_STATE, O_O_STATE };
    
    int[][][] pairs = new int[][][] {
        new int[][] { stop2states, stop1states },
        new int[][] { b2states, b1states },
        new int[][] { i2states, i1states },
        new int[][] { o2states, o1states }
    };
    
    for (int[][] pair: pairs)
      for (int i: pair[0])
        for (int j: pair[1])
          CONSTRAINTS[i][j] = false;
  }
    
  public GrandparentWithStopBIOEncoder(String stop, Alpha alpha) {
    super(stop, alpha);
    simpleEncoder = new SimpleBIOEncoder(stop, alpha);
  }

  @Override
  public int[] bioTrain(ChunkedSegmentedCorpus corpus, int n) throws EncoderError {
    int[] simpTrain = simpleEncoder.bioTrain(corpus, n);
    int[] train = new int[simpTrain.length];
    
    assert simpTrain[0] == SimpleBIOEncoder.STOP_STATE;
    train[0] = STOP_STATE;
    
    for (int i = 1; i < simpTrain.length; i++) {
      if (simpTrain[i] == SimpleBIOEncoder.STOP_STATE) 
        train[i] = STOP_STATE;

      else if (simpTrain[i-1] == SimpleBIOEncoder.STOP_STATE) {
        switch (simpTrain[i]) {
          case SimpleBIOEncoder.B_STATE:
            train[i] = STOP_B_STATE; break;
            
          case SimpleBIOEncoder.O_STATE:
            train[i] = STOP_O_STATE; break;
            
          default: 
            throw new EncoderError("Unexpected tag sequence");
        }
      } else if (simpTrain[i-1] == SimpleBIOEncoder.B_STATE) {
        switch (simpTrain[i]) {
          case SimpleBIOEncoder.I_STATE:
            train[i] = B_I_STATE; break;
            
          default: 
            throw new EncoderError("Unexpected tag sequence");
        }    
      } else if (simpTrain[i-1] == SimpleBIOEncoder.I_STATE) {
        switch (simpTrain[i]) {
          case SimpleBIOEncoder.B_STATE:
            train[i] = I_B_STATE; break;

          case SimpleBIOEncoder.I_STATE:
            train[i] = I_I_STATE; break;
            
          case SimpleBIOEncoder.O_STATE:
            train[i] = I_O_STATE; break;
            
          default: 
            throw new EncoderError("Unexpected tag sequence");
        }
      } else if (simpTrain[i-1] == SimpleBIOEncoder.O_STATE) {
        switch (simpTrain[i]) {
          case SimpleBIOEncoder.B_STATE:
            train[i] = O_B_STATE; break;
          
          case SimpleBIOEncoder.O_STATE:
            train[i] = O_O_STATE; break;
            
          default: 
            throw new EncoderError("Unexpected tag sequence");
        }
      } else throw new EncoderError("Unexpected tag sequence");
    }
      
    return train;
  }

  @Override
  public ChunkedSegmentedCorpus clumpedCorpusFromBIOOutput(
      int[] tokens, int[] output) throws EncoderError {
    int[] simpleOutput = new int[output.length];
    for (int i = 0; i < output.length; i++) {
      switch (output[i]) {
        case (STOP_STATE):  
          simpleOutput[i] = SimpleBIOEncoder.STOP_STATE; break; 
        
        case (STOP_B_STATE):
        case (O_B_STATE):
        case (I_B_STATE): 
          simpleOutput[i] = SimpleBIOEncoder.B_STATE; break;
          
        case (B_I_STATE):
        case (I_I_STATE):
          simpleOutput[i] = SimpleBIOEncoder.I_STATE; break;
          
        case (STOP_O_STATE):
        case (I_O_STATE):
        case (O_O_STATE):
          simpleOutput[i] = SimpleBIOEncoder.O_STATE; break;
          
        default:
          throw new EncoderError(
              String.format("Unexpected tag %d", output[i]));
          
      }
    }

    return simpleEncoder.clumpedCorpusFromBIOOutput(tokens, simpleOutput);
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
    final double[][] tags = new double[train.length][numTags()];
    for (int i = 0; i < train.length; i++)
      if (isStopOrEos(train[i]))
        tags[i][STOP_STATE] = 1;
      else {
        assert i != 0;
        assert i + 1 < train.length;
        if (isStopOrEos(train[i-1]) && isStopOrEos(train[i+1]))
          tags[i][STOP_O_STATE] = 1;
        
        else if (isStopOrEos(train[i-1]))
          tags[i][STOP_O_STATE] = tags[i][STOP_B_STATE] = .5;
        
        else {
          assert i > 1;
          
          if (isStopOrEos(train[i-2]) && isStopOrEos(train[i+1]))
            tags[i][O_O_STATE] = tags[i][B_I_STATE] = .5;

          else if (isStopOrEos(train[i-2]))
            tags[i][O_B_STATE] = tags[i][O_O_STATE] = tags[i][B_I_STATE] = 
              ATHIRD;

          else
            tags[i][B_I_STATE] = tags[i][I_B_STATE] = tags[i][I_I_STATE] = 
              tags[i][I_O_STATE] = tags[i][O_B_STATE] = tags[i][O_O_STATE] = 
                ASIXTH;

        }
      }
    return tags;
  }

  @Override
  public int numTags() {
    return 9;
  }

  @Override
  public boolean[][] constraints() {
    return CONSTRAINTS;
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
    return STOP_TAGS;
  }
}
