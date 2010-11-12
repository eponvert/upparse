package upparse.eval;

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

  public void setEvalReportType(EvalReportType type) {
    evalReportType = type;
  }

  public void setParserEvaluationTypes(String string) throws EvalError {
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
          
        default: 
          throw new EvalError("Unexpected evaluation type: " + s);
      }
    }
  }
  
  public void setTestCorpusString(String[] filenames) {
    corpusFiles = filenames;
  }
  
  private UnlabeledBracketSetCorpus getGoldUnlabeledBracketSets() throws EvalError { 
    if (goldUnlabeledBracketSet == null) makeGoldUnlabeledBracketSets();
    assert goldUnlabeledBracketSet != null;
    return goldUnlabeledBracketSet;
  }
  
  private void makeGoldUnlabeledBracketSets() throws EvalError {
    switch (testFileType) {
      case WSJ:
        goldUnlabeledBracketSet = 
         CorpusUtil.wsjUnlabeledBracketSetCorpus(alpha, corpusFiles);
        break;
        
      case NEGRA:
        goldUnlabeledBracketSet =
          CorpusUtil.negraUnlabeledBrackSetCorpus(alpha, corpusFiles);
        break;
        
      case CTB:
        goldUnlabeledBracketSet =
          CorpusUtil.ctbUnlabeledBracketSetCorpus(alpha, corpusFiles);
        
      default:
        throw new EvalError(
            "Unexpected file type for unlabeled bracket sets: " + testFileType);
    }
    
    if (filterLength > 0)
      goldUnlabeledBracketSet = 
        goldUnlabeledBracketSet.filterBySentenceLength(filterLength);
  }
  
  private void makeNPsGoldStandard() throws EvalError {
    switch (testFileType) {
      case WSJ:
        npsGoldStandard = CorpusUtil.wsjNPsGoldStandard(alpha, corpusFiles);
        break;
        
      case NEGRA:
        npsGoldStandard = CorpusUtil.negraNPsGoldStandard(alpha, corpusFiles);
        break;
        
      case CTB:
        npsGoldStandard = CorpusUtil.ctbNPsGoldStandard(alpha, corpusFiles);
        break;
        
      default:
        throw new EvalError(
            "Unexpected file type for NPs gold standard: " + testFileType);
    }
    
    if (filterLength > 0)
      npsGoldStandard = npsGoldStandard.filterBySentenceLength(filterLength);
  }

  private ChunkedCorpus getNPsGoldStandard() throws EvalError {
    if (npsGoldStandard == null) makeNPsGoldStandard();
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

  public StopSegmentCorpus getEvalStopSegmentCorpus() {
    if (testStopSegmentCorpus == null) {
      testStopSegmentCorpus = 
        getStopSegmentCorpus(corpusFiles, testFileType);
      if (filterTest > 0)
        testStopSegmentCorpus = testStopSegmentCorpus.filterLen(filterTest);
    }
    return testStopSegmentCorpus;
  }
}
