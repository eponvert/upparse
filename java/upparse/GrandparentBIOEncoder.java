package upparse;

/**
 * Utility to encode clumped corpus as a BIO encoded training set with 
 * "grandparent" tags, for emulating a second-order HMM
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class GrandparentBIOEncoder extends BIOEncoder {
  
  private final SimpleBIOEncoder simpleEncoder;
  
  public static final int 
    STOP_B_STATE    = 0,
    STOP_O_STATE    = 1,
    B_I_STATE       = 2,
    I_B_STATE       = 3,
    I_I_STATE       = 4,
    I_O_STATE       = 5,
    O_B_STATE       = 6,
    O_O_STATE       = 7,
    I_STOP_STATE    = 8,
    O_STOP_STATE    = 9,
    STOP_STOP_STATE = 10;
    
  public GrandparentBIOEncoder(String stop, Alpha alpha) {
    super(stop, alpha);
    simpleEncoder = new SimpleBIOEncoder(stop, alpha);
  }

  @Override
  public int[] bioTrain(ChunkedSegmentedCorpus corpus, int n) throws EncoderError {
    int[] simpTrain = simpleEncoder.bioTrain(corpus, n);
    int[] train = new int[simpTrain.length];
    
    assert simpTrain[0] == SimpleBIOEncoder.STOP_STATE;
    train[0] = O_STOP_STATE;
    
    for (int i = 1; i < simpTrain.length; i++) {
      if (simpTrain[i-1] == SimpleBIOEncoder.STOP_STATE) {
        switch (simpTrain[i]) {
          case SimpleBIOEncoder.B_STATE:
            train[i] = STOP_B_STATE; break;
            
          case SimpleBIOEncoder.O_STATE:
            train[i] = STOP_O_STATE; break;
            
          case SimpleBIOEncoder.STOP_STATE:
            train[i] = STOP_STOP_STATE; break;
            
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
            
          case SimpleBIOEncoder.STOP_STATE:
            train[i] = I_STOP_STATE; break;
            
          default: 
            throw new EncoderError("Unexpected tag sequence");
        }
      } else if (simpTrain[i-1] == SimpleBIOEncoder.O_STATE) {
        switch (simpTrain[i]) {
          case SimpleBIOEncoder.B_STATE:
            train[i] = O_B_STATE; break;
          
          case SimpleBIOEncoder.O_STATE:
            train[i] = O_O_STATE; break;
            
          case SimpleBIOEncoder.STOP_STATE:
            train[i] = O_STOP_STATE; break;
          
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
        case (O_STOP_STATE):  
        case (I_STOP_STATE):  
        case (STOP_STOP_STATE):  
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
        return t == O_STOP_STATE || t == I_STOP_STATE || t == STOP_STOP_STATE;
      }
    };
  }
}
