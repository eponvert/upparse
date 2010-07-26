package upparse.eval;

import java.io.*;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class TreebankFlatEval extends TreebankEval {
  
  private TreebankFlatEval() {
    
  }

  @Override
  public void eval(String string, ChunkedSegmentedCorpus outputCorpus) 
  throws EvalError {
    // TODO Auto-generated method stub

  }

  public static Eval fromUnlabeledBracketSets(
      UnlabeledBracketSetCorpus unlabeledBracketSetCorpus, boolean checkTerms) {
    // TODO Auto-generated method stub
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
