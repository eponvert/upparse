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
  
  public EvalManager(Alpha a) { alpha = a; } 

  private EvalReportType evalReportType = EvalReportType.PR;
  List<Eval> evals = new ArrayList<Eval>();
  private String[] corpusFiles = null;
  private int filterLength = -1;
  private CorpusType testFileType = CorpusType.WSJ;
  private ChunkedCorpus npsGoldStandard;
  private ChunkedCorpus clumpGoldStandard = null;
  private UnlabeledBracketSetCorpus goldUnlabeledBracketSet = null;
  private StopSegmentCorpus testStopSegmentCorpus = null;
  private int numSent = -1;
  private boolean onlyLast = false;
  private TreebankEval treebankEval;
  private ChunkingEval npsEval;
  private ChunkingEval clumpsEval;
  private TreebankEval ubsFromClumpsEval;
  private TreebankEval ubsFromNPsEval;


  public void writeMetadata(PrintStream s) {
    s.print("  Evaluation type: ");
    for (Eval e: evals) s.print(e.getEvalName() + " ");
    s.println();
    s.println("  Test files:");
    for (String f: corpusFiles) s.println("    " + f);
    if (filterLength > 0) 
      s.println("  Filter test files by len: " + filterLength);
    if (numSent > 0)
      s.println("  Num test sentences: " + numSent);
    s.println("  Test file type: " + testFileType);
  }
  
  public void setTestFileType(CorpusType t) { testFileType = t; }

  public void setEvalReportType(EvalReportType type) {
    evalReportType = type;
  }
  
  public void setParserEvaluationTypes(String string) 
  throws EvalError, CorpusError {
    if (string.equals("")) { 
      evals.add(
          ChunkingEval.fromChunkedCorpus(
              OutputType.CLUMP, getClumpGoldStandard()));
      evals.add(
          ChunkingEval.fromChunkedCorpus(
              OutputType.NPS, getNPsGoldStandard()));
    }
    else for (String s: string.split(",")) {
      switch (OutputType.valueOf(s)) {
        case CLUMP:
          evals.add(ChunkingEval
              .fromChunkedCorpus(OutputType.CLUMP, getClumpGoldStandard()));
          break;
          
        case NPS:
          evals.add(ChunkingEval
              .fromChunkedCorpus(OutputType.NPS, getNPsGoldStandard()));
          break;
          
        case TREEBANKPREC:
          evals.add(TreebankPrecisionEval.fromUnlabeledBracketSets(
              OutputType.TREEBANKPREC, getGoldUnlabeledBracketSets()));
          break;
          
        case TREEBANKFLAT:
          evals.add(TreebankFlatEval.fromUnlabeledBracketSets(
              OutputType.TREEBANKFLAT, getGoldUnlabeledBracketSets()));
          break;
          
        case TREEBANKRB:
          evals.add(TreebankRBEval.fromUnlabeledBracketSets(
              OutputType.TREEBANKRB, getGoldUnlabeledBracketSets()));
          break;
          
        case NONE:
          evals.add(NullEval.instance());
          break;
          
        default: 
          throw new EvalError("Unexpected evaluation type: " + s);
      }
    }
  }
  
  public void setTestCorpusString(String[] filenames) {
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
          testFileType, alpha, corpusFiles, filterLength);
    assert goldUnlabeledBracketSet != null;
  }
  
  private void checkNPsGoldStandard() throws CorpusError {
    if (npsGoldStandard == null) 
      npsGoldStandard = CorpusUtil.npsGoldStandard(
          testFileType, alpha, corpusFiles, filterLength);
    assert npsGoldStandard != null;
  }

  private ChunkedCorpus getNPsGoldStandard() throws CorpusError {
    checkNPsGoldStandard();
    return npsGoldStandard;
  }
  
  private void makeClumpGoldStandard() throws EvalError {
    switch (testFileType) {
      case WSJ:
        clumpGoldStandard = CorpusUtil.wsjClumpGoldStandard(alpha, corpusFiles);
        break;
        
      case NEGRA:
        clumpGoldStandard = CorpusUtil.negraClumpGoldStandard(alpha, corpusFiles);
        break;
        
      case CTB:
        clumpGoldStandard  = CorpusUtil.ctbClumpGoldStandard(alpha, corpusFiles);
        break;
        
      default:
        throw new EvalError(
            "Unexpected file type for clumping gold standard: " + corpusFiles);
    }
    
    if (filterLength > 0)
      clumpGoldStandard = 
        clumpGoldStandard.filterBySentenceLength(filterLength);
  }
  
  private void checkClumpGoldStandard() throws EvalError {
    if (clumpGoldStandard == null) makeClumpGoldStandard();
    assert clumpGoldStandard != null;
  }

  private ChunkedCorpus getClumpGoldStandard() throws EvalError {
    checkClumpGoldStandard();
    return clumpGoldStandard;
  }



  public void setFilterLen(int len) { filterLength = len; }

  public StopSegmentCorpus getEvalStopSegmentCorpus() throws CorpusError {
    if (testStopSegmentCorpus == null) makeEvalStopSegmentCorpus();
    return testStopSegmentCorpus;
  }

  private void makeEvalStopSegmentCorpus() throws CorpusError {
    testStopSegmentCorpus = CorpusUtil.stopSegmentCorpus(
          alpha, corpusFiles, testFileType, numSent, filterLength); 
  }

  public boolean isNull() {
    return evalReportType == null || noEvals();
  }

  private boolean noEvals() {
    return evals.size() == 0 || (evals.size() == 1 && evals.get(0) == null);
  }

  public void addChunkerOutput(
      final String comment, final ChunkedSegmentedCorpus output) 
  throws EvalError {
    for (Eval eval: evals)
      eval.eval(comment, output);
  }

  public void writeEval(PrintStream out) throws EvalError {
    for (Eval eval: evals) {
      eval.writeSummary(evalReportType, out, onlyLast);
      out.println();
    }
  }
  
  public void initializeCCLParserEval() throws EvalError, CorpusError {
    checkNPsGoldStandard();
    checkClumpGoldStandard();
    treebankEval = new TreebankEval("asTrees", getGoldUnlabeledBracketSets());
    npsEval =  ChunkingEval.fromChunkedCorpus(OutputType.NPS, npsGoldStandard);
    clumpsEval =
      ChunkingEval.fromChunkedCorpus(OutputType.CLUMP, clumpGoldStandard);
    ubsFromClumpsEval = new TreebankEval("clumps Recall", 
        clumpGoldStandard.toUnlabeledBracketSetCorpus());
    ubsFromNPsEval = new TreebankEval("NPs Recall", 
        npsGoldStandard.toUnlabeledBracketSetCorpus());
  }

  public void evalParserOutput(
      final UnlabeledBracketSetCorpus output, final OutputManager man) 
  throws CorpusError, EvalError, IOException {
    ChunkedCorpus chunked = CorpusUtil.getChunkedCorpusClumps(alpha, output);
    treebankEval
      .getExperiment("asTrees", output.getTrees())
      .writeSummary(man.getResultsStream());
    
    clumpsEval.addExperiment(
        clumpsEval.newChunkingExperiment("clumps", chunked));
    clumpsEval.writeSummary(evalReportType, man.getResultsStream(), false);
    
    ubsFromClumpsEval
      .getExperiment("", output.getTrees())
      .writeSummary(man.getResultsStream());
    
    npsEval.addExperiment(
        npsEval.newChunkingExperiment("NPs", chunked));
    npsEval.writeSummary(evalReportType, man.getResultsStream(), false);
    
    ubsFromNPsEval
      .getExperiment("", output.getTrees())
      .writeSummary(man.getResultsStream());
    
      
    if (!man.isNull()) {
      output.writeTo(man.treeOutputFilename());
      chunked.writeTo(man.clumpsOutputFilename());
    }
  }
}
