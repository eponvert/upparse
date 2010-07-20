package upparse.corpus;

import java.util.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public abstract class CorpusTreeIter implements Iterable<LabeledBracketSet> {

  private final Iterable<String> strIter;

  public CorpusTreeIter(Iterable<String> _strIter) {
    strIter = _strIter;
  }

  public Iterable<UnlabeledBracketSet> toUnlabeledIter() {
    return new Iterable<UnlabeledBracketSet>() {
      
      @Override
      public Iterator<UnlabeledBracketSet> iterator() {
        final Iterator<LabeledBracketSet> iter = 
          CorpusTreeIter.this.iterator();
        return new Iterator<UnlabeledBracketSet>() {
          
          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
          
          @Override
          public UnlabeledBracketSet next() {
            return iter.next().unlabeled();
          }
          
          @Override
          public boolean hasNext() {
            return iter.hasNext();
          }
        };
      }
    };
  }

  @Override
  public Iterator<LabeledBracketSet> iterator() {
    return new Iterator<LabeledBracketSet>() {
      
      final Iterator<String> iterator = strIter.iterator();
      
      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
      
      @Override
      public LabeledBracketSet next() {
        return LabeledBracketSet.fromString(iterator.next());
      }
      
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }
    };
  }


}
