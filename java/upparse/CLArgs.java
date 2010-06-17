package upparse;

import java.io.*;
import java.util.*;

/**
 * Simple utility for dealing with command line arguments
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class CLArgs {
  
  public String output = null;
  public String factor = "2";
  public int iter;
  public double emdelta = 1;
  public String[] args = new String[0];
  public String tagContraints = null;
  public String cmethod = null;
  public String stopv = "__stop__";
  public BIOEncoder.GPOpt grandparents = BIOEncoder.GPOpt.NOGP;
  public String goldStandards = null;
  private ChunkingEval[] evals = null;
  public String evalType = "PR"; 
  public final Alpha alpha = new Alpha();
  public boolean verbose = false;
  public String testCorpus = null;
  public double scaleFactor = 1;
  public double scaleFactor2 = 1;
  public boolean checkTerms = true;

  public double[] getFactor() {
    String[] fpieces = factor.split(",");

    double[] f = new double[fpieces.length];
    for (int i = 0; i < fpieces.length; i++) 
      f[i] = Double.parseDouble(fpieces[i]);

    return f;
  }

  public CLArgs(String[] args) throws BadCLArgsException, IOException {

    int i = 0;
    String arg;

    try {
      List<String> otherArgs = new ArrayList<String>();

      while (i < args.length) {
        arg = args[i++];

        if (arg.equals("-o") || arg.equals("-output")) 
          output = args[i++];
        
        else if (arg.equals("-T") || arg.equals("-dontCheckTerms"))
          checkTerms = false;
        
        else if (arg.equals("-S") || arg.equals("-scale"))
          scaleFactor = Double.parseDouble(args[i++]);

        else if (arg.equals("-S2") || arg.equals("-scale2"))
          scaleFactor2 = Double.parseDouble(args[i++]);

        else if (arg.equals("-t") || arg.equals("-test"))
          testCorpus = args[i++];

        else if (arg.equals("-F") || arg.equals("-factor")) 
          factor = args[i++];

        else if (arg.equals("-i") || arg.equals("-iterations")) 
          iter = Integer.parseInt(args[i++]);

        else if (arg.equals("-D") || arg.equals("-emdelta")) 
          emdelta = Float.parseFloat(args[i++]);

        else if (arg.equals("-c") || arg.equals("-constraints"))
          tagContraints = args[i++];

        else if (arg.equals("-C") || arg.equals("-constraintmethod")) 
          cmethod = args[i++];

        else if (arg.equals("-S") || arg.equals("-stopsymbol"))
          stopv = args[i++];

        else if (arg.equals("-G") || arg.equals("-grandparents"))
          grandparents = BIOEncoder.GPOpt.GP;
        
        else if (arg.equals("-GG") || arg.equals("-grandparentsN"))
          grandparents = BIOEncoder.GPOpt.NOSTOP;

        else if (arg.equals("-g") || arg.equals("-goldstandards"))
          goldStandards = args[i++];

        else if (arg.equals("-E") || arg.equals("-evaltype"))
          evalType = args[i++];

        else if (arg.equals("-v") || arg.equals("-verbose")) 
          verbose = true;

        else
          otherArgs.add(arg);
      }

      this.args = otherArgs.toArray(new String[0]);

      // don't run EM more than 200 iterations
      if (iter < 0) iter = 200;
    }
    catch (ArrayIndexOutOfBoundsException e) {
      throw new BadCLArgsException();
    }
  }

  /**
   * Print program usage to stream
   * @param stream
   */
  public static void printUsage(PrintStream stream) {
    String prog = Main.class.getName();
    stream.println(
        "Usage: java " + prog + " action [options] [args]\n" +
        "\n" +
        "Actions:\n" +
        "  simple-chunk\n" +
        "  hmm1-chunk\n" +
        "  hmm2-chunk\n" +
        "  rrg1-chunk\n" +  
        "  rrg2-chunk\n" +
        "\n" +
        "Options:\n" +
        "  -o|-output FILE            Set output file/template\n" +
        "  -S|-scaleFactor N          HMM smoothing scaling factor\n" +
        "  -S2|-scaleFactor2 N        RRG smoothing scaling factor\n" +
        "  -F|-factor N1,N2...        Mult factors for baseline chunking\n" +
        "  -g|-goldstandards F1,F2... Use specified gold-standard corpora for eval\n" +
        "  -G|-grandparents           Use pseudo 2nd order tagset\n" +
        "  -GG|-grandparentsN         Use pseudo 2nd order tagset without altering STOP tag\n" +
        "  -i|-iterations N           Iterations of EM\n" +
        "  -D|-emdelta D              Halt EM when data perplexity change is less than\n" +
        "  -c|-tagconstraints FILE    Use tag-pair constraint spec\n" +
        "  -C|-constraintmethod M     Use specified method for enforcing constraints\n" +
        "  -t|-test F                 Use this data set as test\n" +
        "  -T|-dontCheckTerms         Don't check that the eval and output terms are equal"
    );
  }

  /**
   * @return An array of evaluation objects for each gold standard corpus 
   * provided
   * @throws IOException If there is any problem opening one of the corpus files 
   */
  public ChunkingEval[] getEvals() throws IOException {
    if (evals == null)
      if (goldStandards != null) {
        String[] gs = goldStandards.split(",");
        evals = new ChunkingEval[gs.length];
        for (int i = 0; i < gs.length; i++) {
          evals[i] = ChunkingEval.fromCorpusFile(gs[i], alpha, checkTerms);
        }
      } else {
        evals = new ChunkingEval[0];
      }

    return evals;
  }
}