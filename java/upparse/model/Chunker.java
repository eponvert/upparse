package upparse.model;

import upparse.corpus.*;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public interface Chunker {

  ChunkedSegmentedCorpus getChunkedCorpus(StopSegmentCorpus c) throws ChunkerError;
}
