package upparse;

import java.io.*;

/**
 * Main class manages command-line interface to UPP
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class Main {
  
  private static void usageError() {
    CLArgs.printUsage(System.err);
    System.exit(1);
  }

  /** Execute simple clumping model 
   * @param fname File name of training/eval data
   * @throws IOException If there's a problem reading the training data
   */
  private static void simpleClump(String fname, CLArgs clargs) throws IOException {
    PrintWriter output = new PrintWriter(new File(clargs.output));
    
    BasicCorpus corpus = new BasicCorpus(fname);
    int[] factor = clargs.getFactor();
    String stopv = clargs.stopv;
    SimpleClumper clumper = new SimpleClumper(corpus, factor, stopv);
    for (String s: clumper.getClumpedCorpus().strIter()) 
      output.println(s);
    
    output.close();
  }

  /** Execute HMM clumping model based on baseline output training
   * @param fname File name of training/eval data
   * @throws IOException If there's a problem reading the training data
   */
  private static void hmm1Clump(String fname, CLArgs clargs) throws IOException {
    PrintWriter output = new PrintWriter(new File(clargs.output));
    
    BasicCorpus corpus = new BasicCorpus(fname);
    int[] factor = clargs.getFactor();
    String stopv = clargs.stopv;
    SimpleClumper clumper = new SimpleClumper(corpus, factor, stopv);
    ClumpedCorpus clumpedCorpus = clumper.getClumpedCorpus();
    Alpha wrdAlpha = clumper.alpha;
    BIOEncoder encoder = 
      BIOEncoder.getBIOEncoder(clargs.grandparents, clargs.stopv, wrdAlpha);
    int[] tokens = encoder.tokensFromClumpedCorpus(clumpedCorpus);
    
    int[][] bioTrain = encoder.bioTrain(clumpedCorpus, tokens.length);
    HMM hmm = HMM.mleEstimate(tokens, bioTrain);
    int[] bioOutput = hmm.tag(tokens);
    for (String s: encoder.clumpedCorpusFromBIOOutput(tokens, hmm.tag(tokens)).strIter())
      output.println(s);
    
    output.close();
  }
  
  public static void main(String[] argv) {
    try {
      CLArgs clargs = new CLArgs(argv);
      
      String[] args = clargs.args;

      if (clargs.args.length == 0) 
        usageError();

      String action = args[0];
      
      if (action.equals("simple-clump")) { 
        if (args.length < 2) usageError();
        simpleClump(args[1], clargs);
      }
      
      else if (action.equals("hmm1-clump")) {
        if (args.length < 2) usageError();
        hmm1Clump(args[1], clargs);
      }
      
      else if (action.equals("hmm2-clump"))
        System.exit(0);
      
      else if (action.equals("rrg1-clump"))
        System.exit(0);
      
      else if (action.equals("rrg2-clump"))
        System.exit(0);
      
      else
        usageError();
      
    } catch (BadCLArgsException e) {
      usageError();
      
    } catch (IOException e) {
      System.err.println("IO problem");
      e.printStackTrace(System.err);
      usageError();
    }
    
  }
}
