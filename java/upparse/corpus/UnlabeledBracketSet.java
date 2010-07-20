package upparse.corpus;

import java.util.*;

/**
 * A set of unlabeled brackets representing a syntax tree without category
 * labels
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class UnlabeledBracketSet {

  private final String[] tokens;
  private final List<List<UnlabeledBracket>> firstInd, lastInd;

  public UnlabeledBracketSet(
      final String[] _tokens, 
      final Collection<UnlabeledBracket> _brackets) {
    this(_tokens, _brackets, true);
  }
  
  public UnlabeledBracketSet(
      final String[] _tokens, 
      final Collection<UnlabeledBracket> _brackets,
      final boolean countRoot) {
    
    tokens = _tokens;
    
    if (countRoot) {
      UnlabeledBracket root = new UnlabeledBracket(0, tokens.length);
      if (!_brackets.contains(root))
        _brackets.add(root);
    }
    
    firstInd = new ArrayList<List<UnlabeledBracket>>();
    lastInd = new ArrayList<List<UnlabeledBracket>>();
    
    for (int i = 0; i < tokens.length; i++) {
      firstInd.add(new ArrayList<UnlabeledBracket>());
      lastInd.add(new ArrayList<UnlabeledBracket>());
    }
    
    for (UnlabeledBracket b: _brackets) {
      firstInd.get(b.getFirst()).add(b);
      lastInd.get(b.getLast()-1).add(b);
    }
  }

  public String[] getTokens() {
    return tokens;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer();
    for (int i = 0; i < tokens.length; i++) {
      for (int j = 0; j < firstInd.get(i).size(); j++) sb.append("(");
      sb.append(tokens[i]);
      for (int j = 0; j < lastInd.get(i).size(); j++) sb.append(")");
      sb.append(" ");
    }
    return sb.toString().trim();
  }

  public static UnlabeledBracketSet fromString(String string) {
    return fromTokens(
      string.replaceAll("\\(", "( ").replaceAll("\\)", " )").split(" +"));
  }
  
  public static UnlabeledBracketSet fromTokens(String[] items) {
    List<String> tokens = new ArrayList<String>();
    Stack<Integer> firstIndices = new Stack<Integer>();
    Set<UnlabeledBracket> brackets = new HashSet<UnlabeledBracket>();
    
    int n = 0;
    
    for (int i = 0; i < items.length; i++) {
      if (items[i].equals("(")) {
        firstIndices.push(n);
      
      } else if (items[i].equals(")")) {
        assert firstIndices.size() > 0;
        int first = firstIndices.pop();
        if (first + 1 < n)
          brackets.add(new UnlabeledBracket(first, n));
        
      } else {
        tokens.add(items[i]);
        n++;
      }
    }
    return new UnlabeledBracketSet(tokens.toArray(new String[0]), brackets);
  }
}
