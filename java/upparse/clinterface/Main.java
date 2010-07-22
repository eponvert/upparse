package upparse.clinterface;

import java.io.*;
import java.util.*;

import upparse.corpus.*;
import upparse.eval.*;
import upparse.model.*;

/**
 * Main class manages command-line interface to UPP
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class Main {
  
  public String output = null;
  public String factor = "2,1,1";
  public int iter = -1;
  public double emdelta = 1;
  public String[] args = new String[0];
  public BIOEncoder.GPOpt grandparents = BIOEncoder.GPOpt.NOGP;
  public String evalReport = "PR";
  public String[] evalType = new String[] { "clump" };
  public final Alpha alpha = new Alpha();
  public boolean verbose = false;
  public String[] testCorpusString = null;
  public String testFileType = "wsj";
  public String[] trainCorpusString = null;
  public String trainFileType = "wsj";
  public boolean checkTerms = true;
  public String goldStandardTrain;
  public boolean onlyLast = false;
  public boolean outputAll = false;
  public int trainSents = -1;
  private StopSegmentCorpus trainStopSegmentCorpus;
  private StopSegmentCorpus testStopSegmentCorpus;
  private Eval[] evals;
  private ChunkedCorpus clumpGoldStandard;
  private UnlabeledBracketSetCorpus goldUnlabeledBracketSet;
  private ChunkedCorpus npsGoldStandard;
  private List<ChunkedSegmentedCorpus>  writeOutput = 
    new ArrayList<ChunkedSegmentedCorpus>();
  private String outputType = "clumps";

  private Main(String[] args) throws BadCLArgsException, IOException {

    int i = 0;
    String arg;

    try {
      List<String> otherArgs = new ArrayList<String>();

      while (i < args.length) {
        arg = args[i++];

        if (arg.equals("-output")) 
          output = args[i++];
        
        if (arg.equals("-numtrain"))
          trainSents = Integer.parseInt(args[i++]);
        
        else if (arg.equals("-dontCheckTerms"))
          checkTerms = false;
        
        else if (arg.equals("-test")) {
          List<String> sb = new ArrayList<String>();
          while (i < args.length && args[i].charAt(0) != '-') sb.add(args[i++]); 
          testCorpusString = sb.toArray(new String[0]);
        }
        
        else if (arg.equals("-train")) {
          List<String> sb = new ArrayList<String>();
          while (i < args.length && args[i].charAt(0) != '-') sb.add(args[i++]); 
          trainCorpusString = sb.toArray(new String[0]);
        }
        
        else if (args.equals("-testFileType"))
          testFileType = args[i++];
        
        else if (arg.equals("-trainFileType"))
          trainFileType = args[i++];

        else if (arg.equals("-factor") || arg.equals("-F")) 
          factor = args[i++];

        else if (arg.equals("-iterations")) 
          iter = Integer.parseInt(args[i++]);

        else if (arg.equals("-emdelta")) 
          emdelta = Float.parseFloat(args[i++]);

        else if (arg.equals("-G") || arg.equals("-grandparents"))
          grandparents = BIOEncoder.GPOpt.GP;
        
        else if (arg.equals("-GG") || arg.equals("-grandparentsN"))
          grandparents = BIOEncoder.GPOpt.NOSTOP;

        else if (arg.equals("-E") || arg.equals("-evalreport"))
          evalReport = args[i++];
        
        else if (arg.equals("-evaltypes"))
          evalType = args[i++].split(",");

        else if (arg.equals("-v") || arg.equals("-verbose")) 
          verbose = true;
        
        else if (arg.equals("-onlyLast"))
          onlyLast = true;
        
        else if (arg.equals("-outputAll"))
          outputAll = true;

        else
          otherArgs.add(arg);
      }

      this.args = otherArgs.toArray(new String[0]);

      // don't run EM more than 200 iterations
      if (iter < 0) iter = 200;
    }
    catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace(System.err);
      throw new BadCLArgsException();
    }
  }

  public double[] getFactor() {
    String[] fpieces = factor.split(",");

    double[] f = new double[fpieces.length];
    for (int i = 0; i < fpieces.length; i++) 
      f[i] = Double.parseDouble(fpieces[i]);

    return f;
  }

  /**
   * Print program usage to stream
   * @param stream
   */
  protected static void printUsage(PrintStream stream) {
    String prog = Main.class.getName();
    stream.println(
        "Usage: java " + prog + " action [options] [args]\n" +
        "\n" +
        "Actions:\n" +
        "  stage1-chunk\n" +
        "  hmm1-chunk\n" +
        "  prlg1-chunk\n" +  
        "  hmm2-chunk\n" +
        "  prlg2-chunk\n" +  
        "\n" +
        "Options:\n" +
        "  -train FILES        Train using specified files\n" +
        "  -test FILES         Evaluated on specified files\n" +
        "  -trainFileType X    Train files file type (eg wsj)\n" +
        "  -testFileType X     Test files file type (eg wsj)\n" +
        "  -output FILE        Set output file/template\n" +
        "  -outputAll          Produce model output for all EM iterations\n"+
        "  -F|-factor N1,N2... Factors for Stage 1 chunking\n" +
        "  -G|-grandparents    Use pseudo 2nd order tagset\n" +
        "  -GG|-grandparentsN  Use pseudo 2nd order tagset without altering STOP tag\n" +
        "  -E|-evaltype EVAL   Evaluation type (eg PRL)\n" +
        "  -iterations N       Iterations of EM\n" +
        "  -emdelta D          Halt EM when data perplexity change is less than\n" +
        "  -dontCheckTerms     Don't check that the eval and output terms are equal\n" +
        "  -onlyLast           Only show evaluation of last itertation of EM\n" +
        "\n" +
        "File types:\n" +
        "  wsj    : WSJ/Penn Treebank corpus\n" +
        "  negra  : Negra Treebank (Penn Treebank like)\n" +
        "  ctb    : Penn Chinese Treebank corpus\n" + 
        "  spl    : Sentence per line\n" +
        "  wpl    : Word per line (sentences seperated by blank lines)\n" +
        "\n" + 
        ChunkingEval.evalTypesHelp()
    );
  }
  
  private StopSegmentCorpus getStopSegmentCorpus(
      final String[] corpusStr, final String fileType) 
  throws BadCLArgsException {
    
    if (fileType.equals("wsj"))
      return CorpusUtil.wsjStopSegmentCorpus(alpha, corpusStr);
    
    else if (fileType.equals("negra"))
      return CorpusUtil.negraStopSegmentCorpus(alpha, corpusStr);
    
    else if (fileType.equals("ctb"))
      return CorpusUtil.ctbStopSegmentCorpus(alpha, corpusStr);
    
    else if (fileType.equals("spl"))
      return CorpusUtil.splStopSegmentCorpus(alpha, corpusStr);
    
    else if (fileType.equals("wpl"))
      return CorpusUtil.wplStopSegmentCorpus(alpha, corpusStr);
    
    else
      throw new BadCLArgsException("Unexpected file-type: " + fileType);
  }
  
  private StopSegmentCorpus getTrainStopSegmentCorpus() 
  throws BadCLArgsException {
    if (trainStopSegmentCorpus == null)
      trainStopSegmentCorpus = 
        getStopSegmentCorpus(trainCorpusString, trainFileType);
    return trainStopSegmentCorpus;
  }

  private StopSegmentCorpus getTestStopSegmentCorpus() 
  throws BadCLArgsException {
    if (testStopSegmentCorpus == null)
      testStopSegmentCorpus = 
        getStopSegmentCorpus(testCorpusString, testFileType);
    return testStopSegmentCorpus;
  }

  protected Eval[] getEvals() throws IOException, BadCLArgsException {
    if (evals == null) {
      evals = new Eval[evalType.length];
      int i = 0;
      for (String etype: evalType)
        if (etype.equals("clump"))
          evals[i++] = 
            ChunkingEval.fromChunkedCorpus("Clumps", getClumpGoldStandard()); 
      
        else if (etype.equals("nps"))
          evals[i++] = 
            ChunkingEval.fromChunkedCorpus("NPs", getNPsGoldStandard());
      
        else if (etype.equals("treebank-prec"))
          evals[i++] = 
            TreebankPrecisionEval.fromUnlabeledBracketSets(
                getGoldUnlabeledBracketSets());
      
        else if (etype.equals("treebank-flat"))
          evals[i++] = 
            TreebankFlatEval.fromUnlabeledBracketSets(
                getGoldUnlabeledBracketSets());
      
        else if (etype.equals("treebank-rb"))
          evals[i++] =
            RBConversionTreebankEval.fromUnlabeledBracketSets(
                getGoldUnlabeledBracketSets());
      
        else
          throw new BadCLArgsException("Unexpected eval type: " + etype);

    }

    return evals;
  }

  private UnlabeledBracketSetCorpus getGoldUnlabeledBracketSets(
      final String[] corpusFiles, final String fileType) 
  throws BadCLArgsException { 
    if (fileType.equals("wsj"))
      return CorpusUtil.wsjUnlabeledBracketSetCorpus(alpha, corpusFiles);
    
    else if (fileType.equals("negra"))
      return CorpusUtil.negraUnlabeledBrackSetCorpus(alpha, corpusFiles);
    
    else if (fileType.equals("ctb"))
      return CorpusUtil.ctbUnlabeledBracketSetCorpus(alpha, corpusFiles);
    
    else
      throw new BadCLArgsException(
          "Unexpected file type for unlabeled bracket sets: " + fileType);
  }
  
  private UnlabeledBracketSetCorpus getGoldUnlabeledBracketSets(
      final String[] corpusFiles, final String fileType, final int n) 
  throws BadCLArgsException { 
    return getGoldUnlabeledBracketSets(corpusFiles, fileType)
    .filterBySentenceLength(n);
  }
  
  private UnlabeledBracketSetCorpus getGoldUnlabeledBracketSets() 
  throws BadCLArgsException {
    if (goldUnlabeledBracketSet == null) 
      if (testCorpusString == null)
        goldUnlabeledBracketSet = 
          getGoldUnlabeledBracketSets(trainCorpusString, trainFileType);
  
      else if (isSubsetExperiment()) 
        goldUnlabeledBracketSet = 
          getGoldUnlabeledBracketSets(
              trainCorpusString, trainFileType, getSubsetN());
            
    return goldUnlabeledBracketSet;
  }
  
  private ChunkedCorpus getNPsGoldStandard(
      final String[] corpusFiles, final String fileType, final int n) 
  throws BadCLArgsException {
    return getNPsGoldStandard(corpusFiles, fileType).filterBySentenceLength(n);
  }
  
  private ChunkedCorpus getNPsGoldStandard(
      final String[] corpusFiles, final String fileType) 
  throws BadCLArgsException {
    if (fileType.equals("wsj"))
      return CorpusUtil.wsjNPsGoldStandard(alpha, corpusFiles);
    
    else if (fileType.equals("negra"))
      return CorpusUtil.negraNPsGoldStandard(alpha, corpusFiles);
    
    else if (fileType.equals("ctb"))
      return CorpusUtil.ctbNPsGoldStandard(alpha, corpusFiles);
    
    else
      throw new BadCLArgsException(
          "Unexpected file type for NPs gold standard: " + fileType);
  }

  private ChunkedCorpus getNPsGoldStandard() throws BadCLArgsException {
    if (npsGoldStandard == null)
      if (testCorpusString == null)
        npsGoldStandard = getNPsGoldStandard(trainCorpusString, trainFileType);
    
      else if (isSubsetExperiment())
        npsGoldStandard = getNPsGoldStandard(trainCorpusString, trainFileType, getSubsetN());
    
      else
        npsGoldStandard = getNPsGoldStandard(testCorpusString, testFileType);
    
    return npsGoldStandard;
  }
  
  private ChunkedCorpus getClumpGoldStandard(
      final String[] corpusFiles, final String fileType) 
  throws BadCLArgsException {
    
    if (fileType.equals("wsj")) 
      return CorpusUtil.wsjClumpGoldStandard(alpha, corpusFiles);
    
    else if (fileType.equals("negra"))
      return CorpusUtil.negraClumpGoldStandard(alpha, corpusFiles);
    
    else if (fileType.equals("ctb"))
      return CorpusUtil.ctbClumpGoldStandard(alpha, corpusFiles);
    
    else 
      throw new BadCLArgsException(
          "Unexpected file type for clumping gold standard: " + fileType);
      
  }

  private ChunkedCorpus getClumpGoldStandard(
      final String[] corpusFiles, final String fileType, final int n) 
  throws BadCLArgsException {
    return 
    getClumpGoldStandard(corpusFiles, fileType).filterBySentenceLength(n);
  }

  private ChunkedCorpus getClumpGoldStandard() throws BadCLArgsException {
    if (clumpGoldStandard == null) 
      if (testCorpusString == null) 
        clumpGoldStandard = 
          getClumpGoldStandard(trainCorpusString, trainFileType);
    
      else if (isSubsetExperiment())
        clumpGoldStandard =
          getClumpGoldStandard(args, testFileType, getSubsetN());
    
      else 
        clumpGoldStandard = 
          getClumpGoldStandard(testCorpusString, testFileType);
    return clumpGoldStandard;
  }

  protected SimpleChunker getSimpleChunker() throws BadCLArgsException {
    return SimpleChunker.fromStopSegmentCorpus(
        alpha,
        getTrainStopSegmentCorpus(),
        getFactor());
  }
  

  private BIOEncoder getBIOEncoder() throws EncoderError {
    return 
    BIOEncoder.getBIOEncoder(grandparents, KeepStop.STOP, alpha); 
  }

  protected SequenceModelChunker getHMMModelChunker() 
  throws BadCLArgsException, EncoderError {
    final SimpleChunker c = getSimpleChunker();
    final StopSegmentCorpus trainCorpus = getTestStopSegmentCorpus();
    final ChunkedSegmentedCorpus outputCorpus = c.getChunkedCorpus(trainCorpus);
    final BIOEncoder enco = getBIOEncoder();
    final SequenceModel hmm = HMM.mleEstimate(outputCorpus, enco);
    return new SequenceModelChunker(hmm, emdelta);

    /*
    
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
    */
  }

  protected SequenceModelChunker getPRLGModelChunker() 
  throws BadCLArgsException, EncoderError {
    final SimpleChunker c = getSimpleChunker();
    final StopSegmentCorpus trainCorpus = getTestStopSegmentCorpus();
    final ChunkedSegmentedCorpus outputCorpus = c.getChunkedCorpus(trainCorpus);
    final BIOEncoder enco = getBIOEncoder();
    final SequenceModel prlg = RRG.mleEstimate(outputCorpus, enco);
    return new SequenceModelChunker(prlg, emdelta);
  }

  private boolean isSubsetExperiment() {
    return testCorpusString.length == 1 && 
    testCorpusString[0].startsWith("subset"); 
  }
  
  private StopSegmentCorpus getSubsetStopSegmentCorpus() 
  throws BadCLArgsException {
    final int num = Integer.parseInt(testCorpusString[0].substring(6));
    return getTrainStopSegmentCorpus().filterLen(num);
  }
  
  private int getSubsetN() {
    assert trainCorpusString.length == 1;
    assert trainCorpusString[0].startsWith("subset");
    return Integer.parseInt(trainCorpusString[0].substring(6));
  }
  
  private StopSegmentCorpus getEvalCorpus() throws BadCLArgsException {
    if (testCorpusString == null)
      return getTrainStopSegmentCorpus();
    
    else if (isSubsetExperiment()) 
      return getSubsetStopSegmentCorpus();
    
    else 
      return getTestStopSegmentCorpus();
  }

  protected void eval(final String comment, final Chunker chunker) 
  throws BadCLArgsException, IOException, EvalError, ChunkerError {
    
    if (evalType == null || 
        (evalType.length == 1 && evalType[0].equals("none"))) return;
    
    final ChunkedSegmentedCorpus chunkerOutput =  
      chunker.getChunkedCorpus(getEvalCorpus());
    
    for (Eval eval: getEvals()) 
      eval.eval(comment, chunkerOutput);
    
    if (output != null)
      if (outputAll || writeOutput.isEmpty())
        writeOutput.add(chunkerOutput);
    
      else 
        writeOutput.set(0, chunkerOutput);
      
  }

  public void writeEval(PrintStream out) 
  throws IOException, BadCLArgsException, EvalError {
    for (Eval eval: getEvals()) {
      eval.writeSummary(evalReport, out, onlyLast);
      out.println();
    }
    
    if (output != null) {
      if (writeOutput.size() == 1)
        writeOutput.get(0).writeTo(output, outputType);
      else
        for (int i = 0; i < writeOutput.size(); i++) {
          final String outputIter = output + "-" + Integer.toString(i);
          writeOutput.get(i).writeTo(outputIter, outputType);
        }
    }
  }

  public PrintStream getVerbosePrintStream() {
    if (verbose)
      return System.err;
    else
      return null;
  }
  
  private static void usageError() {
    printUsage(System.err);
    System.exit(1);
  }

  // TODO
  /*
  private static BIOEncoder getBIOEncoder(Main clargs, Alpha alpha) 
  throws EncoderError {
    return 
    BIOEncoder.getBIOEncoder(clargs.grandparents, clargs.stopv, alpha); 
  }
  */
  
  /** Execute and evaluate simple chunking model 
   * @throws BadCLArgsException 
   * @throws EvalError 
   * @throws IOException 
   * @throws ChunkerError */ 
  private static void stage1Chunk(Main clargs) 
  throws BadCLArgsException, IOException, EvalError, ChunkerError {
    Chunker chunker = clargs.getSimpleChunker();
    clargs.eval("stage-1", chunker);
    clargs.writeEval(System.out);
  }

  /** Execute HMM chunking model based on baseline output training
   * @param clargs Command-line arguments
   * @throws IOException If there's a problem reading the training data
   * @throws EncoderError 
   * @throws BadCLArgsException 
   * @throws ChunkerError 
   * @throws EvalError 
   */
  private static void hmm1Chunk(Main clargs) 
  throws IOException, BadCLArgsException, EncoderError, EvalError, ChunkerError {
    
    SequenceModelChunker model = clargs.getHMMModelChunker();
    clargs.eval("Stage 1", model.getCurrentChunker());
    while (model.anotherIteration()) {
      model.updateWithEM(clargs.getVerbosePrintStream());
      clargs.eval(
          String.format("Iter %d", model.getCurrentIter()),
          model.getCurrentChunker());
          
    }
  }
  
  /** Execute right-regular grammar model based on output training
   * @param clargs Command-line arguments
   * @throws EncoderError 
   * @throws BadCLArgsException 
   * @throws ChunkerError 
   * @throws EvalError 
   */
  private static void prlg1Chunk(Main clargs) 
  throws IOException, BadCLArgsException, EncoderError, EvalError, ChunkerError {

    SequenceModelChunker model = clargs.getPRLGModelChunker();
    clargs.eval("Stage 1", model.getCurrentChunker());
    while (model.anotherIteration()) {
      model.updateWithEM(clargs.getVerbosePrintStream());
      clargs.eval(
          String.format("Iter %d", model.getCurrentIter()),
          model.getCurrentChunker());
          
    }
  }
  
  private static void hmm2Chunk(Main clargs) throws IOException {
    
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
  
  private static void prlg2Chunk(String fname, Main clargs) throws IOException {
    
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
  
  public static void main(String[] argv) {
    try {
      Main clargs = new Main(argv);
      
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
      
    } catch (EncoderError e) {
      System.err.println("Encoder problem");
      e.printStackTrace(System.err);
      usageError();
    } catch (ChunkerError e) {
      System.err.println("Problem with the chunker");
      e.printStackTrace();
      usageError();
    }
  }
}
