package upparse.eval;

import java.io.*;
import java.util.*;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
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
      final String evalType, final PrintStream out, final boolean onlyLast) 
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
      UnlabeledExperimentEval experiment, String evalType, PrintStream out) 
  throws EvalError {
    if (evalType.equals("PR"))
      experiment.writeSummary(out);

    else if (evalType.equals("PRLcsv"))
      experiment.writeSummaryWithLenCSV(out);

    else if (evalType.equals("nPRLcsv"))
      experiment.writeSummaryWithLenAndNameCSV(out);

    else if (evalType.equals("PRL")) 
      experiment.writeSummaryWithLen(out);

    else if (evalType.equals("PRC"))
      experiment.writeSummaryWithCounts(out);
    
    else if (evalType.equals("PRCL"))
      experiment.writeSummaryWithCountsAndLen(out);

    else 
      throw new EvalError("Unknown eval type:: " + evalType);
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
