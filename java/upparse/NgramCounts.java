package upparse;

import java.util.*;

/**
 * Simple structure for counting arrays of int
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class NgramCounts {
  
  private final Map<Integer, Double> map = new HashMap<Integer, Double>(50000);
  
  public double get(int... a) {
    return _get(Arrays.hashCode(a));
  }
  
  private double _get(int k) {
    Double d = map.get(k);
    return (d == null) ? 0. : d; 
  }
  
  public void incr(int... a) {
    int k = Arrays.hashCode(a);
    map.put(k, _get(k)+1.);
  }
}
