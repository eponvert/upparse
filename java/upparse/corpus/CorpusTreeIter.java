package upparse.corpus;

import java.util.*;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public abstract class CorpusTreeIter implements Iterable<LabeledBracketSet> {

  private final Iterable<String> strIter;
  private final Alpha alpha;

  public CorpusTreeIter(final Iterable<String> _strIter, final Alpha _alpha) {
    alpha = _alpha;
    strIter = _strIter;
  }

  public Iterable<UnlabeledBracketSet> toUnlabeledIter(
      final CorpusConstraints cc) {
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
            return iter.next().unlabeled(cc);
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
        return LabeledBracketSet.fromString(iterator.next(), alpha);
      }
      
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }
    };
  }


}
