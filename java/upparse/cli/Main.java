package upparse.cli;

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

  private static enum ChunkerType { PRLG, HMM; } 
  private static enum ChunkingStrategy { TWOSTAGE, SOFT; }
  
  private final Alpha alpha = new Alpha();
  private OutputManager outputManager = OutputManager.nullOutputManager();
  private EvalManager evalManager = new EvalManager(alpha);
  private String factor = "2,1,1";
  private int iter = -1;
  private double emdelta = .001;
  private String[] args = new String[0];
  private BIOEncoder.EncoderType encoderType = BIOEncoder.EncoderType.BIO;
  // private EvalReportType evalReport = EvalReportType.PR;
  // private OutputType[] evalType = new OutputType[] { OutputType.CLUMP };
  private boolean verbose = false;
  private CorpusType testFileType = CorpusType.WSJ;
  private String[] trainCorpusString = null;
  private CorpusType trainFileType = CorpusType.WSJ;
  private boolean checkTerms = true;
  private boolean onlyLast = false;
  private int trainSents = -1;
  private StopSegmentCorpus trainStopSegmentCorpus;
  private StopSegmentCorpus testStopSegmentCorpus;
  // private Eval[] evals;
  // private ChunkedCorpus clumpGoldStandard;
  // private UnlabeledBracketSetCorpus goldUnlabeledBracketSet;
  // private ChunkedCorpus npsGoldStandard;
  // private List<ChunkedSegmentedCorpus>  writeOutput = 
    // new ArrayList<ChunkedSegmentedCorpus>();
  // private OutputType outputType = OutputType.CLUMP;
  private double prlgSmooth = 0.1;
  private ChunkerType chunkerType = ChunkerType.PRLG;
  private ChunkingStrategy chunkingStrategy = ChunkingStrategy.TWOSTAGE;
  private int filterTrain = -1;
  private int filterTest = -1;
  private final String action;

  private Main(String[] args) throws CommandLineError, IOException, EvalError {
    int i = 0;
    String arg;
    
    boolean outputAll = false;
    OutputType outputType = OutputType.CLUMP;
    String eval = "";
    String[] testCorpusString = new String[0];
    
    if (args.length < 1)
      throw new CommandLineError("Please specify an action");

    try {
      List<String> otherArgs = new ArrayList<String>();

      while (i < args.length) {
        arg = args[i++];

        if (arg.equals("-output")) 
          outputManager = OutputManager.fromDirname(args[i++]);
        
        else if (arg.equals("-filterTrain"))
          filterTrain = Integer.parseInt(args[i++]);
        
        else if (arg.equals("-filterTest"))
          filterTest = Integer.parseInt(args[i++]);
        
        else if (arg.equals("-chunkingStrategy"))
          chunkingStrategy = ChunkingStrategy.valueOf(args[i++]);
        
        else if (arg.equals("-chunkerType"))
          chunkerType = ChunkerType.valueOf(args[i++]);
        
        else if (arg.equals("-prlgSmooth"))
          prlgSmooth = Double.parseDouble(args[i++]);
        
        else if (arg.equals("-outputType"))
          outputType = OutputType.valueOf(args[i++]);
        
        else if (arg.equals("-numtrain"))
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
          testFileType = CorpusType.valueOf(args[i++]);
        
        else if (arg.equals("-trainFileType"))
          trainFileType = CorpusType.valueOf(args[i++]);

        else if (arg.equals("-factor") || arg.equals("-F")) 
          factor = args[i++];

        else if (arg.equals("-iterations")) 
          iter = Integer.parseInt(args[i++]);

        else if (arg.equals("-emdelta")) 
          emdelta = Float.parseFloat(args[i++]);

        else if (arg.equals("-G") || arg.equals("-encoderType"))
          encoderType = BIOEncoder.EncoderType.valueOf(args[i++]);
        
        else if (arg.equals("-E") || arg.equals("-evalReportType"))
          evalManager.setEvalReportType(EvalReportType.valueOf(args[i++]));
        
        else if (arg.equals("-e") || arg.equals("-evalTypes"))
          eval = args[i++];

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
      this.action = this.args[0];
      
      // Setup outputManager
      outputManager.setOutputAllIter(outputAll);
      outputManager.setOutputType(outputType);
      
      // Setup evalManager
      evalManager.setParserEvaluationTypes(eval);
      if (testCorpusString.length == 1 && 
          testCorpusString[0].startsWith("subset")) {
        int len = Integer.parseInt(testCorpusString[0].substring(6)); 
        evalManager.setFilterLen(len);
        evalManager.setTestCorpusString(trainCorpusString);
      }
      else evalManager.setTestCorpusString(testCorpusString);
      
      // don't run EM more than 200 iterations
      if (iter < 0) iter = 200;
    }
    catch (ArrayIndexOutOfBoundsException e) {
      e.printStackTrace(System.err);
      throw new CommandLineError();
    }
  }

  private double[] getFactor() {
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
  private static void printUsage(PrintStream stream) {
    String prog = Main.class.getName();
    stream.println(
        "Usage: java " + prog + " action [options] [args]\n" +
        "\n" +
        "Actions:\n" +
        "  chunk\n" +
        "\n" +
        "Options:\n" +
        "  -train FILES        Train using specified files\n" +
        "  -test FILES         Evaluated on specified files\n" +
        "  -trainFileType X    Train files file type (eg wsj)\n" +
        "  -testFileType X     Test files file type (eg wsj)\n" +
        "  -output FILE        Set output file/template\n" +
        "  -outputType T       Output type (see eval types)\n" +
        "  -outputAll          Produce model output for all EM iterations\n"+
        "  -F|-factor N1,N2... Factors for Stage 1 chunking\n" +
        "  -G|-encoderType T   Use chunk-encoder type T\n" +
        "  -GG|-grandparentsN  Use pseudo 2nd order tagset without altering STOP tag\n" +
        "  -E|-evalreort EVAL  Evaluation report (eg PRL)\n" +
        "  -e|-evaltypes E1,E2 Evaluation types \n" +
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
        evalTypesHelp() + 
        "\n\n" +
        Eval.evalReportHelp() +
        "\n\n" +
        BIOEncoder.EncoderType.encoderTypeHelp()
    );
  }
  
  private StopSegmentCorpus getStopSegmentCorpus(
      final String[] corpusStr, final CorpusType fileType, final int numSent) 
  throws CorpusError {
    return CorpusUtil.stopSegmentCorpus(alpha, corpusStr, fileType, numSent);
  }
    
  
  private StopSegmentCorpus getTrainStopSegmentCorpus() throws CorpusError {
    if (trainStopSegmentCorpus == null) { 
      trainStopSegmentCorpus =
        CorpusUtil.stopSegmentCorpus(alpha, trainCorpusString, testFileType, trainSents);
      if (filterTrain > 0)
        trainStopSegmentCorpus = trainStopSegmentCorpus.filterLen(filterTrain);
    }
    return trainStopSegmentCorpus;
  }

  private static String evalTypesHelp() {
    return 
    "Evaluation types:\n" +
    "  clump\n" + 
    "  nps\n"+
    "  treebank-prec\n"+
    "  treebank-flat\n"+
    "  treebank-rb";
  }

  private SimpleChunker getSimpleChunker() throws CorpusError {
    return SimpleChunker.fromStopSegmentCorpus(
        alpha,
        getTrainStopSegmentCorpus(),
        getFactor());
  }
  

  private BIOEncoder getBIOEncoder() throws EncoderError {
    return 
    BIOEncoder.getBIOEncoder(encoderType, KeepStop.STOP, alpha); 
  }

  private SequenceModelChunker getHMMModelChunker() 
  throws CorpusError, EncoderError {
    final SimpleChunker c = getSimpleChunker();
    final StopSegmentCorpus trainCorpus = getTrainStopSegmentCorpus();
    final ChunkedSegmentedCorpus outputCorpus = c.getChunkedCorpus(trainCorpus);
    final BIOEncoder enco = getBIOEncoder();
    final SequenceModel hmm = HMM.mleEstimate(outputCorpus, enco);
    return new SequenceModelChunker(hmm, emdelta);
  }

  private SequenceModelChunker getPRLGModelChunker() 
  throws CorpusError, EncoderError {
    final SimpleChunker c = getSimpleChunker();
    final StopSegmentCorpus trainCorpus = getTrainStopSegmentCorpus();
    final ChunkedSegmentedCorpus outputCorpus = c.getChunkedCorpus(trainCorpus);
    final BIOEncoder enco = getBIOEncoder();
    final SequenceModel prlg = RRG.mleEstimate(outputCorpus, enco, prlgSmooth);
    return new SequenceModelChunker(prlg, emdelta);
  }
  
  private SequenceModelChunker getHMMSoftModelChunker() 
  throws CorpusError, EncoderError {
    return new SequenceModelChunker(
        HMM.softCountEstimate(getTrainStopSegmentCorpus(), getBIOEncoder()), 
        emdelta);
  }

  private SequenceModelChunker getPRLGSoftModelChunker() 
  throws CorpusError, EncoderError {
    return new SequenceModelChunker(
        RRG.softCountEstimate(getTrainStopSegmentCorpus(), getBIOEncoder(), prlgSmooth), 
        emdelta);
  }

  private void eval(final String comment, final Chunker chunker) 
  throws CommandLineError, IOException, EvalError, ChunkerError {
    
    if (outputManager.isNull() && (evalType == null || 
        (evalType.length == 1 && evalType[0].equals("none")))) return;
    
    final ChunkedSegmentedCorpus chunkerOutput =  
      chunker.getChunkedCorpus(evalManager.getEvalStopSegmentCorpus());
    
    for (Eval eval: getEvals()) 
      eval.eval(comment, chunkerOutput);
    
    if (!outputManager.isNull())
      if (outputAll || writeOutput.isEmpty())
        writeOutput.add(chunkerOutput);
    
      else 
        writeOutput.set(0, chunkerOutput);
      
  }

  private void writeEval(final PrintStream out) 
  throws IOException, CommandLineError, EvalError, CorpusError {
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

  private PrintStream getVerbosePrintStream() {
    if (verbose)
      return System.err;
    else
      return null;
  }
  
  private static void usageError() {
    printUsage(System.err);
    System.exit(1);
  }

  private static void stagedModelEval(
      final Main prog, final SequenceModelChunker model) 
  throws CommandLineError, IOException, EvalError, ChunkerError, CorpusError {
    prog.eval("No EM", model.getCurrentChunker());
    while (model.anotherIteration()) {
      model.updateWithEM(prog.getVerbosePrintStream());
      prog.eval(
          String.format("Iter %d", model.getCurrentIter()),
          model.getCurrentChunker());
    }
    prog.writeEval(System.out);
  }
  

  private static void chunk(final Main prog) 
  throws CommandLineError, IOException, EvalError, ChunkerError, CorpusError, 
  EncoderError {
    switch (prog.chunkingStrategy) {
      case TWOSTAGE:
        stagedModelEval(prog, prog.getTwoStageChunker());
        break;
        
      case SOFT:
        softModelEval(prog, prog.getSoftTrainChunker());
        break;
        
      default:
        return;
    }
  }
  
  
  private SequenceModelChunker getTwoStageChunker() 
  throws CommandLineError, EncoderError {
    switch (chunkerType) {
      case HMM:
        return getHMMModelChunker();
      
      case PRLG:
        return getPRLGModelChunker();
        
      default:
        return null;  
    }
  }
  
  private SequenceModelChunker getSoftTrainChunker() 
  throws CommandLineError, EncoderError {
    switch (chunkerType) {
      case HMM:
        return getHMMSoftModelChunker();
        
      case PRLG:
        return getPRLGSoftModelChunker();
        
      default:
        return null;
    }
  }

  private static void softModelEval(
      final Main prog, final SequenceModelChunker model) 
  throws IOException, IOException, EvalError, ChunkerError, CommandLineError, 
  CorpusError {
    prog.eval("Iter 0", model.getCurrentChunker());
    while (model.anotherIteration()) {
      model.updateWithEM(prog.getVerbosePrintStream());
      prog.eval(
          String.format("Iter %d", model.getCurrentIter()),
          model.getCurrentChunker());
    }
    prog.outputManager.writeEval(prog.evalManager);
  }
  
  private static void debug(Main prog) throws IOException, CommandLineError {
    if (prog.trainCorpusString != null) {
      StopSegmentCorpus corpus = prog.getTrainStopSegmentCorpus();
      corpus.writeTo(prog.output);
    }
    
    else if (prog.testCorpusString != null) {
      UnlabeledBracketSetCorpus corpus = prog.getGoldUnlabeledBracketSets();
      corpus.writeTo(prog.output);
    }
  }
  
  public static void main(String[] argv) {
    try {
      Main prog = new Main(argv);
      
      if (prog.args.length == 0)  {
        System.err.println("Please specify an action\n");
        usageError();
      }

      if (prog.action.equals("chunk"))
        chunk(prog);
      
      else if (prog.action.equals("debug"))
        debug(prog);
      
      else {
        System.err.println("Unexpected action: " + prog.action);
        usageError();
      }
      
    } catch (CommandLineError e) {
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
      e.printStackTrace(System.err);
      usageError();
    } catch (CorpusError e) {
      System.err.println("Problem with corpus");
      e.printStackTrace(System.err);
    }
  }
}
