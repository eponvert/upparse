package upparse;

/**
 * Utility to encode clumped corpus as a BIO encoded training set with 
 * "grandparent" tags, for emulating a second-order HMM
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class GrandparentBIOEncoder extends BIOEncoder {

  public GrandparentBIOEncoder(String stop, Alpha alpha) {
    super(stop, alpha);
  }

  @Override
  public int[][] bioTrain(ClumpedCorpus corpus, int n) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ClumpedCorpus clumpedCorpusFromBIOOutput(int[] tokens, int[] output) {
    // TODO Auto-generated method stub
    return null;
  }

}
