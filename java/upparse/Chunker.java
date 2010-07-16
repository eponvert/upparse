package upparse;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public interface Chunker {

  ChunkedSegmentedCorpus getChunkedCorpus(StopSegmentCorpus c);
}
