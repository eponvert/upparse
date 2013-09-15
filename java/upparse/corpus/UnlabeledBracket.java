package upparse.corpus;

/**
 * Data structure representing a bracket, not including a category label
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class UnlabeledBracket implements Comparable<UnlabeledBracket> {
  
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

  public int len() {
    return last - first;
  }

  @Override
  public int compareTo(UnlabeledBracket o) {
    if (o == null) throw new NullPointerException();
    else if (last < o.first) return -1;
    else if (o.last < first) return 1;
    else if (o.first == first && last == o.last) return 0;
    else if (o.first <= first && last <= o.last) return -1;
    else if (first <= o.first && o.last <= last) return 1;
    else return 0;
  }

  public boolean contains(UnlabeledBracket b) {
    return first <= b.first && b.last <= last;
  }

  @Override
  public String toString() {
    return String.format("[%d,%d]", first, last);
  }
}
