package upparse.corpus;

import java.io.*;
import java.util.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class UnlabeledBracketSetCorpus 
implements Iterable<UnlabeledBracketSet> { 
  
  private final UnlabeledBracketSet[] trees;

  private UnlabeledBracketSetCorpus(UnlabeledBracketSet[] _trees) {
    trees = _trees;
  }
  
  public UnlabeledBracketSet[] getTrees() { return trees; }

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
  
  public int size() {
    return trees.length;
  }
  
  public UnlabeledBracketSet get(int i) {
    return trees[i];
  }

  public static UnlabeledBracketSetCorpus fromArrays(
      final UnlabeledBracketSet[] a) {
    return new UnlabeledBracketSetCorpus(a);
  }

  public void writeTo(String output) throws IOException {
    PrintWriter writer = new PrintWriter(new BufferedWriter(
        new FileWriter(new File(output))));
    for (int i = 0; i < trees.length; i++)
      writer.println(trees[i].toString());
    writer.close();
  }

  @Override
  public Iterator<UnlabeledBracketSet> iterator() {
    return new Iterator<UnlabeledBracketSet>() {
      
      int i = 0;
      
      @Override
      public void remove() { throw new UnsupportedOperationException(); }
      
      @Override
      public UnlabeledBracketSet next() { return trees[i++]; }
      
      @Override
      public boolean hasNext() { return i < trees.length; }
    };
  }
}
