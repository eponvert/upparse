package upparse.eval;

import java.io.*;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public abstract class UnlabeledExperimentEval {
  
  private final String name;

  protected UnlabeledExperimentEval(final String _name) {
    name = _name;
  }

  public abstract int[][] getTPcounts();
  public abstract int[][] getFPcounts();
  public abstract int[][] getFNcounts();
  
  public abstract int getTPlen();
  public abstract int getFPlen();
  public abstract int getFNlen();
  
  public String getName() {
    return name;
  }
  
  public abstract String getEvalName(); 

  public void writeSummaryWithLenAndNameCSV(PrintStream out) {
    int 
    tp = sum(getTPcounts()), 
    fp = sum(getFPcounts()),
    fn = sum(getFNcounts()),
    predCount = tp + fp,
    predLen = getTPlen() + getFPlen();

    double
    predCountF = (double) predCount,
    predLenF = (double) predLen,
    predLenAvg = predLenF / predCountF,
    tpF = (double) tp, 
    fpF = (double) fp, 
    fnF = (double) fn,
    prec = 100 * tpF / (tpF + fpF),
    rec = 100 * tpF / (tpF + fnF),
    f = 2 * prec * rec / (prec + rec);

    String[] pieces = getName().split(" ");
    String name = pieces[pieces.length-1];

    if (getName().equals("No EM"))
      name = "000";

    if (!name.equals("Baseline"))
      out.println(String.format(
          "%s,%.1f,%.1f,%.1f,%.2f", name, prec, rec, f, predLenAvg));
  }

  public void writeSummaryWithLenCSV(PrintStream out) {
    int 
    tp = sum(getTPcounts()),
    fp = sum(getFPcounts()),
    fn = sum(getFNcounts()),
    predCount = tp + fp,
    predLen = getFPlen() + getFPlen();
  
  double
    predCountF = (double) predCount,
    predLenF = (double) predLen,
    predLenAvg = predLenF / predCountF,
    tpF = (double) tp, 
    fpF = (double) fp, 
    fnF = (double) fn,
    prec = 100 * tpF / (tpF + fpF),
    rec = 100 * tpF / (tpF + fnF),
    f = 2 * prec * rec / (prec + rec);
    
  out.println(String.format(
    "%.1f,%.1f,%.1f,%.2f", prec, rec, f, predLenAvg));
  }

  public void writeSummary(PrintStream out) {
    int 
      tp = sum(getTPcounts()),
      fp = sum(getFPcounts()),
      fn = sum(getFNcounts());
    
    double
      tpF = (double) tp, 
      fpF = (double) fp, 
      fnF = (double) fn,
      prec = 100 * tpF / (tpF + fpF),
      rec = 100 * tpF / (tpF + fnF),
      f = 2 * prec * rec / (prec + rec);
    
    out.println(String.format("%25s %10s : %.1f / %.1f / %.1f ",
        getEvalName(), getName(), prec, rec, f));
  }

  public void writeSummaryWithLen(PrintStream out) {
    int 
      tp = sum(getTPcounts()),
      fp = sum(getFPcounts()),
      fn = sum(getFNcounts()),
      goldCount = tp + fn,
      predCount = tp + fp,
      goldLen = getTPlen() + getFNlen(),
      predLen = getTPlen() + getFPlen();
    
    double
      goldCountF = (double) goldCount,
      predCountF = (double) predCount,
      goldLenF = (double) goldLen,
      predLenF = (double) predLen,
      goldLenAvg = goldLenF / goldCountF,
      predLenAvg = predLenF / predCountF,
      tpF = (double) tp, 
      fpF = (double) fp, 
      fnF = (double) fn,
      prec = 100 * tpF / (tpF + fpF),
      rec = 100 * tpF / (tpF + fnF),
      f = 2 * prec * rec / (prec + rec);
      
    out.println(String.format(
      "%25s %10s : %.1f / %.1f / %.1f [G = %.2f, P = %.2f]", 
      getEvalName(), getName(), prec, rec, f, goldLenAvg, predLenAvg));
  }

  public void writeSummaryWithCounts(PrintStream out) {
    int 
    tp = sum(getTPcounts()),
    fp = sum(getFPcounts()),
    fn = sum(getFNcounts());
  
  double
    tpF = (double) tp, 
    fpF = (double) fp, 
    fnF = (double) fn,
    prec = 100 * tpF / (tpF + fpF),
    rec = 100 * tpF / (tpF + fnF),
    f = 2 * prec * rec / (prec + rec);
  
  out.println(String.format(
      "%25s %10s : %.1f / %.1f / %.1f ( %6d / %6d / %6d )", 
      getEvalName(), getName(), prec, rec, f, tp, fp, fn));
  }

  public void writeSummaryWithCountsAndLen(PrintStream out) {
    int 
    tp = sum(getTPcounts()),
    fp = sum(getFPcounts()),
    fn = sum(getFNcounts()),
    goldCount = tp + fn,
    predCount = tp + fp,
    goldLen = getTPlen() + getFNlen(),
    predLen = getTPlen() + getFPlen();
  
  double
    goldCountF = (double) goldCount,
    predCountF = (double) predCount,
    goldLenF = (double) goldLen,
    predLenF = (double) predLen,
    goldLenAvg = goldLenF / goldCountF,
    predLenAvg = predLenF / predCountF,
    tpF = (double) tp, 
    fpF = (double) fp, 
    fnF = (double) fn,
    prec = 100 * tpF / (tpF + fpF),
    rec = 100 * tpF / (tpF + fnF),
    f = 2 * prec * rec / (prec + rec);
    
  out.println(String.format(
    "%25s %10s : %.1f / %.1f / %.1f ( %6d / %6d / %6d ) [G = %.2f, P = %.2f]", 
    getEvalName(), getName(), prec, rec, f, tp, fp, fn, goldLenAvg, predLenAvg));
  }

  private int sum(int[][] is) {
    int s = 0;
    for (int[] a: is) s += sum(a);
    return s;
  }

  private int sum(int[] a) {
    int s = 0;
    for (int n: a) s += n;
    return s;
  }
}
