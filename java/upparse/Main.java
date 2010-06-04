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
  
  private static SimpleChunker getSimpleChunker(String fname, CLArgs clargs) 
  throws IOException {
    return 
    new SimpleChunker(
        new BasicCorpus(fname), clargs.getFactor(), clargs.stopv, clargs.alpha);
  }
  
  private static BIOEncoder getBIOEncoder(
      ChunkedSegmentedCorpus corpus, CLArgs clargs) {
    return 
    BIOEncoder.getBIOEncoder(clargs.grandparents, clargs.stopv, corpus.alpha); 
  }

  /** Execute simple chunking model 
   * @param fname File name of training/eval data
   * @throws IOException If there's a problem reading the training data
   */
  private static void simpleChunk(String fname, CLArgs clargs) 
  throws IOException {
    
    final ChunkedSegmentedCorpus outputCorpus;
    final SimpleChunker chunker = getSimpleChunker(fname, clargs);
    if (clargs.testCorpus != null)
      outputCorpus = chunker.getChunkedCorpus(clargs.testCorpus);
    else
      outputCorpus = chunker.getChunkedCorpus();
    
    if (clargs.output != null)
      outputCorpus.writeTo(clargs.output);
    
    try {
      for (ChunkingEval eval: clargs.getEvals()) {
        eval.eval("Baseline", outputCorpus.toChunkedCorpus());
        eval.writeSummary(clargs.evalType);
      }
    } catch (EvalError e) {
      System.err.println("Problem with evaluation");
      System.err.println(e.getMessage());
    }
  }

  /** Execute HMM chunking model based on baseline output training
   * @param fname File name of training/eval data
   * @throws IOException If there's a problem reading the training data
   */
  private static void hmm1Chunk(String fname, CLArgs clargs) 
  throws IOException {
    
    boolean writeOut = clargs.output != null;
    
    try {
      SimpleChunker chunker = getSimpleChunker(fname, clargs); 
      ChunkedSegmentedCorpus baselineCorpus = chunker.getChunkedCorpus();
      
      ChunkedSegmentedCorpus evalCorpus;
      if (clargs.testCorpus == null)
        evalCorpus = baselineCorpus;
      else
        evalCorpus = chunker.getChunkedCorpus(clargs.testCorpus); 
      
      if (writeOut)
        evalCorpus.writeTo(clargs.output + ".baseline.txt");
      
      ChunkingEval[] evals = clargs.getEvals();
      for (ChunkingEval eval: evals) 
        eval.eval("Baseline", evalCorpus.toChunkedCorpus());
      
      BIOEncoder encoder = getBIOEncoder(baselineCorpus, clargs);
      HMM hmm = HMM.mleEstimate(baselineCorpus, encoder);
      
      int[] testCorpus;
      if (clargs.testCorpus != null)
        testCorpus = encoder.tokensFromFile(clargs.testCorpus);
      else
        testCorpus = hmm.orig;
      
      evalCorpus = hmm.tagCC(testCorpus);
      
      if (writeOut)
        evalCorpus.writeTo(clargs.output + ".noem.txt");
      
      for (ChunkingEval eval: evals)
        eval.eval("No EM", 
            ChunkedCorpus.fromChunkedSegmentedCorpus(evalCorpus));

      if (clargs.iter != 0) {
        double lastPerplex = 0., currPerplex, lastPerplexChange = 1e10;
        
        final CSVFileWriter perplexLog = 
          writeOut ?  
              new CSVFileWriter(clargs.output + ".perplex.csv") :
              null;

        for (int i = 0;
             i < clargs.iter && lastPerplexChange > clargs.emdelta;
             i++) {
          
          hmm.emUpdateFromTrain();
          currPerplex = hmm.currPerplex();
          if (clargs.verbose)
            System.out.println(String.format(
                "Iteration %d: Perplexity = %f", i+1, currPerplex));
          if (writeOut)
            perplexLog.write(i+1, currPerplex);
          lastPerplexChange = Math.abs(currPerplex - lastPerplex);
          lastPerplex = currPerplex;

          evalCorpus = hmm.tagCC(testCorpus);
          
          if (writeOut)
            evalCorpus.writeTo(
                String.format("%s.iter%03d.txt", clargs.output, i+1));

          for (ChunkingEval eval: evals)
            eval.eval(String.format("Iter %03d", i+1), 
                ChunkedCorpus.fromChunkedSegmentedCorpus(evalCorpus));
        }
        
        if (writeOut) perplexLog.close();
      }
      
      for (ChunkingEval eval: evals) 
        eval.writeSummary(clargs.evalType);

    } catch (HMMError e) {
      System.err.println("Problem initializing HMM: " + e.getMessage());
      usageError();
      
    } catch (EvalError e) {
      System.err.println("Problem with eval: " + e.getMessage());
      System.exit(1);
    }
  }
  
  public static void main(String[] argv) {
    try {
      CLArgs clargs = new CLArgs(argv);
      
      String[] args = clargs.args;

      if (clargs.args.length == 0)  {
        System.err.println("Please specify an action\n");
        usageError();
      }

      String action = args[0];
      
      if (action.equals("simple-chunk")) { 
        if (args.length < 2) {
          System.err.println("Training file required\n");
          usageError();
        }
        simpleChunk(args[1], clargs);
      }
      
      else if (action.equals("hmm1-chunk")) {
        if (args.length < 2) {
          System.err.println("Training file required");
          usageError();
        }
        hmm1Chunk(args[1], clargs);
      }
      
      else if (action.equals("hmm2-chunk"))
        System.exit(0);
      
      else if (action.equals("rrg1-chunk"))
        System.exit(0);
      
      else if (action.equals("rrg2-chunk"))
        System.exit(0);
      
      else {
        System.err.println("Unexpected action: " + action);
        usageError();
      }
      
    } catch (BadCLArgsException e) {
      System.err.println("Bad command line error: " + e.getMessage());
      usageError();
      
    } catch (IOException e) {
      System.err.println("IO problem");
      e.printStackTrace(System.err);
      usageError();
    }
  }
}
