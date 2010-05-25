package upparse.tests;

import org.junit.*;

import upparse.*;
import static org.junit.Assert.*;
import static upparse.SimpleBIOEncoder.*;

/**
 * Unit tests for HMM BIO encoding methods
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class BIOEncodingTests {

  @Test
  public void testSimpleBIOEncoding() {
    
    Alpha alpha = new Alpha();

    int 
      startC = alpha.getCode("__start__"),
      theC = alpha.getCode("the"),
      quickC = alpha.getCode("quick"),
      brownC = alpha.getCode("brown"),
      foxC = alpha.getCode("fox"),
      jumpedC = alpha.getCode("jumped"),
      overC = alpha.getCode("over"),
      lazyC = alpha.getCode("lazy"),
      dogC = alpha.getCode("dog"),
      eosC = alpha.getCode("__eos__");
    
    int[] tokens = 
      new int[] { startC, theC, quickC, brownC, foxC, jumpedC, overC, theC, 
                  lazyC, dogC, eosC };
    
    int[] output =
      new int[] { STOP_STATE, B_STATE, I_STATE, I_STATE, I_STATE, O_STATE, 
        O_STATE, B_STATE, I_STATE, I_STATE, STOP_STATE };
    
    ClumpedCorpus expClumpedCorpus = 
      ClumpedCorpus.fromArrays(
          new int[][][][] { { { { theC, quickC, brownC, foxC }, 
                                { jumpedC }, 
                                { overC }, 
                                { theC, lazyC, dogC } } } },
          alpha);
    
    SimpleBIOEncoder encoder = new SimpleBIOEncoder("__stop__", alpha);
    ClumpedCorpus clumpedCorpusOutput = 
      encoder.clumpedCorpusFromBIOOutput(tokens, output);
    
    assertClumpedCorporaEq(expClumpedCorpus, clumpedCorpusOutput);
  }
   
  @Test public void testNestedBIOEncoding() {
    
    Alpha alpha = new Alpha();

    int 
      startC = alpha.getCode("__start__"),
      onC = alpha.getCode("on"),
      sundayC = alpha.getCode("sunday"),
      stopC = alpha.getCode("__stop__"),
      theC = alpha.getCode("the"),
      grizzlyC = alpha.getCode("grizzly"),
      bearC = alpha.getCode("bear"),
      sleepsC = alpha.getCode("sleeps"),
      eosC = alpha.getCode("__eos__");
    
    int[] tokens = new int[] { 
        startC, onC, sundayC, stopC, theC, grizzlyC, bearC, sleepsC, eosC };
    
    int[] output =
      new int[] { STOP_STATE, B_STATE, I_STATE, STOP_STATE, B_STATE, I_STATE, 
        I_STATE, O_STATE, STOP_STATE };
    
    ClumpedCorpus expClumpedCorpus = 
      ClumpedCorpus.fromArrays(
          new int[][][][] { { { { onC, sundayC } },
                              { { theC, grizzlyC, bearC },
                                { sleepsC } } } },
          alpha);
    
    SimpleBIOEncoder encoder = new SimpleBIOEncoder("__stop__", alpha);
    ClumpedCorpus clumpedCorpusOutput = 
      encoder.clumpedCorpusFromBIOOutput(tokens, output);
    
    assertClumpedCorporaEq(expClumpedCorpus, clumpedCorpusOutput);
  }

  public void assertClumpedCorporaEq(
      ClumpedCorpus expected, ClumpedCorpus output) {
    
    int[][][][] exp = expected.getArrays(), outp = output.getArrays();
    
    assertEquals(exp.length, outp.length);
    for (int i = 0; i < outp.length; i++) {
      assertEquals(exp[i].length, outp[i].length);
      
      for (int j = 0; j < outp[i].length; j++) {
        assertEquals(exp[i][j].length, outp[i][j].length);
        
        for (int k = 0; k < outp[i][j].length; k++) {
          assertArrayEquals(exp[i][j][k], outp[i][j][k]);
        }
      }
    }
  }
}