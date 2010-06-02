package upparse;

import java.util.*;

/**
 * Simple structure for counting arrays of int
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class NgramCounts {
  private static class Key {
    private int[] _a;

    Key(int[] a) { _a = a; }

    @Override
    public boolean equals(Object obj) {
      if (obj.getClass() != this.getClass())  return false;
      return Arrays.equals(_a, ((Key)obj)._a);
    }

    @Override
    public int hashCode() { return Arrays.hashCode(_a); }
  }
  
  private final Map<Key, Double> map = new HashMap<Key, Double>();
  
  public double get(int... a) { return _get(key(a)); }
  
  private double _get(Key k) {
    Double d = map.get(k);
    if (d == null) return 0.;
    else return d;
  }
  
  public void incr(int... a) {
    Key k = key(a);
    double curr = _get(k);
    map.put(k, curr+1.);
  }
  
  private Key key(int[] a) { return new Key(a); }
}
