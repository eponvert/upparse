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
    PrintWriter output = 
      new PrintWriter(new BufferedWriter(new FileWriter(clargs.output)));
    
    BasicCorpus corpus = new BasicCorpus(fname);
    int[] factor = clargs.getFactor();
    String stopv = clargs.stopv;
    SimpleClumper clumper = new SimpleClumper(corpus, factor, stopv);
    clumper.getClumpedCorpus().printTo(output);
    output.close();
  }

  /** Execute HMM clumping model based on baseline output training
   * @param fname File name of training/eval data
   * @throws IOException If there's a problem reading the training data
   */
  private static void hmm1Clump(String fname, CLArgs clargs) throws IOException {
    int[] factor = clargs.getFactor();
    String stopv = clargs.stopv;
    ClumpedCorpus corpus = 
      new SimpleClumper(new BasicCorpus(fname), factor, stopv).getClumpedCorpus();
    Alpha wrdAlpha = corpus.alpha;
    BIOEncoder encoder = 
      BIOEncoder.getBIOEncoder(clargs.grandparents, clargs.stopv, wrdAlpha);
    
    try {
      HMM hmm = HMM.mleEstimate(corpus, encoder);
      String outFile = clargs.output + ".iter000.txt"; 
      PrintWriter output = 
        new PrintWriter(new BufferedWriter(new FileWriter(outFile)));
      hmm.reTagTrainCC().printTo(output);
      output.close();

      int i = 0;

      // Don't run more than 200 iterations of EM
      int iter = clargs.iter < 0 ? 200 : clargs.iter;
      double lastPerplex = 0., currPerplex, lastPerplexChange = 1e10;

      String pFile = clargs.output + ".perplex.csv";
      PrintWriter perplexLog = new PrintWriter(new File(pFile));

      while (i++ < iter && lastPerplexChange <= clargs.emdelta) {
        hmm.emUpdateFromTrain();
        outFile = clargs.output + String.format(".iter%03d.txt", i+1);
        output = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));
        hmm.reTagTrainCC().printTo(output);
        output.close();
        currPerplex = hmm.currPerplex();
        perplexLog.println(String.format("%d,%f", i+1, currPerplex));
        lastPerplexChange = Math.abs(currPerplex - lastPerplex);
        lastPerplex = currPerplex;
      }

      perplexLog.close();
    } catch (HMMError e) {
      System.err.println("Problem initializing HMM: " + e.getMessage());
      usageError();
    }
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
