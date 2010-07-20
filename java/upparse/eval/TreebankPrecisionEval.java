package upparse.eval;

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
      final UnlabeledBracketSetCorpus tree) {
    // TODO
    return null;
  }

}
