package upparse.corpus;

public class SimpleBEOEncoder extends SimpleBIOEncoder {

  private static boolean[][] CONSTRAINTS = new boolean[4][4];
  static {
    CONSTRAINTS[STOP_STATE][STOP_STATE] = false;
    CONSTRAINTS[STOP_STATE][B_STATE] = false;
    CONSTRAINTS[STOP_STATE][I_STATE] = true;
    CONSTRAINTS[STOP_STATE][O_STATE] = false;
    CONSTRAINTS[B_STATE][STOP_STATE] = true;
    CONSTRAINTS[B_STATE][B_STATE] = true;
    CONSTRAINTS[B_STATE][I_STATE] = false;
    CONSTRAINTS[B_STATE][O_STATE] = true;
    CONSTRAINTS[I_STATE][STOP_STATE] = false;
    CONSTRAINTS[I_STATE][B_STATE] = false;
    CONSTRAINTS[I_STATE][I_STATE] = true;
    CONSTRAINTS[I_STATE][O_STATE] = false;
    CONSTRAINTS[O_STATE][STOP_STATE] = false;
    CONSTRAINTS[O_STATE][B_STATE] = false;
    CONSTRAINTS[O_STATE][I_STATE] = true;
    CONSTRAINTS[O_STATE][O_STATE] = false;
  }

  public SimpleBEOEncoder(String stop, Alpha alpha) {
    super(stop, alpha);
  }

  @Override
  public boolean[][] constraints() {
    return CONSTRAINTS;
  }
}
