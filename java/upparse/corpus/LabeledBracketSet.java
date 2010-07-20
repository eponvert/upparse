package upparse.corpus;

import java.util.*;

/**
 * Labeled bracket set, which represents a tree
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class LabeledBracketSet {
  
  private final String[] tokens;
  private final List<LabeledBracket> brackets;
  private final List<List<LabeledBracket>> firstInd, lastInd;
  private final String[] pos;

  private LabeledBracketSet(
      final String[] _tokens, final List<LabeledBracket> _brackets) {
    tokens = _tokens;
    brackets = _brackets;
    
    firstInd = new ArrayList<List<LabeledBracket>>();
    lastInd = new ArrayList<List<LabeledBracket>>();
    
    for (int i = 0; i < tokens.length; i++) {
      firstInd.add(new ArrayList<LabeledBracket>());
      lastInd.add(new ArrayList<LabeledBracket>());
    }
    
    for (LabeledBracket b: brackets) {
      firstInd.get(b.getFirst()).add(b);
      lastInd.get(b.getLast()-1).add(b);
    }

    pos = new String[tokens.length];
    
    // The FIRST length 1 bracket stored in the list will correspond to the POS
    for (LabeledBracket b: brackets)
      if (b.length() == 1 && pos[b.getFirst()] == null)
        pos[b.getFirst()] = b.getLabel();
}
  
  public String[] getTokens() {
    return tokens;
  }
  
  @Override
  public String toString() {
    return toString(NoTransform.instance);
  }
  
  public String toString(CorpusConstraints c) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < tokens.length; i++) {
      for (LabeledBracket b: firstInd.get(i))
        if (b.length() > 1)
          sb.append("(");
      sb.append(c.getToken(tokens[i], pos[i]));
      for (LabeledBracket b: lastInd.get(i))
        if (b.length() > 1)
          sb.append(")");
      sb.append(" ");
    }
    return sb.toString().trim();
  }

  public UnlabeledBracketSet unlabeled() {
    return UnlabeledBracketSet.fromString(toString());
  }
  
  public String tokenString() {
    return tokenString(NoTransform.instance);
  }
  
  public String tokenString(CorpusConstraints c) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < tokens.length; i++) {
      final String t = c.getToken(tokens[i], pos[i]);
      if (!t.equals("")) {
        sb.append(t);
        sb.append(" ");
      }
    }
    return c.wrap(sb.toString().trim());
  }
  
  public UnlabeledBracketSet unlabeled(CorpusConstraints c) {
    return UnlabeledBracketSet.fromString(toString(c));
  }
  
  public static LabeledBracketSet fromString(String next) {
    next = next.replaceAll("\\(\\(", "( (").replaceAll("\\)", " )");
    return fromTokens(next.split(" +"));
  }

  public static LabeledBracketSet fromTokens(String[] items) {
    List<String> tokens = new ArrayList<String>();
    Stack<Integer> firstIndices = new Stack<Integer>();
    Stack<String> labels = new Stack<String>();
    List<LabeledBracket> brackets = new ArrayList<LabeledBracket>();
    
    int n = 0;
    
    for (int i = 0; i < items.length; i++) {
      if (items[i].charAt(0) == '(') {
        labels.push(items[i].substring(1));
        firstIndices.push(n);
      
      } else if (items[i].charAt(0) == ')') {
        assert items[i].length() == 1;
        assert labels.size() == firstIndices.size();
        assert firstIndices.size() > 0;
        brackets.add(new LabeledBracket(firstIndices.pop(), n, labels.pop()));
        
      } else {
        tokens.add(items[i]);
        n++;
      }
    }
    return new LabeledBracketSet(tokens.toArray(new String[0]), brackets);
  }
}
