package upparse;

import java.io.*;
import java.util.*;

/**
 * Simple utility for dealing with command line arguments
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class CLArgs {

  public String output;
  public String factor = "2";
  public int iter;
  public float emdelta;
  public String[] args;
  public String tagContraints;
  public String cmethod;
  public String stopv = "__stop__";
  public boolean grandparents = false;
  
  public int[] getFactor() {
    String[] fpieces = factor.split(",");
    
    int[] f = new int[fpieces.length];
    for (int i = 0; i < fpieces.length; i++) 
      f[i] = Integer.parseInt(fpieces[i]);
    
    return f;
  }

  public CLArgs(String[] args) throws BadCLArgsException {

    int i = 0;
    String arg;

    try {
      List<String> otherArgs = new ArrayList<String>();
      
      while (i < args.length) {
        arg = args[i++];

        if (arg.equals("-o") || arg.equals("--output")) 
          output = args[i++];

        else if (arg.equals("-F") || arg.equals("--factor")) 
          factor = args[i++];

        else if (arg.equals("-i") || arg.equals("--iter")) 
          iter = Integer.parseInt(args[i]);

        else if (arg.equals("-D") || arg.equals("--emdelta")) 
          emdelta = Float.parseFloat(args[i]);

        else if (arg.equals("-c") || arg.equals("--tag_contraints"))
          tagContraints = args[i++];

        else if (arg.equals("-C") || arg.equals("--cmethod")) 
          cmethod = args[i++];
        
        else if (arg.equals("-S") || arg.equals("--stopv"))
          stopv = args[i++];
        
        else if (arg.contains("-G") || arg.equals("--grandparents"))
          grandparents = true;

        else
          otherArgs.add(arg);
      }
      
      this.args = otherArgs.toArray(new String[0]);
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
        "  simple-clump\n" +
        "  hmm1-clump\n" +
        "  hmm2-clump\n" +
        "  rrg1-clump\n" +  
        "  rrg2-clump\n" +
        "\n" +
        "Options:\n" +
        "  -o|--output FILE          Set output file/template\n" +
        "  -F|--factor F             Mult factors for baseline chunking\n" +
        "  -G|--grandparents         Use pseudo 2nd order tagset\n" +
        "  -i|--iter N               Iterations of EM\n" +
        "  -D|--emdelta D            Halt EM when data perplexity change is less than\n" +
        "  -c|--tag_constraints FILE Use tag-pair constraint spec\n"
    );
  }
}