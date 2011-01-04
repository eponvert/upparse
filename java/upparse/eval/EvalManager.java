package upparse.eval;

import java.io.*;
import java.util.*;

import upparse.cli.*;
import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class EvalManager {

  Alpha alpha;

  public EvalManager(final Alpha a, PrintStream printStream) {
    alpha = a;
    statusStream = printStream;
  }
  
  public void setStatusStream(PrintStream stream) {
    statusStream = stream;
  }

  private EvalReportType evalReportType = EvalReportType.PR;
  private final List<Eval> evals = new ArrayList<Eval>();
  private final List<OutputType> evalTypes = new ArrayList<OutputType>();
  private String[] corpusFiles = null;
  private int filterLength = -1;
  private CorpusType testFileType = CorpusType.WSJ;
  private ChunkedCorpus npsGoldStandard;
  private ChunkedCorpus clumpGoldStandard = null;
  private UnlabeledBracketSetCorpus goldUnlabeledBracketSet = null;
  private StopSegmentCorpus testStopSegmentCorpus = null;
  private final int numSent = -1;
  private final boolean onlyLast = false;
  private TreebankEval treebankEval;
  private ChunkingEval npsEval;
  private ChunkingEval clumpsEval;
  private TreebankEval ubsFromClumpsEval;
  private TreebankEval ubsFromNPsEval;
  private boolean noSeg = false;
  private PrintStream statusStream;
  private ChunkedCorpus ppsGoldStandard;
  private TreebankEval ubsFromPPsEval;
  private boolean reverse = false;

  public void setNoSeg(final boolean b) {
    noSeg = b;
  }

  public void writeMetadata(final PrintStream s) {
    s.print("  Evaluation type: ");
    for (final Eval e : evals)
      s.print(e.getEvalName() + " ");
    s.println();
    s.println("  Test files:");
    for (final String f : corpusFiles)
      s.println("    " + f);
    if (filterLength > 0)
      s.println("  Filter test files by len: " + filterLength);
//    if (numSent > 0)
//      s.println("  Num test sentences: " + numSent);
    s.println("  Test file type: " + testFileType);
  }

  public void setTestFileType(final CorpusType t) {
    testFileType = t;
  }

  public void setEvalReportType(final EvalReportType type) {
    evalReportType = type;
  }

  public void setParserEvaluationTypes(final String string) throws EvalError,
      CorpusError {
    if (string.equals("")) {
      evalTypes.add(OutputType.CLUMP);
      evalTypes.add(OutputType.NPS);
    } else
      for (final String s : string.split(","))
        evalTypes.add(OutputType.valueOf(s));
  }

  private void initParseEvaluationTypes() throws EvalError, CorpusError {
    for (final OutputType t : evalTypes) {
      switch (t) {
        case CLUMP:
          evals.add(ChunkingEval.fromChunkedCorpus(t, getClumpGoldStandard()));
          break;

        case NPS:
          evals.add(ChunkingEval.fromChunkedCorpus(t, getNPsGoldStandard()));
          break;
          
        case PPS:
          evals.add(ChunkingEval.fromChunkedCorpus(t, getPPsGoldStandard()));
          break;

        case TREEBANKPREC:
          evals.add(TreebankPrecisionEval.fromUnlabeledBracketSets(t,
              getGoldUnlabeledBracketSets()));
          break;

        case TREEBANKFLAT:
          evals.add(TreebankFlatEval.fromUnlabeledBracketSets(t,
              getGoldUnlabeledBracketSets()));
          break;

        case TREEBANKRB:
          evals.add(TreebankRBEval.fromUnlabeledBracketSets(t,
              getGoldUnlabeledBracketSets()));
          break;

        case NONE:
          evals.add(NullEval.instance());
          break;

        default:
          throw new EvalError("Unexpected evaluation type: " + t);
      }
    }
  }

  public void setTestCorpusString(final String[] filenames) {
    corpusFiles = filenames;
  }

  private UnlabeledBracketSetCorpus getGoldUnlabeledBracketSets()
      throws EvalError, CorpusError {
    checkGoldUnlabeledBracketSet();
    return goldUnlabeledBracketSet;
  }

  private void checkGoldUnlabeledBracketSet() throws CorpusError {
    if (goldUnlabeledBracketSet == null)
      goldUnlabeledBracketSet = CorpusUtil.goldUnlabeledBracketSets(
          testFileType, alpha, corpusFiles, filterLength, reverse);
    assert goldUnlabeledBracketSet != null;
  }

  private void checkNPsGoldStandard() throws CorpusError {
    if (npsGoldStandard == null)
      npsGoldStandard = CorpusUtil.npsGoldStandard(testFileType, alpha,
          corpusFiles, filterLength, reverse);
    assert npsGoldStandard != null;
  }

  private ChunkedCorpus getNPsGoldStandard() throws CorpusError {
    checkNPsGoldStandard();
    return npsGoldStandard;
  }

  private void checkPPsGoldStandard() throws CorpusError {
    if (ppsGoldStandard == null)
      ppsGoldStandard = CorpusUtil.ppsGoldStandard(testFileType, alpha,
          corpusFiles, filterLength, reverse);
    assert ppsGoldStandard != null;
  }

  private ChunkedCorpus getPPsGoldStandard() throws CorpusError {
    checkPPsGoldStandard();
    return ppsGoldStandard;
  }

  private void makeClumpGoldStandard() throws EvalError {
    switch (testFileType) {
      case WSJ:
        clumpGoldStandard = CorpusUtil.wsjClumpGoldStandard(alpha, corpusFiles);
        break;

      case NEGRA:
        clumpGoldStandard = CorpusUtil.negraClumpGoldStandard(alpha,
            corpusFiles);
        break;

      case CTB:
        clumpGoldStandard = CorpusUtil.ctbClumpGoldStandard(alpha, corpusFiles);
        break;
        
      case SPL:
        evalTypes.clear();
        evalTypes.add(OutputType.NONE);
        evals.clear();
        evals.add(NullEval.instance());
        break;

      default:
        throw new EvalError("Unexpected file type for clumping gold standard: "
            + corpusFiles);
    }

    if (filterLength > 0)
      clumpGoldStandard = clumpGoldStandard
          .filterBySentenceLength(filterLength);
    
    if (reverse)
      clumpGoldStandard.reverse();
  }

  private void checkClumpGoldStandard() throws EvalError {
    if (clumpGoldStandard == null)
      makeClumpGoldStandard();
  }

  private ChunkedCorpus getClumpGoldStandard() throws EvalError {
    checkClumpGoldStandard();
    return clumpGoldStandard;
  }

  public void setFilterLen(final int len) {
    filterLength = len;
  }

  public StopSegmentCorpus getEvalStopSegmentCorpus() throws CorpusError,
      EvalError {
    if (evals.size() == 0 && testFileType != CorpusType.SPL) 
      initParseEvaluationTypes();
    if (testStopSegmentCorpus == null)
      makeEvalStopSegmentCorpus();
    return testStopSegmentCorpus;
  }

  private void makeEvalStopSegmentCorpus() throws CorpusError {
    testStopSegmentCorpus = CorpusUtil.stopSegmentCorpus(alpha, corpusFiles,
        testFileType, numSent, filterLength, noSeg, statusStream, reverse);
  }

  public boolean isNull() throws EvalError, CorpusError {
    return corpusFiles.length == 0 || evalReportType == null || noEvals();
  }

  private boolean noEvals() throws EvalError, CorpusError {
    if (evals.size() == 0)
      initParseEvaluationTypes();
    return evals.size() == 0 || (evals.size() == 1 && evals.get(0) == null);
  }

  public void addChunkerOutput(final String comment,
      final ChunkedSegmentedCorpus output) throws EvalError, CorpusError {
    if (evals.size() == 0)
      initParseEvaluationTypes();
    for (final Eval eval : evals)
      eval.eval(comment, output);
  }

  public void writeEval(final PrintStream out) throws EvalError {
    for (final Eval eval : evals) {
      eval.writeSummary(evalReportType, out, onlyLast);
      out.println();
    }
  }

  public void initializeCCLParserEval() throws EvalError, CorpusError {
    checkNPsGoldStandard();
    checkPPsGoldStandard();
    checkClumpGoldStandard();
    treebankEval = new TreebankEval("asTrees", getGoldUnlabeledBracketSets());
    npsEval = ChunkingEval.fromChunkedCorpus(OutputType.NPS, npsGoldStandard);
    clumpsEval = ChunkingEval.fromChunkedCorpus(OutputType.CLUMP,
        clumpGoldStandard);
    ubsFromClumpsEval = new TreebankEval("clumps Recall",
        clumpGoldStandard.toUnlabeledBracketSetCorpus());
    ubsFromNPsEval = new TreebankEval("NPs Recall",
        npsGoldStandard.toUnlabeledBracketSetCorpus());
    ubsFromPPsEval = new TreebankEval("PPs Recall", 
        ppsGoldStandard.toUnlabeledBracketSetCorpus());
  }

  public void evalParserOutput(final UnlabeledBracketSetCorpus output,
      final OutputManager man) throws CorpusError, EvalError, IOException {
    final ChunkedCorpus chunked = CorpusUtil.getChunkedCorpusClumps(alpha,
        output);
    treebankEval.getExperiment("asTrees", output.getTrees()).writeSummary(
        man.getResultsStream());

    clumpsEval.addExperiment(clumpsEval
        .newChunkingExperiment("clumps", chunked));
    clumpsEval.writeSummary(evalReportType, man.getResultsStream(), false);

    ubsFromClumpsEval.getExperiment("", output.getTrees()).writeSummary(
        man.getResultsStream());

    npsEval.addExperiment(npsEval.newChunkingExperiment("NPs", chunked));
    npsEval.writeSummary(evalReportType, man.getResultsStream(), false);
    
    ubsFromNPsEval.getExperiment("", output.getTrees()).writeSummary(
        man.getResultsStream());

    ubsFromPPsEval.getExperiment("", output.getTrees()).writeSummary(
        man.getResultsStream());

    if (!man.isNull()) {
      output.writeTo(man.treeOutputFilename());
      chunked.writeTo(man.clumpsOutputFilename());
    }
  }

  public int getFilterLen() { 
    return filterLength;
  }

  public boolean doEval() { 
    return testFileType != CorpusType.SPL;
  }

  public void setReverseEval(final boolean r) {
    reverse = r;
  }
}
