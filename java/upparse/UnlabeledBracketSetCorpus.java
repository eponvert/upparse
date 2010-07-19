package upparse;

import java.util.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class UnlabeledBracketSetCorpus { 
  
  private final UnlabeledBracketSet[] trees;

  private UnlabeledBracketSetCorpus(UnlabeledBracketSet[] _trees) {
    trees = _trees;
  }

  public UnlabeledBracketSetCorpus filterBySentenceLength(int n) {
    List<Integer> indices = new ArrayList<Integer>();
    for (int i = 0; i < trees.length; i++) 
      if (trees[i].getTokens().length <= n)
        indices.add(i);

    UnlabeledBracketSet[] newTrees = new UnlabeledBracketSet[indices.size()];
    int i = 0;
    for (Integer j: indices)
      newTrees[i++] = trees[j];
    
    return new UnlabeledBracketSetCorpus(newTrees);
  }

  public static UnlabeledBracketSetCorpus fromTreeIter(Iterable<UnlabeledBracketSet> iter) {
    List<UnlabeledBracketSet> trees = new ArrayList<UnlabeledBracketSet>();
    for (UnlabeledBracketSet tree: iter)
      trees.add(tree);
    return new UnlabeledBracketSetCorpus(
        trees.toArray(new UnlabeledBracketSet[0]));
  }
}
