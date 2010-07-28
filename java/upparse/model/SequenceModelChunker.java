package upparse.model;

import java.io.*;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class SequenceModelChunker {

  private double lastPerplex = 0;
  private int currIter = 0;
  private final SequenceModel model;
  private final double emdelta;
  
  public SequenceModelChunker(
      final SequenceModel _model, final double _emdelta) {
    model = _model;
    emdelta = _emdelta;
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
  
  public boolean anotherIteration() {
    final double curr = model.currPerplex();
    return emdelta < Math.abs(lastPerplex - curr);
  }
  
  public void updateWithEM(PrintStream verboseOut) {
    currIter++;
    lastPerplex = model.currPerplex();
    model.emUpdateFromTrain();
    if (verboseOut != null)
      verboseOut.println(
          String.format(
              "Current iter = %d , perplex = %f", 
              currIter, model.currPerplex()));
    
  }

  public int getCurrentIter() {
    return currIter;
  }
}
