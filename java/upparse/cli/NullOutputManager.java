package upparse.cli;

import java.io.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class NullOutputManager extends OutputManager {
  
  private static NullOutputManager INSTANCE = new NullOutputManager();
  
  private NullOutputManager() { }

  public static OutputManager instance() {
    return INSTANCE;
  }

  @Override
  public boolean isNull() { return true; }

  @Override
  public PrintStream getResultsStream() { return System.out; }
  
  @Override
  public void closeAll() { }
}
