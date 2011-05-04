package upparse.eval;

import java.io.*;
import java.util.*;

import upparse.corpus.*;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public abstract class Eval {
  
  private final List<UnlabeledExperimentEval> experiments = 
    new ArrayList<UnlabeledExperimentEval>();

  private final String evalName;
  
  protected Eval(final String _evalName) {
    evalName = _evalName;
  }
  
  public String getEvalName() {
    return evalName;
  }
  
  protected void addExperiment(UnlabeledExperimentEval exp) {
    experiments.add(exp);
  }
  
  protected UnlabeledExperimentEval lastExperment() {
    return experiments.get(experiments.size()-1);
  }

  public abstract void eval(String string, ChunkedSegmentedCorpus output) 
  throws EvalError;

  public String getName() {
    return evalName;
  }

  public void writeSummary(
      final EvalReportType evalType, final PrintStream out, final boolean onlyLast) 
  throws EvalError {
    if (onlyLast) {
       writeSummary(lastExperment(), evalType, out);
    } else {
      for (UnlabeledExperimentEval experiment: experiments) {
        writeSummary(experiment, evalType, out);
      }
    }
  }

  private void writeSummary(
      UnlabeledExperimentEval experiment, EvalReportType evalType, PrintStream out) 
  throws EvalError {
    switch (evalType) {
      case PR:
        experiment.writeSummary(out);
        break;
        
      case PRLcsv: 
        experiment.writeSummaryWithLenCSV(out);
        break;
        
      case nPRLcsv: 
        experiment.writeSummaryWithLenAndNameCSV(out);
        break;
        
      case PRL:
        experiment.writeSummaryWithLen(out);
        break;
        
      case PRC:
        experiment.writeSummaryWithCounts(out);
        break;
        
      case PRCL:
        experiment.writeSummaryWithCountsAndLen(out);
        break;
        
      default:
        throw new EvalError("Unknown eval type:: " + evalType);
    }
  }


  public static String evalReportHelp() {
    return
    "Evaluation reports:\n" +
    "  PR      : Precision / recall / F-score\n" +
    "  PRL     : Prec / rec / F plus chunk length info\n" +
    "  PRC     : Prec / rec/ F plus raw counts for tp, fp and fn\n" +
    "  PRCL    : PRL output with raw counts\n" +
    "  PRLcsv  : PRL output in CSV format\n" +
    "  nPRLcsv : PRLcsv with experiment name column";
  }
}
