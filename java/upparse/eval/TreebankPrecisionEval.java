package upparse.eval;

import java.io.*;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class TreebankPrecisionEval extends TreebankEval {
  
  private TreebankPrecisionEval() { 
  }

  @Override
  public void eval(String string, ChunkedSegmentedCorpus outputCorpus) 
  throws EvalError {
    // TODO Auto-generated method stub
  }
  
  public static TreebankPrecisionEval fromUnlabeledBracketSets(
      final UnlabeledBracketSetCorpus tree, boolean checkTerms) {
    // TODO
    return null;
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void writeSummary(String evalType, PrintStream out, boolean onlyLast)
      throws EvalError {
    // TODO Auto-generated method stub
    
  }
}
