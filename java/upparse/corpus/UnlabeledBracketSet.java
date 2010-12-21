package upparse.corpus;

import java.util.*;

/**
 * A set of unlabeled brackets representing a syntax tree without category
 * labels
 * 
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class UnlabeledBracketSet {

  private final Alpha alpha;
  private final int[] tokens;
  private final List<List<UnlabeledBracket>> firstInd, lastInd;
  private final Set<UnlabeledBracket> brackets;

  public UnlabeledBracketSet(final int[] tokensI,
      final Collection<UnlabeledBracket> _brackets, final Alpha _alpha) {
    this(tokensI, _brackets, _alpha, true);
  }

  public UnlabeledBracketSet(final int[] _tokens,
      final Collection<UnlabeledBracket> _brackets, final Alpha _alpha,
      final boolean countRoot) {

    alpha = _alpha;
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

    brackets = new HashSet<UnlabeledBracket>();
    for (UnlabeledBracket b : _brackets)
      addBracket(b);
  }

  private void addBracket(UnlabeledBracket b) {
    if (tokens.length == 0 && b.getFirst() == 0 && b.getLast() == 0)
      return;
    if (!firstInd.get(b.getFirst()).contains(b)) {
      brackets.add(b);
      firstInd.get(b.getFirst()).add(b);
      lastInd.get(b.getLast() - 1).add(b);
    }
  }

  public Set<UnlabeledBracket> getBrackets() {
    return brackets;
  }

  public int[] getTokens() {
    return tokens;
  }

  private List<String> tokenParenList() {
    final List<String> l = new ArrayList<String>();
    for (int i = 0; i < tokens.length; i++) {
      for (int j = 0; j < firstInd.get(i).size(); j++)
        l.add("(");
      l.add("_");
      for (int j = 0; j < lastInd.get(i).size(); j++)
        l.add(")");
    }
    return l;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer();
    for (int i = 0; i < tokens.length; i++) {
      for (int j = 0; j < firstInd.get(i).size(); j++)
        sb.append("(");
      sb.append(alpha.getString(tokens[i]));
      for (int j = 0; j < lastInd.get(i).size(); j++)
        sb.append(")");
      sb.append(" ");
    }
    return sb.toString().trim();
  }

  public static UnlabeledBracketSet fromString(String string, Alpha alpha) {
    if (string.trim().isEmpty())
      return new UnlabeledBracketSet(new int[0],
          new ArrayList<UnlabeledBracket>(), alpha);
    return fromTokens(string.replaceAll("\\(", "( ").replaceAll("\\)", " )")
        .split(" +"), alpha);
  }

  public static UnlabeledBracketSet fromTokens(String[] items, Alpha alpha) {
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

    int[] tokensI = new int[tokens.size()];
    for (int i = 0; i < tokensI.length; i++)
      tokensI[i] = alpha.getCode(tokens.get(i));
    return new UnlabeledBracketSet(tokensI, brackets, alpha);
  }

  public int[][] clumps() {
    if (tokens.length == 0) 
      return new int[0][];
    
    int n = 0;
    int lastOpen = -1;
    final List<Integer> open = new ArrayList<Integer>(), closed = new ArrayList<Integer>();
    for (String item : tokenParenList()) {
      if (item.equals("("))
        lastOpen = n;

      else if (item.equals(")")) {
        if (lastOpen != -1) {
          open.add(lastOpen);
          closed.add(n);
          lastOpen = -1;
        }
      }

      else
        n++;
    }

    assert n == tokens.length;
    assert open.size() == closed.size();
    assert open.get(0) >= 0;
    for (int k = 0; k < open.size() - 1; k++)
      assert closed.get(k) <= open.get(k + 1);
    assert closed.get(closed.size() - 1) <= tokens.length;

    int i = 0, j = 0;
    final List<Integer> openC = new ArrayList<Integer>(), closeC = new ArrayList<Integer>();
    while (i < n) {
      if (j < open.size() && open.get(j) == i) {
        assert i == 0 || i == closeC.get(closeC.size() - 1);
        openC.add(i);
        i = closed.get(j++);
        assert i <= tokens.length;
        closeC.add(i);
      }

      else {
        assert i == 0 || i == closeC.get(closeC.size() - 1);
        openC.add(i++);
        assert i <= tokens.length;
        closeC.add(i);
      }
    }

    assert openC.size() == closeC.size();
    assert openC.get(0).intValue() == 0;
    for (int l = 0; l < openC.size() - 1; l++)
      assert closeC.get(l).intValue() == openC.get(l + 1).intValue();
    assert closeC.get(closeC.size() - 1).intValue() == tokens.length;

    int[][] clumps = new int[openC.size()][];

    int m = 0;
    for (int k = 0; k < openC.size(); k++) {
      final int start = openC.get(k), end = closeC.get(k), len = end - start;
      int[] clump = new int[len];
      for (int l = 0; l < len; l++)
        clump[l] = tokens[start + l];
      clumps[m++] = clump;
    }
    return clumps;
  }

  public static UnlabeledBracketSet emptySet(final Alpha alpha,
      final int[] tokens, final boolean countRoot) {
    return new UnlabeledBracketSet(tokens, new ArrayList<UnlabeledBracket>(),
        alpha, countRoot);
  }

  public static UnlabeledBracketSet emptySet(Alpha alpha2, int[] tokens2) {
    return emptySet(alpha2, tokens2, false);
  }

  public void appendNonSingletonBrackets(Collection<UnlabeledBracket> bs) {
    for (UnlabeledBracket b : bs)
      if (b.len() > 1)
        addBracket(b);
  }

  public void appendRootBracket() {
    addBracket(new UnlabeledBracket(0, tokens.length));
  }

  public UnlabeledBracketSet copy() {
    final UnlabeledBracketSet copy = emptySet(alpha, tokens);
    copy.appendNonSingletonBrackets(brackets);
    return copy;
  }
}
