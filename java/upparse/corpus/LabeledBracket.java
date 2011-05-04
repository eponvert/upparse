package upparse.corpus;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class LabeledBracket {

  private final int first, last;
  private final String label;

  public LabeledBracket(int _first, int _last, String _label) {
    first = _first;
    last = _last;
    label = _label;
  }
  
  @Override
  public String toString() { 
    return String.format("%s(%d,%d)", label, first, last); 
  }



  public UnlabeledBracket unlabeled() {
    return new UnlabeledBracket(first, last);
  }
  
  public String getLabel() {
    return label;
  }
  
  public int length() { 
    return last - first;
  }
  
  public int getFirst() {
    return first;
  }
  
  public int getLast() {
    return last;
  }
}
