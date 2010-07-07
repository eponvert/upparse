package upparse;

/**
 * Simple class for managing the maximum of a set of double
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public final class MaxVals {
  int argmax = -1;
  double max = Double.NEGATIVE_INFINITY;
  public MaxVals(final double[] vals) {
    for (int i = 0; i < vals.length; i++) {
      if (vals[i] > max) {
        argmax = i;
        max = vals[i];
      }
    }
  }
  public static int arrayMax(int[] t) {
    assert t.length > 0;
    int v = t[0];
    for (int i = 1; i < t.length; i++) 
      if (t[i] > v) 
        v = t[i];
    return v;
  }
}