package upparse.cli;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

import upparse.corpus.ChunkedSegmentedCorpus;
import upparse.corpus.CorpusError;

public class StdoutOutputManager extends OutputManager {

  private ChunkedSegmentedCorpus lastOutput = null;
  private String[][] outputText = null;
  private static StdoutOutputManager INSTANCE = new StdoutOutputManager();  
  
  private StdoutOutputManager() {
  }
  
  public static StdoutOutputManager instance() {
    return INSTANCE;
  }

  @Override
  public boolean isNull() {
      return false;
  }

  @Override
  public PrintStream getResultsStream() {
    return System.err;
  }

  @Override
  public PrintStream getStatusStream() {
    return System.err;
  }

  @Override
  public void closeAll() {
  }

  @Override
  public void writeOutput() throws IOException, CorpusError {
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));
    lastOutput.writeTo(bw, outputType, outputText);
  }

  @Override
  public void addChunkerOutput(ChunkedSegmentedCorpus chunkerOutput,
      String fname) {
    if (lastOutput != null)
      getStatusStream().println(
          "WARING only the last chunker output will be printed");
    lastOutput = chunkerOutput;
  }

  @Override
  public void writeMetadata(Main prog) throws IOException {
  }

  @Override
  public String treeOutputFilename() {
    return null;
  }

  @Override
  public String clumpsOutputFilename() {
    return null;
  }

  @Override
  public void useOutputText(String[][] testPos) {
    outputText = testPos;
  }
}
