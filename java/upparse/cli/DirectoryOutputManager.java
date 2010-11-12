package upparse.cli;

import java.io.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class DirectoryOutputManager extends OutputManager {

  private final String dir;
  private final PrintStream resultsStream;
  
  private DirectoryOutputManager(final String dirName) throws CommandLineError {
    dir = dirName;
    File f = new File(dir);
    if (f.exists()) 
      throw new CommandLineError("Output directory already exists: " + dir);

    f.mkdirs();
    try {
      resultsStream = 
        new PrintStream(new File(dir + File.separator + "RESULTS"));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(
          "This is weird: Directory should have been created: " + dir);
    }
  }

  @Override
  public boolean isNull() { return false; }

  @Override
  public void closeAll() {
    resultsStream.close();
  }

  @Override
  public PrintStream getResultsStream() { return resultsStream; }
}
