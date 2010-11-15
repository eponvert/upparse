package upparse.cli;

import java.io.*;
import java.util.*;

import upparse.corpus.*;
import upparse.corpus.BIOEncoder.*;
import upparse.eval.*;
import upparse.model.*;

/**
 * Main class manages command-line interface to UPP
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class Main {

  private static enum ChunkingStrategy { TWOSTAGE, SOFT; }
  
  private final Alpha alpha = new Alpha();
  private OutputManager outputManager = OutputManager.nullOutputManager();
  private EvalManager evalManager = new EvalManager(alpha);
  private String factor = "2,1,1";
  private int iter = -1;
  private double emdelta = .001;
  private String[] args = new String[0];
  private BIOEncoder encoder =
    BIOEncoder.getBIOEncoder(EncoderType.BIO, KeepStop.STOP, alpha);
  private String[] trainCorpusString = null;
  private CorpusType trainFileType = CorpusType.WSJ;
  private int trainSents = -1;
  private StopSegmentCorpus trainStopSegmentCorpus;
  private double prlgSmooth = 0.1;
  private SequenceModelType chunkerType = SequenceModelType.PRLG;
  private ChunkingStrategy chunkingStrategy = ChunkingStrategy.TWOSTAGE;
  private int filterTrain = -1;
  private final String action;

  private Main(String[] args) 
  throws CommandLineError, IOException, EvalError, EncoderError, CorpusError {
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
      encoder =
        BIOEncoder.getBIOEncoder(EncoderType.BIO, KeepStop.STOP, alpha);
      int filterTest = -1;
      while (i < args.length) {
        arg = args[i++];

        if (arg.equals("-output")) 
          outputManager = OutputManager.fromDirname(args[i++]);
        
        else if (arg.equals("-filterTrain"))
          filterTrain = Integer.parseInt(args[i++]);
        
        else if (arg.equals("-filterTest"))
          filterTest  = Integer.parseInt(args[i++]);
        
        else if (arg.equals("-chunkingStrategy"))
          chunkingStrategy = ChunkingStrategy.valueOf(args[i++]);
        
        else if (arg.equals("-chunkerType"))
          chunkerType = SequenceModelType.valueOf(args[i++]);
        
        else if (arg.equals("-prlgSmooth"))
          prlgSmooth = Double.parseDouble(args[i++]);
        
        else if (arg.equals("-outputType"))
          outputType = OutputType.valueOf(args[i++]);
        
        else if (arg.equals("-numtrain"))
          trainSents = Integer.parseInt(args[i++]);
        
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
          evalManager.setTestFileType(CorpusType.valueOf(args[i++]));

        else if (arg.equals("-trainFileType"))
          trainFileType = CorpusType.valueOf(args[i++]);

        else if (arg.equals("-factor") || arg.equals("-F")) 
          factor = args[i++];

        else if (arg.equals("-iterations")) iter = Integer.parseInt(args[i++]);

        else if (arg.equals("-emdelta")) emdelta = Float.parseFloat(args[i++]);

        else if (arg.equals("-G") || arg.equals("-encoderType"))
          encoder = BIOEncoder.getBIOEncoder(
              EncoderType.valueOf(args[i++]), KeepStop.STOP, alpha);
        
        else if (arg.equals("-E") || arg.equals("-evalReportType"))
          evalManager.setEvalReportType(EvalReportType.valueOf(args[i++]));
        
        else if (arg.equals("-e") || arg.equals("-evalTypes"))
          eval = args[i++];

        else if (arg.equals("-outputAll")) outputManager.setOutputAll(true);

        else otherArgs.add(arg);
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
      } else { 
        evalManager.setTestCorpusString(testCorpusString);
        evalManager.setFilterLen(filterTest);
      }
      
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
        OutputType.outputTypesHelp() +
        "\n\n" +
        Eval.evalReportHelp() +
        "\n\n" +
        BIOEncoder.EncoderType.encoderTypeHelp()
    );
  }
  
  private StopSegmentCorpus getTrainStopSegmentCorpus() throws CorpusError {
    if (trainStopSegmentCorpus == null) makeTrainStopSegmentCorpus();
    return trainStopSegmentCorpus;
  }
  
  private void makeTrainStopSegmentCorpus() throws CorpusError {
    trainStopSegmentCorpus = CorpusUtil.stopSegmentCorpus(
          alpha, trainCorpusString, trainFileType, trainSents, filterTrain);
    assert trainStopSegmentCorpus != null;
  }
  
  private SimpleChunker getSimpleChunker() throws CorpusError {
    return SimpleChunker.fromStopSegmentCorpus(
        alpha,
        getTrainStopSegmentCorpus(),
        getFactor());
  }

  private void evalChunker(final String comment, final Chunker chunker) 
  throws IOException, EvalError, ChunkerError, CorpusError {
    
    if (outputManager.isNull() && evalManager.isNull()) return; 
    
    final ChunkedSegmentedCorpus chunkerOutput =  
      chunker.getChunkedCorpus(evalManager.getEvalStopSegmentCorpus());
    
    evalManager.addChunkerOutput(comment, chunkerOutput);
    
    if (!outputManager.isNull())
      outputManager.addChunkerOutput(chunkerOutput);
  }

  private static void usageError() {
    printUsage(System.err);
    System.exit(1);
  }
  
  private void writeOutput() throws EvalError, IOException, CorpusError {
    evalManager.writeEval(outputManager.getResultsStream());
    outputManager.writeOutput();
  }

  private void chunkerEval(final SequenceModelChunker model) 
  throws IOException, EvalError, ChunkerError, CorpusError {
    evalChunker("Iter 0", model.getCurrentChunker());
    while (model.anotherIteration()) {
      model.updateWithEM(outputManager.getStatusStream());
      evalChunker(
          String.format("Iter %d", model.getCurrentIter()),
          model.getCurrentChunker());
    }
    writeOutput();
  }
  
  private SequenceModel getSequenceModel() 
  throws EncoderError, SequenceModelError, CorpusError, CommandLineError {
    final StopSegmentCorpus train = getTrainStopSegmentCorpus();
    switch (chunkingStrategy) {
      case TWOSTAGE:
        final SimpleChunker c = getSimpleChunker();
        final ChunkedSegmentedCorpus psuedoTraining = c.getChunkedCorpus(train);
        return SequenceModel.mleEstimate(
            chunkerType, psuedoTraining, encoder, prlgSmooth);
      case SOFT:
        return SequenceModel.softEstimate(
            chunkerType, train, encoder, prlgSmooth);
        
      default:
        throw new CommandLineError(
            "Unexpected chunking strategy: " + chunkingStrategy);
    }
  }
  
  private void chunk()
  throws CommandLineError, IOException, EvalError, ChunkerError, CorpusError, 
  EncoderError, SequenceModelError {
    chunkerEval(new SequenceModelChunker(getSequenceModel(), emdelta));
  }
  
  public static void main(String[] argv) {
    try {
      Main prog = new Main(argv);
      
      if (prog.args.length == 0)  {
        System.err.println("Please specify an action\n");
        usageError();
      }

      if (prog.action.equals("chunk")) prog.chunk();

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
    } catch (SequenceModelError e) {
      System.err.println("Problem with sequence model");
      e.printStackTrace(System.err);
    }
  }
}
