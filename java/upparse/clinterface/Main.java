package upparse.clinterface;

import java.io.*;

import upparse.eval.*;
import upparse.model.*;

/**
 * Main class manages command-line interface to UPP
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class Main {
  
  private static void usageError() {
    CLArgs.printUsage(System.err);
    System.exit(1);
  }

  // TODO
  /*
  private static BIOEncoder getBIOEncoder(CLArgs clargs, Alpha alpha) 
  throws EncoderError {
    return 
    BIOEncoder.getBIOEncoder(clargs.grandparents, clargs.stopv, alpha); 
  }
  */

  /** Execute and evaluate simple chunking model 
   * @throws IOException If there's a problem reading the training data
   * @throws EvalError 
   */
  private static void stage1Chunk(CLArgs clargs) 
  throws IOException, EvalError {
    try {
      Chunker chunker = clargs.getSimpleChunker();
      clargs.eval("stage-1", chunker);
    } catch (BadCLArgsException e) {
      e.printStackTrace(System.err);
      System.exit(1);
    }
    
    // TODO
    /*
    final ChunkedSegmentedCorpus outputCorpus;
    final SimpleChunker chunker = getSimpleChunker(fname, clargs);
    if (clargs.testCorpusString != null)
      outputCorpus = chunker.getChunkedCorpus(clargs.testCorpusString, -1);
    else
      outputCorpus = chunker.getChunkedCorpus();
    
    if (clargs.output != null)
      outputCorpus.writeTo(clargs.output);
    
    try {
      for (ChunkingEval eval: clargs.getEvals()) {
        eval.eval("Baseline", outputCorpus.toChunkedCorpus());
        eval.writeSummary(clargs.evalType, clargs.onlyLast);
      }
    } catch (EvalError e) {
      System.err.println("Problem with evaluation");
      System.err.println(e.getMessage());
    }
    */
  }

  /** Execute right-regular grammar model based on output training
   * @param clargs Command-line arguments
   */
  private static void prlg1Chunk(CLArgs clargs) 
  throws IOException {

    // TODO
    /*
    boolean writeOut = clargs.output != null;
    
    try {
      SimpleChunker chunker = getSimpleChunker(fname, clargs); 
      ChunkedSegmentedCorpus baselineCorpus = chunker.getChunkedCorpus();
      
      ChunkedSegmentedCorpus evalCorpus;
      if (clargs.testCorpusString == null)
        evalCorpus = baselineCorpus;
      else
        evalCorpus = chunker.getChunkedCorpus(clargs.testCorpusString, -1); 
      
      if (writeOut)
        evalCorpus.writeTo(clargs.output + ".baseline.txt");
      
      ChunkingEval[] evals = clargs.getEvals();
      for (ChunkingEval eval: evals) 
        eval.eval("Baseline", evalCorpus.toChunkedCorpus());
      
      BIOEncoder encoder = getBIOEncoder(clargs, baselineCorpus.alpha);
      RRG rrg = RRG.mleEstimate(baselineCorpus, encoder);
      
      int[] testCorpus;
      if (clargs.testCorpusString != null)
        testCorpus = encoder.tokensFromFile(clargs.testCorpusString, -1);
      else
        testCorpus = rrg.getOrig();
      
      evalCorpus = rrg.tagCC(testCorpus);
      
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
          
          rrg.emUpdateFromTrain();
          currPerplex = rrg.currPerplex();
          if (clargs.verbose)
            System.out.println(String.format(
                "Iteration %d: Perplexity = %f", i+1, currPerplex));
          if (writeOut)
            perplexLog.write(i+1, currPerplex);
          lastPerplexChange = Math.abs(currPerplex - lastPerplex);
          lastPerplex = currPerplex;

          evalCorpus = rrg.tagCC(testCorpus);
          
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
        eval.writeSummary(clargs.evalType, clargs.onlyLast);

    } catch (EvalError e) {
      System.err.println("Problem with eval: " + e.getMessage());
      System.exit(1);

    } catch (SequenceModelError e) {
      System.err.println("Problem initializing RRG: " + e.getMessage());
      usageError();
      
    } catch (EncoderError e) {
      System.err.println("Problem with BIO encoding: " + e.getMessage());
      usageError();
    }
    */
  }
  
  private static void hmm2Chunk(CLArgs clargs) throws IOException {
    
    // TODO
    /*
    boolean writeOut = clargs.output != null;
    
    try {

      final BIOEncoder encoder = getBIOEncoder(clargs, clargs.alpha);
      final StopSegmentCorpus corpus = 
        StopSegmentCorpus.fromFile(
            fname, clargs.alpha, clargs.alpha.getCode(clargs.stopv), 
            clargs.trainSents);
      final int[] tokens = encoder.tokensFromStopSegmentCorpus(corpus);
      final HMM hmm = 
        HMM.fromCounts(encoder.softCounts(tokens), encoder, tokens);
      
      ChunkingEval[] evals = clargs.getEvals();
      int[] testCorpus;
      if (clargs.testCorpusString != null)
        testCorpus = encoder.tokensFromFile(clargs.testCorpusString, -1);
      else
        testCorpus = hmm.getOrig();

      ChunkedSegmentedCorpus evalCorpus = hmm.tagCC(testCorpus);

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
        eval.writeSummary(clargs.evalType, clargs.onlyLast);

    } catch (SequenceModelError e) {
      System.err.println("Problem initializing HMM: " + e.getMessage());
      usageError();
      
    } catch (EvalError e) {
      System.err.println("Problem with eval: " + e.getMessage());
      System.exit(1);
      
    } catch (EncoderError e) {
      System.err.println("Problem with BIO encoding: " + e.getMessage());
      usageError();
    }
    */
  }
  
  private static void prlg2Chunk(String fname, CLArgs clargs) throws IOException {
    
    //TODO
    /*
    boolean writeOut = clargs.output != null;
    
    try {

      final BIOEncoder encoder = getBIOEncoder(clargs, clargs.alpha);
      final StopSegmentCorpus corpus = 
        StopSegmentCorpus.fromFile(
            fname, clargs.alpha, clargs.alpha.getCode(clargs.stopv), 
            clargs.trainSents);
      final int[] tokens = encoder.tokensFromStopSegmentCorpus(corpus);
      final RRG hmm = 
        RRG.fromCounts(encoder.softCounts(tokens), encoder, tokens);
      
      ChunkingEval[] evals = clargs.getEvals();
      int[] testCorpus;
      if (clargs.testCorpusString != null)
        testCorpus = encoder.tokensFromFile(clargs.testCorpusString, -1);
      else
        testCorpus = hmm.getOrig();

      ChunkedSegmentedCorpus evalCorpus = hmm.tagCC(testCorpus);

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
        eval.writeSummary(clargs.evalType, clargs.onlyLast);

    } catch (SequenceModelError e) {
      System.err.println("Problem initializing HMM: " + e.getMessage());
      usageError();
      
    } catch (EvalError e) {
      System.err.println("Problem with eval: " + e.getMessage());
      System.exit(1);
      
    } catch (EncoderError e) {
      System.err.println("Problem with BIO encoding: " + e.getMessage());
      usageError();
    }
    */
  }
  
  /** Execute HMM chunking model based on baseline output training
   * @param clargs Command-line arguments
   * @throws IOException If there's a problem reading the training data
   */
  private static void hmm1Chunk(CLArgs clargs) 
  throws IOException {
    
    // TODO
    /*
    boolean writeOut = clargs.output != null;
    
    try {
      ChunkedSegmentedCorpus trainCorpus;
      ChunkedSegmentedCorpus evalCorpus;
      ChunkingEval[] evals = clargs.getEvals();
      
      if (clargs.goldStandardTrain == null) {
        SimpleChunker chunker = getSimpleChunker(fname, clargs); 
        trainCorpus = chunker.getChunkedCorpus();
        if (clargs.testCorpusString == null)
          evalCorpus = trainCorpus;
        else
          evalCorpus = 
            chunker.getChunkedCorpus(clargs.testCorpusString, -1); 
        
        if (writeOut)
          evalCorpus.writeTo(clargs.output + ".baseline.txt");
        
        for (ChunkingEval eval: evals) 
          eval.eval("Baseline", evalCorpus.toChunkedCorpus());
        
      } else {
        trainCorpus = 
          ChunkedSegmentedCorpus.fromFiles(
              fname, clargs.goldStandardTrain, clargs.stopv, clargs.trainSents);
      }
      
      BIOEncoder encoder = getBIOEncoder(clargs, trainCorpus.alpha);
      HMM hmm = HMM.mleEstimate(trainCorpus, encoder);
      
      int[] testCorpus;
      if (clargs.testCorpusString != null)
        testCorpus = encoder.tokensFromFile(clargs.testCorpusString, -1);
      else
        testCorpus = hmm.getOrig();
      
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
        eval.writeSummary(clargs.evalType, clargs.onlyLast);

    } catch (SequenceModelError e) {
      System.err.println("Problem initializing HMM: " + e.getMessage());
      usageError();
      
    } catch (EvalError e) {
      System.err.println("Problem with eval: " + e.getMessage());
      System.exit(1);
      
    } catch (EncoderError e) {
      System.err.println("Problem with BIO encoding: " + e.getMessage());
      usageError();
    }
    */
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
      
      if (action.equals("stage1-chunk"))  
        stage1Chunk(clargs);
      
      else if (action.equals("hmm1-chunk")) 
        hmm1Chunk(clargs);
      
      else if (action.equals("hmm2-chunk")) 
        hmm2Chunk(clargs);
      
      else if (action.equals("prlg1-chunk")) 
        prlg1Chunk(clargs);
      
      else if (action.equals("prlg2-chunk")) 
        prlg2Chunk(args[1], clargs);
      
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
      
    } catch (EvalError e) {
      System.err.println("Eval problem");
      e.printStackTrace(System.err);
      usageError();
    }
  }
}
