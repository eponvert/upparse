package upparse;

import java.util.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class NgramCounts {
  
  private final Map<Integer, Double> map = new HashMap<Integer, Double>();
  
  public double get(int... a) {
    return _get(key(a));
  }
  
  private double _get(int k) {
    Double d = map.get(k);
    return (d == null) ? 0. : d; 
  }
  
  public void incr(int... a) {
    int k = key(a);
    map.put(k, _get(k)+1.);
  }
  
  public int key(int... a) {
    int key = 1;
    for (int v: a) { 
      key *= 31;
      key += v;
    }
    return key;
  }
}
