package upparse.cli;

import java.io.*;
import java.util.*;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class DirectoryOutputManager extends OutputManager {

  private final String dir;
  private final PrintStream resultsStream;
  private List<ChunkedSegmentedCorpus>  writeOutput = 
    new ArrayList<ChunkedSegmentedCorpus>();
  private int numIter = 0;
  private final PrintStream statusStream;
  
  private DirectoryOutputManager(final String dirName) throws CommandLineError {
    dir = dirName;
    File f = new File(dir);
    if (f.exists()) 
      throw new CommandLineError("Output directory already exists: " + dir);

    f.mkdirs();
    try {
      resultsStream = 
        new PrintStream(new File(dir + File.separator + "RESULTS"));
      statusStream = 
        new PrintStream(new File(dir + File.separator + "STATUS"));
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

  @Override
  public PrintStream getStatusStream() { return statusStream; }

  @Override
  public void addChunkerOutput(ChunkedSegmentedCorpus chunkerOutput) {
    numIter++;
    if (outputAllIter || writeOutput.isEmpty())
      writeOutput.add(chunkerOutput);
    else 
      writeOutput.set(0, chunkerOutput);
  }

  @Override
  public void writeOutput() throws CorpusError, IOException {
    if (writeOutput.size() == 1)
      writeOutput.get(0).writeTo(lastChunkerIterOutputFname(), outputType);
    else
      for (int i = 0; i < writeOutput.size(); i++)
        writeOutput.get(i).writeTo(getChunkerIterOutputFname(i), outputType);
  }

  private String getChunkerIterOutputFname(int i) {
    int _i = i+1;
    return dir + File.separator + "I" + _i + (_i == numIter ? "_last" : "");
  }

  private String lastChunkerIterOutputFname() {
    return dir + File.separator + "I" + numIter + "_last";
  }
  
  public static OutputManager fromDirname(final String filename) 
  throws CommandLineError {
    return new DirectoryOutputManager(filename);
  }

  @Override
  public void writeMetadata(Main prog) throws IOException {
    PrintStream metadataStream = 
      new PrintStream(new File(dir + File.separator + "README"));
    prog.writeMetadata(metadataStream);
    metadataStream.close();
  }
}