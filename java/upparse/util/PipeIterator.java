package upparse.util;

import java.util.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
P */
public abstract class PipeIterator<S, T> implements Iterator<T> {
  
  private final Iterator<S> fromIter;

  public PipeIterator(Iterator<S> fromIter) {
    this.fromIter = fromIter;
  }

  @Override
  public boolean hasNext() { return fromIter.hasNext(); }

  @Override
  public T next() { return getNext(fromIter); }

  public abstract T getNext(Iterator<S> fromIter);

  @Override
  public void remove() { throw new UnsupportedOperationException(); }
}
