package upparse.util;

import java.util.*;

/**
 * A wrapper for easy create of pipeline style chains of iterables
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public abstract class Pipe<S,T> implements Iterable<T> {
  
  private final Iterable<S> fromIter;

  public Pipe(Iterable<S> fromIter) {
    this.fromIter = fromIter;
  }
  
  public abstract T getNext(Iterator<S> iter);

  @Override
  public final Iterator<T> iterator() {
    return new PipeIterator<S, T>(fromIter.iterator()) {
      @Override
      public T getNext(Iterator<S> fromIter) {
        return Pipe.this.getNext(fromIter);
      }
    };
  }
}
