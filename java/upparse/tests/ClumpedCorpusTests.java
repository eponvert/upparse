package upparse.tests;

import java.util.*;

import org.junit.*;

import upparse.*;

import static org.junit.Assert.*;


/**
 * Testing clumped corpus string output etc
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class ClumpedCorpusTests {

  @Test public void testToString() {
    Alpha alpha = new Alpha();

    int 
      theC = alpha.getCode("the"),
      quickC = alpha.getCode("quick"),
      brownC = alpha.getCode("brown"),
      foxC = alpha.getCode("fox"),
      jumpedC = alpha.getCode("jumped"),
      overC = alpha.getCode("over"),
      lazyC = alpha.getCode("lazy"),
      dogC = alpha.getCode("dog"),
      onC = alpha.getCode("on"),
      sundayC = alpha.getCode("sunday"),
      grizzlyC = alpha.getCode("grizzly"),
      bearC = alpha.getCode("bear"),
      sleepsC = alpha.getCode("sleeps");
    
    int[][][][] corpusArrays =
      new int[][][][] { { { { theC, quickC, brownC, foxC }, 
                            { jumpedC }, 
                            { overC }, 
                            { theC, lazyC, dogC } } },
                        { { { onC, sundayC } },
                          { { theC, grizzlyC, bearC },
                            { sleepsC } } } };
    
    ChunkedSegmentedCorpus corpus = ChunkedSegmentedCorpus.fromArrays(corpusArrays, alpha);
    
    Iterator<String> iter = corpus.strIter().iterator();

    assertTrue(iter.hasNext());
    
    String s = iter.next();
    
    assertEquals("(the quick brown fox) jumped over (the lazy dog)", s);
    
    assertTrue(iter.hasNext());
    
    s = iter.next();
    
    assertEquals("(on sunday) (the grizzly bear) sleeps", s);
    
    assertFalse(iter.hasNext());
  }
  
  @Test public void testSentenceWithSingleWordSegment() {
    Alpha alpha = new Alpha();

    int 
      dummyC = alpha.getCode("dummy"),
      sentenceC = alpha.getCode("sentence"),
      theC = alpha.getCode("the"),
      asbestosC = alpha.getCode("asbestos"),
      fiberC = alpha.getCode("fiber"),
      crocidoliteC = alpha.getCode("crocidolite"),
      isC = alpha.getCode("is"),
      unusuallyC = alpha.getCode("unusually"),
      resilientC = alpha.getCode("resilient"),
      onceC = alpha.getCode("once"),
      itC = alpha.getCode("it"),
      entersC = alpha.getCode("enters"),
      lungsC = alpha.getCode("lungs");

    int[][][][] corpusArrays =
      new int[][][][]
          { { { { dummyC }, 
                { sentenceC } } },
            { { { theC, asbestosC, fiberC }, },
              { { crocidoliteC } },
              { { isC },
                { unusuallyC },
                { resilientC },
                { onceC },
                { itC },
                { entersC },
                { theC, lungsC } } },
            { { { dummyC },
                { sentenceC } } } };

    ChunkedSegmentedCorpus corpus = ChunkedSegmentedCorpus.fromArrays(corpusArrays, alpha);
    
    Iterator<String> iter = corpus.strIter().iterator();
    assertTrue(iter.hasNext());
    String s = iter.next();
    assertEquals("dummy sentence", s);
    assertTrue(iter.hasNext());
    s = iter.next();
    assertEquals(
        "(the asbestos fiber) crocidolite is unusually resilient once it enters (the lungs)", 
        s);
    assertTrue(iter.hasNext());
    s = iter.next();
    assertEquals("dummy sentence", s);
    assertFalse(iter.hasNext());

  }
}
