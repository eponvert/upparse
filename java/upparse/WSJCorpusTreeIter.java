package upparse;

import java.util.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class WSJCorpusTreeIter implements Iterable<LabeledBracketSet> {
  
  private final Iterable<String> strIter;

  public WSJCorpusTreeIter(WSJCorpusTreeStringIter _strIter) {
    strIter = _strIter;
  }

  public static WSJCorpusTreeIter fromFiles(String[] files) {
    return fromStrIter(new WSJCorpusTreeStringIter(files));
  }

  public static WSJCorpusTreeIter fromStrIter(WSJCorpusTreeStringIter strIter) {
    return new WSJCorpusTreeIter(strIter);
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
