package upparse.util;

import static java.lang.Math.*;

/**
 * Static math utilities 
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class Util {

  public static double sum(final double[] ds) {
    double s = 0;
    for (double d: ds)
      s += d;
    return s;
  }

  public static double logadd(final double x, final double y) {
    assert !Double.isNaN(y);
    assert !Double.isNaN(x);
    if (x <= Util.LOGEPS) 
      return y;
    else if (y <= Util.LOGEPS) 
      return x;
    else if (y <= x) 
      return x + log(1 + exp(y-x)); 
    else 
      return y + log(1 + exp(x-y));
  }

  static final double LOGEPS = -1e+16;

  public static double sum(double[][] ds) {
    double s = 0;
    for (double[] a: ds)
      for (double b: a)
        s += b;
    return s;
  }

  public static double countNonzero(double[] ds) {
    int c = 0;
    for (double d: ds) if (d != 0) c++;
    return (double) c;
  }
}
