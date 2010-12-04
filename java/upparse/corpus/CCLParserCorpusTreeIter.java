package upparse.corpus;

import java.util.*;

import upparse.util.*;

public class CCLParserCorpusTreeIter extends Pipe<String, UnlabeledBracketSet> {
  
  private final Alpha alpha;
  private CCLParserCorpusTreeIter(Alpha a, Iterable<String> treeStrIter) {
    super(treeStrIter);
    alpha = a;
  }

  @Override
  public UnlabeledBracketSet getNext(Iterator<String> iter) {
    return UnlabeledBracketSet.fromString(iter.next(), alpha);
  }

  public static Iterable<UnlabeledBracketSet> fromFiles(String[] files,
      Alpha alpha) {
    return new CCLParserCorpusTreeIter(
        alpha, CCLParserTreeStringIter.fromFiles(files));
  }
}
