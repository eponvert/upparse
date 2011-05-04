package upparse.corpus;

import java.util.*;

/**
 * Simple alphabet data structure
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class Alpha {
  
  private final Map<String, Integer> stoi = new HashMap<String, Integer>(50000);
  private final List<String> itos = new ArrayList<String>();
  
  public int getCode(String term) {
    if (stoi.containsKey(term))
      return stoi.get(term);
    
    assert term.length() != 0;
    
    int code = itos.size();
    stoi.put(term, code);
    itos.add(term);
    return code;
  }
  
  public String getString(int code) {
    return itos.get(code);
  }

  public int size() {
    return itos.size();
  }
}
