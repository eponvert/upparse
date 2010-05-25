package upparse;

import java.io.*;

/**
 * Main class manages command-line interface to UPP
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public class Main {
  
  private static void usageError() {
    CLArgs.printUsage(System.err);
    System.exit(1);
  }
  
  public static void main(String[] argv) {
    try {
      CLArgs clargs = new CLArgs(argv);
      
      String[] args = clargs.args;

      if (clargs.args.length == 0) 
        usageError();

      String action = args[0];
      
      if (action.equals("simple-clump")) { 
        if (args.length < 2) usageError();
        
        PrintWriter output = new PrintWriter(new File(clargs.output));
        
        BasicCorpus corpus = new BasicCorpus(args[1]);
        int[] factor = clargs.getFactor();
        String stopv = clargs.stopv;
        SimpleClumper clumper = new SimpleClumper(corpus, factor, stopv);
        for (String s: clumper.clumpedCorpusStr()) 
          output.println(s);
        
        output.close();
        
      }
      
      else if (action.equals("hmm1-clump"))
        System.exit(0);
      
      else if (action.equals("hmm2-clump"))
        System.exit(0);
      
      else if (action.equals("rrg1-clump"))
        System.exit(0);
      
      else if (action.equals("rrg2-clump"))
        System.exit(0);
      
      else
        usageError();
      
    } catch (BadCLArgsException e) {
      usageError();
      
    } catch (IOException e) {
      System.err.println("IO problem");
      usageError();
    }
    
  }
}
