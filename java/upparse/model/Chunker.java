package upparse.model;

import upparse.corpus.*;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public interface Chunker {

  ChunkedSegmentedCorpus getChunkedCorpus(StopSegmentCorpus c);
}
