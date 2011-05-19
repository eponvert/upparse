package upparse.cli;

import java.io.*;

import upparse.corpus.*;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public abstract class OutputManager {
  
  protected boolean outputAllIter = false;
  protected OutputType outputType = OutputType.CLUMP;
  
  /**
   * @param filename The name of the directory for system output
   */
  public static OutputManager fromDirname(final String filename) 
  throws CommandLineError {
    return DirectoryOutputManager.fromDirname(filename);
  }

  public static OutputManager nullOutputManager() {
    return NullOutputManager.instance();
  }
  
  public static OutputManager stdoutOutputManager() {
    return StdoutOutputManager.instance();
  }

  public abstract boolean isNull();

  public abstract PrintStream getResultsStream();
  public abstract PrintStream getStatusStream();
  
  public abstract void closeAll();

  public void setOutputType(OutputType type) { outputType  = type; }

  public abstract void writeOutput() throws IOException, CorpusError;

  public abstract void addChunkerOutput(ChunkedSegmentedCorpus chunkerOutput,
      String fname);
  
  public void setOutputAll(boolean b) { outputAllIter = b; }
  
  public abstract void writeMetadata(Main prog) throws IOException;

  public abstract String treeOutputFilename();
  
  public abstract String clumpsOutputFilename();

  public abstract void useOutputText(String[][] testPos);
}

