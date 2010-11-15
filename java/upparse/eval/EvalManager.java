package upparse.eval;

import java.io.*;
import java.util.*;

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
  
  public void setTestFileType(CorpusType t) { testFileType = t; }

  public void setEvalReportType(EvalReportType type) {
    evalReportType = type;
  }
  
  public void setParserEvaluationTypes(String string) 
  throws EvalError, CorpusError {
    for (String s: string.split(",")) {
      switch (OutputType.valueOf(s)) {
        case CLUMP:
          evals.add(ChunkingEval
              .fromChunkedCorpus("clumps", getClumpGoldStandard()));
          break;
          
        case NPS:
          evals.add(ChunkingEval
              .fromChunkedCorpus("NPs", getNPsGoldStandard()));
          break;
          
        case TREEBANKPREC:
          evals.add(TreebankPrecisionEval
              .fromUnlabeledBracketSets("Prec", getGoldUnlabeledBracketSets()));
          break;
          
        case TREEBANKFLAT:
          evals.add(TreebankFlatEval
              .fromUnlabeledBracketSets("Flat", getGoldUnlabeledBracketSets()));
          
        case TREEBANKRB:
          evals.add(TreebankRBEval.fromUnlabeledBracketSets(
              "RB", getGoldUnlabeledBracketSets()));
          
        case NONE:
          evals.add(null);
          
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
    if (goldUnlabeledBracketSet == null)
      goldUnlabeledBracketSet = CorpusUtil.goldUnlabeledBracketSets(
          testFileType, alpha, corpusFiles, filterLength);
    assert goldUnlabeledBracketSet != null;
    return goldUnlabeledBracketSet;
  }
  
  private ChunkedCorpus getNPsGoldStandard() throws CorpusError {
    if (npsGoldStandard == null) 
      npsGoldStandard = CorpusUtil.npsGoldStandard(
          testFileType, alpha, corpusFiles, filterLength);
    assert npsGoldStandard != null;
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

  private ChunkedCorpus getClumpGoldStandard() throws EvalError {
    if (clumpGoldStandard == null) makeClumpGoldStandard();
    assert clumpGoldStandard != null;
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
}
