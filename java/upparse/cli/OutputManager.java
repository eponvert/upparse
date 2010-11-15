package upparse.cli;

import java.io.*;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public abstract class OutputManager {
  
  protected boolean outputAllIter = false;
  protected OutputType outputType = OutputType.CLUMP;
  
  /**
   * @param filename The name of the directory for system output
   */
  public static OutputManager fromDirname(final String filename) throws CommandLineError {
    return DirectoryOutputManager.fromDirname(filename);
  }

  public static OutputManager nullOutputManager() {
    return NullOutputManager.instance();
  }

  public abstract boolean isNull();

  public void setOutputAllIter(boolean outputAll) { outputAllIter = outputAll; }
  
  public abstract PrintStream getResultsStream();
  public abstract PrintStream getStatusStream();
  
  public abstract void closeAll();

  public void setOutputType(OutputType type) { outputType  = type; }

  public abstract void writeOutput() throws IOException, CorpusError;

  public abstract void addChunkerOutput(ChunkedSegmentedCorpus chunkerOutput);
  
  public void setOutputAll(boolean b) { outputAllIter = b; }
}

