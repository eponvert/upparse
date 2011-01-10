package upparse.cli;

import java.io.*;
import java.util.*;

import upparse.corpus.*;
import upparse.corpus.TagEncoder.*;
import upparse.eval.*;
import upparse.model.*;

/**
 * Main class manages command-line interface to UPP
 * 
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class Main {

  private static enum ChunkingStrategy {
    TWOSTAGE, SOFT, UNIFORM, RANDOM, SUPERVISED_CLUMP, SUPERVISED_NPS;
  }

  private static final String CCLPARSER_EVAL_ACTION = "cclp-eval";
  private static final String CHUNK_ACTION = "chunk";

  private final Alpha alpha = new Alpha();
  private OutputManager outputManager = OutputManager.nullOutputManager();
  private final EvalManager evalManager = new EvalManager(alpha,
      outputManager.getStatusStream());
  private String factor = "2,1,1";
  private int iter = -1;
  private double emdelta = .001;
  private String[] args = new String[0];
  private TagEncoder encoder = TagEncoder.getBIOEncoder(EncoderType.BIO,
      KeepStop.STOP, alpha);
  private String[] trainCorpusString = null;
  private CorpusType trainFileType = CorpusType.WSJ;
  private int trainSents = -1;
  private StopSegmentCorpus trainStopSegmentCorpus;
  private double smooth = 0.1;
  private SequenceModelType chunkerType = SequenceModelType.PRLG;
  private ChunkingStrategy chunkingStrategy = ChunkingStrategy.TWOSTAGE;
  private int filterTrain = -1;
  private final String action;
  private String cclparserOutput;
  private boolean doContinuousEval = false;
  private boolean noSeg = false;
  private boolean reverse = false;
  private ChunkedSegmentedCorpus trainChunkedSegmentedCorpus;

  private Main(final String[] args) throws CommandLineError, IOException,
      EvalError, EncoderError, CorpusError {
    int i = 0;
    String arg;

    final boolean outputAll = false;
    OutputType outputType = OutputType.CLUMP;
    String eval = "";
    String[] testCorpusString = new String[0];

    if (args.length < 1)
      throw new CommandLineError("Please specify an action");

    try {
      final List<String> otherArgs = new ArrayList<String>();
      encoder = TagEncoder.getBIOEncoder(EncoderType.BIO, KeepStop.STOP, alpha);
      int filterTest = -1;
      while (i < args.length) {
        arg = args[i++];

        if (arg.equals("-output")) {
          outputManager = OutputManager.fromDirname(args[i++]);
          evalManager.setStatusStream(outputManager.getStatusStream());
        }

        if (arg.equals("-noSeg"))
          noSeg = true;

        else if (arg.equals("-reverse"))
          reverse = true;

        else if (arg.equals("-continuousEval"))
          doContinuousEval = true;

        else if (arg.equals("-cclpOutput"))
          cclparserOutput = args[i++];

        else if (arg.equals("-filterTrain"))
          filterTrain = Integer.parseInt(args[i++]);

        else if (arg.equals("-filterTest"))
          filterTest = Integer.parseInt(args[i++]);

        else if (arg.equals("-chunkingStrategy"))
          chunkingStrategy = ChunkingStrategy.valueOf(args[i++]);

        else if (arg.equals("-chunkerType"))
          chunkerType = SequenceModelType.valueOf(args[i++]);

        else if (arg.equals("-smooth"))
          smooth = Double.parseDouble(args[i++]);

        else if (arg.equals("-outputType"))
          outputType = OutputType.valueOf(args[i++]);

        else if (arg.equals("-numtrain"))
          trainSents = Integer.parseInt(args[i++]);

        else if (arg.equals("-test")) {
          final List<String> sb = new ArrayList<String>();
          while (i < args.length && args[i].charAt(0) != '-')
            sb.add(args[i++]);
          testCorpusString = sb.toArray(new String[0]);
        }

        else if (arg.equals("-train")) {
          final List<String> sb = new ArrayList<String>();
          while (i < args.length && args[i].charAt(0) != '-')
            sb.add(args[i++]);
          trainCorpusString = sb.toArray(new String[0]);
        }

        else if (arg.equals("-testFileType"))
          evalManager.setTestFileType(CorpusType.valueOf(args[i++]));

        else if (arg.equals("-trainFileType"))
          trainFileType = CorpusType.valueOf(args[i++]);

        else if (arg.equals("-factor") || arg.equals("-F"))
          factor = args[i++];

        else if (arg.equals("-iterations"))
          iter = Integer.parseInt(args[i++]);

        else if (arg.equals("-emdelta"))
          emdelta = Float.parseFloat(args[i++]);

        else if (arg.equals("-G") || arg.equals("-encoderType"))
          encoder = TagEncoder.getBIOEncoder(EncoderType.valueOf(args[i++]),
              KeepStop.STOP, alpha);

        else if (arg.equals("-E") || arg.equals("-evalReportType"))
          evalManager.setEvalReportType(EvalReportType.valueOf(args[i++]));

        else if (arg.equals("-e") || arg.equals("-evalTypes"))
          eval = args[i++];

        else if (arg.equals("-outputAll"))
          outputManager.setOutputAll(true);

        else
          otherArgs.add(arg);
      }

      this.args = otherArgs.toArray(new String[0]);
      this.action = this.args[0];

      // Setup outputManager
      outputManager.setOutputAllIter(outputAll);
      outputManager.setOutputType(outputType);

      // Setup evalManager
      evalManager.setNoSeg(noSeg);
      evalManager.setReverseEval(reverse);

      if (testCorpusString.length == 1
          && testCorpusString[0].startsWith("subset")) {
        final int len = Integer.parseInt(testCorpusString[0].substring(6));
        evalManager.setFilterLen(len);
        evalManager.setTestCorpusString(trainCorpusString);
        evalManager.setParserEvaluationTypes(eval);
      } else if (testCorpusString.length > 0) {
        evalManager.setTestCorpusString(testCorpusString);
        evalManager.setFilterLen(filterTest);

        if (evalManager.doEval())
          evalManager.setParserEvaluationTypes(eval);
      } else {
        evalManager.setTestFileType(trainFileType);
        evalManager.setFilterLen(filterTrain);
        evalManager.setTestCorpusString(trainCorpusString);
        evalManager.setParserEvaluationTypes("NONE");
      }

      // don't run EM more than 200 iterations
      if (iter < 0)
        iter = 200;

      outputManager.writeMetadata(this);
    } catch (final ArrayIndexOutOfBoundsException e) {
      e.printStackTrace(System.err);
      throw new CommandLineError();
    }
  }

  public void writeMetadata(final PrintStream s) {
    if (action.equals(CHUNK_ACTION)) {
      s.println(currentDate());
      s.println("Chunk experiment");
      s.println("  Experiment strategy: " + chunkingStrategy);
      s.println("  Chunker type: " + chunkerType);
      if (chunkingStrategy == ChunkingStrategy.TWOSTAGE)
        s.println("  Stage 1 factor: " + factor);
      s.println("  EM iter delta cutoff: " + emdelta);
      if (iter > 0)
        s.println("  EM iter num cutoff: " + iter);
      s.println("  BIO encoder: " + encoder.getClass().getSimpleName());
      if (filterTrain > 0)
        s.println("  Filter train by len: " + filterTrain);
      s.println("  Smoothing param: " + smooth);
      s.println("  Train files:");
      for (final String f : trainCorpusString)
        s.println("    " + f);
      s.println("  Train file type: " + trainFileType);
      evalManager.writeMetadata(s);
    }
  }

  private String currentDate() {
    return (new Date()).toString();
  }

  private double[] getFactor() {
    final String[] fpieces = factor.split(",");
    final double[] f = new double[fpieces.length];
    for (int i = 0; i < fpieces.length; i++)
      f[i] = Double.parseDouble(fpieces[i]);
    return f;
  }

  /**
   * Print program usage to stream
   * 
   * @param stream
   */
  private static void printUsage(final PrintStream stream) {
    final String prog = Main.class.getName();
    stream
        .println("Usage: java "
            + prog
            + " action [options] [args]\n"
            + "\n"
            + "Actions:\n"
            + "  chunk\n"
            + "  cascade-parse\n"
            + "  cclp-eval\n"
            + "\n"
            + "Options:\n"
            + "  -noSeg              Ignore all punctuation\n"
            + "  -chunkingStrategy K TWOSTAGE or SOFT\n"
            + "  -continuousEval     Run an eval on the test set after each iteration of EM\n"
            + "  -chunkerType K      HMM or PRLG\n"
            + "  -train FILES        Train using specified files\n"
            + "  -filterTrain N      Train only on sentences of len <= N\n"
            + "  -numtrain N         Train only on the first N sentences\n"
            + "  -filterTest N       Evaluate only on sentences of len <= N\n"
            + "  -test FILES         Evaluated on specified files\n"
            + "  -trainFileType X    Train files file type (eg WSJ)\n"
            + "  -testFileType X     Test files file type (eg WSJ)\n"
            + "  -output FILE        Set output file/template\n"
            + "  -outputType T       Output type (see eval types)\n"
            + "  -cclpOutput F       Output of CCLParser for comparison calc\n"
            + "  -outputAll          Produce model output for all EM iterations\n"
            + "  -F|-factor N1,N2... Factors for Stage 1 chunking\n"
            + "  -G|-encoderType T   Use chunk-encoder type T\n"
            + "  -E|-evalReportType  Evaluation report (eg PRL)\n"
            + "  -e|-evalTypes E1,E2 Evaluation types \n"
            + "  -iterations N       Iterations of EM\n"
            + "  -emdelta D          Halt EM when data perplexity change is less than\n"
            + "  -smooth V           Smoothing parameter for emissions probabilities\n"
            + "  -dontCheckTerms     Don't check that the eval and output terms are equal\n"
            + "  -onlyLast           Only show evaluation of last itertation of EM\n"
            + "  -reverse            Evaluate running sequence models backwards\n"
            + "\n" + "File types:\n" + "  WSJ    : WSJ/Penn Treebank corpus\n"
            + "  NEGRA  : Negra Treebank (Penn Treebank like)\n"
            + "  CTB    : Penn Chinese Treebank corpus\n"
            + "  SPL    : Sentence per line\n"
            + "  WPL    : Word per line (sentences seperated by blank lines)\n"
            + "\n" + OutputType.outputTypesHelp() + "\n\n"
            + Eval.evalReportHelp() + "\n\n"
            + TagEncoder.EncoderType.encoderTypeHelp());
  }

  private StopSegmentCorpus getTrainStopSegmentCorpus() throws CorpusError {
    if (trainStopSegmentCorpus == null)
      makeTrainStopSegmentCorpus();
    return trainStopSegmentCorpus;
  }

  private void makeTrainStopSegmentCorpus() throws CorpusError {
    outputManager.getStatusStream().format(
        "Creating train corpus from %d documents\n", trainCorpusString.length);
    trainStopSegmentCorpus = CorpusUtil.stopSegmentCorpus(alpha,
        trainCorpusString, trainFileType, trainSents, filterTrain, noSeg,
        outputManager.getStatusStream(), reverse);
    assert trainStopSegmentCorpus != null;
  }

  private SimpleChunker getSimpleChunker() throws CorpusError {
    return SimpleChunker.fromStopSegmentCorpus(alpha,
        getTrainStopSegmentCorpus(), getFactor());
  }

  private void evalChunker(final String comment, final Chunker chunker)
      throws IOException, EvalError, ChunkerError, CorpusError {

    if (outputManager.isNull() && evalManager.isNull())
      return;

    ChunkedSegmentedCorpus chunkerOutput = chunker.getChunkedCorpus(evalManager
        .getEvalStopSegmentCorpus());

    final int filterLen = evalManager.getFilterLen();
    if (filterLen > 0)
      chunkerOutput = chunkerOutput.filter(filterLen);

    if (reverse)
      chunkerOutput.reverse();

    evalManager.addChunkerOutput(comment, chunkerOutput);

    if (!outputManager.isNull())
      outputManager.addChunkerOutput(chunkerOutput, comment);
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
    while (model.anotherIteration()) {
      if (doContinuousEval)
        evalChunker(String.format("Iter-%d", model.getCurrentIter()),
            model.getCurrentChunker());
      model.updateWithEM(outputManager.getStatusStream());
    }
    evalChunker(String.format("Iter-%d", model.getCurrentIter()),
        model.getCurrentChunker());
    writeOutput();
  }

  private SequenceModel getSequenceModel() throws EncoderError,
      SequenceModelError, CorpusError, CommandLineError {
    final StopSegmentCorpus train = getTrainStopSegmentCorpus();
    outputManager.getStatusStream().format(
        "Training sequence model with %d sentences, using %s\n", train.size(),
        chunkingStrategy);
    switch (chunkingStrategy) {
      case TWOSTAGE:
        final SimpleChunker c = getSimpleChunker();
        final ChunkedSegmentedCorpus psuedoTraining = c.getChunkedCorpus(train);
        return SequenceModel.mleEstimate(chunkerType, psuedoTraining, encoder,
            smooth);

      case SUPERVISED_CLUMP:
        makeTrainChunkedSegmentedCorpusForClumps();
        return SequenceModel.mleEstimate(chunkerType,
            trainChunkedSegmentedCorpus, encoder, smooth);

      case SUPERVISED_NPS:
        makeTrainChunkedSegmentedCorpusForNPs();
        return SequenceModel.mleEstimate(chunkerType,
            trainChunkedSegmentedCorpus, encoder, smooth);

      case SOFT:
        return SequenceModel.softEstimate(chunkerType, train, encoder, smooth);

      case UNIFORM:
        return SequenceModel.uniformEstimate(chunkerType, train, encoder,
            smooth);

      case RANDOM:
        return SequenceModel
            .randomEstimate(chunkerType, train, encoder, smooth);

      default:
        throw new CommandLineError("Unexpected chunking strategy: "
            + chunkingStrategy);
    }
  }

  private void makeTrainChunkedSegmentedCorpusForNPs() {
    evalManager.setNoSeg(true);
    final ChunkedCorpus trainChunkedCorpus;
    switch (trainFileType) {
      case WSJ:
        trainChunkedCorpus = CorpusUtil.wsjNPsGoldStandard(alpha,
            trainCorpusString);
        break;

      case NEGRA:
        trainChunkedCorpus = CorpusUtil.negraNPsGoldStandard(alpha,
            trainCorpusString);
        break;

      case CTB:
        trainChunkedCorpus = CorpusUtil.ctbNPsGoldStandard(alpha,
            trainCorpusString);
        break;

      default:
        throw new RuntimeException(
            "Unsupported train file type for supervised clump eval: "
                + trainFileType);
    }
    trainChunkedSegmentedCorpus = trainChunkedCorpus
        .toSimpleChunkedSegmentedCorpus();
  }

  private void makeTrainChunkedSegmentedCorpusForClumps() {
    evalManager.setNoSeg(true);
    outputManager.getStatusStream().format(
        "Creating train corpus from %d documents\n", trainCorpusString.length);
    final ChunkedCorpus trainChunkedCorpus;
    switch (trainFileType) {
      case WSJ:
        trainChunkedCorpus = CorpusUtil.wsjClumpGoldStandard(alpha,
            trainCorpusString);
        break;

      case NEGRA:
        trainChunkedCorpus = CorpusUtil.negraClumpGoldStandard(alpha,
            trainCorpusString);
        break;

      case CTB:
        trainChunkedCorpus = CorpusUtil.ctbClumpGoldStandard(alpha,
            trainCorpusString);
        break;

      default:
        throw new RuntimeException(
            "Unsupported train file type for supervised clump eval: "
                + trainFileType);
    }
    trainChunkedSegmentedCorpus = trainChunkedCorpus
        .toSimpleChunkedSegmentedCorpus();
  }

  private void chunk() throws CommandLineError, IOException, EvalError,
      ChunkerError, CorpusError, EncoderError, SequenceModelError {
    chunkerEval(new SequenceModelChunker(getSequenceModel(), emdelta, iter));
  }

  private void cclpEval() throws EvalError, IOException, CorpusError {
    final UnlabeledBracketSetCorpus outputCorpus = getCCLParserOutput();
    evalManager.initializeCCLParserEval();
    evalManager.evalParserOutput(outputCorpus, outputManager);
    writeOutput();
  }

  private UnlabeledBracketSetCorpus getCCLParserOutput() {
    final String[] files = new String[] { cclparserOutput };
    return CorpusUtil.cclpUnlabeledBracketSetCorpus(alpha, files);
  }

  public static void main(final String[] argv) {
    try {
      final Main prog = new Main(argv);

      if (prog.args.length == 0) {
        System.err.println("Please specify an action\n");
        usageError();
      }

      if (prog.action.equals(CHUNK_ACTION))
        prog.chunk();
      else if (prog.action.equals(CCLPARSER_EVAL_ACTION))
        prog.cclpEval();

      else {
        System.err.println("Unexpected action: " + prog.action);
        usageError();
      }

    } catch (final CommandLineError e) {
      System.err.println("Bad command line error: " + e.getMessage());
      usageError();

    } catch (final IOException e) {
      System.err.println("IO problem");
      e.printStackTrace(System.err);
      usageError();

    } catch (final EvalError e) {
      System.err.println("Eval problem");
      e.printStackTrace(System.err);
      usageError();

    } catch (final EncoderError e) {
      System.err.println("Encoder problem");
      e.printStackTrace(System.err);
      usageError();
    } catch (final ChunkerError e) {
      System.err.println("Problem with the chunker");
      e.printStackTrace(System.err);
      usageError();
    } catch (final CorpusError e) {
      System.err.println("Problem with corpus");
      e.printStackTrace(System.err);
    } catch (final SequenceModelError e) {
      System.err.println("Problem with sequence model");
      e.printStackTrace(System.err);
    }
  }
}
