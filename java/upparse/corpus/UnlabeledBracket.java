package upparse.corpus;

/**
 * Data structure representing a bracket, not including a category label
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class UnlabeledBracket {
  
  private final int first, last;

  public UnlabeledBracket(final int _first, final int _last) {
    first = _first;
    last = _last;
  }

  @Override
  public boolean equals(Object obj) {
    final UnlabeledBracket b = (UnlabeledBracket) obj;
    return first == b.first && last == b.last;
  }

  @Override
  public int hashCode() {
    return 37 * first + last;
  }

  public int getFirst() {
    return first;
  }
  
  public int getLast() {
    return last;
  }
}
