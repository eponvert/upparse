package upparse.model;

import java.io.*;

import upparse.corpus.*;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class SequenceModelChunker {

  private double lastPerplex = -1;
  private int currIter = 0;
  private final SequenceModel model;
  private final double emdelta;
  public final int totalIter;
  
  public SequenceModelChunker(
      final SequenceModel _model, final double _emdelta) {
    this(_model, _emdelta, -1);
  }
  
  public SequenceModelChunker(
      final SequenceModel _model, final double _emdelta, final int _iter) {
    model = _model;
    emdelta = _emdelta;
    totalIter = _iter;
  }
  
  public Chunker getCurrentChunker() {
    return new Chunker() {
      
      @Override
      public ChunkedSegmentedCorpus getChunkedCorpus(StopSegmentCorpus c) 
      throws ChunkerError{
        try {
          return model.tagCC(model.getEncoder().tokensFromStopSegmentCorpus(c));
        } catch (SequenceModelError e) {
          throw new ChunkerError(e);
        } catch (EncoderError e) {
          throw new ChunkerError(e);
        }
      }
    };
  }
  
  private double currDelta() {
    if (lastPerplex < 0)
      return model.currPerplex();
    
    final double 
    curr = model.currPerplex(),
    less = lastPerplex - curr,
    delta = less / curr;
    return delta;
  }
  
  public boolean anotherIteration() {
    if (totalIter == 0) 
      return false;
    if (totalIter > 0)
      return currIter < totalIter && (lastPerplex < 0 || emdelta < currDelta());
    else
      return lastPerplex < 0 || emdelta < currDelta();
  }
  
  public void updateWithEM(PrintStream verboseOut) {
    currIter++;
    lastPerplex = model.currPerplex();
    model.emUpdateFromTrain();
    if (verboseOut != null)
      verboseOut.println(
          String.format(
              "Current iter = %d , perplex = %f, delta = %f", 
              currIter, model.currPerplex(), currDelta()));
    
  }

  public int getCurrentIter() {
    return currIter;
  }
}
