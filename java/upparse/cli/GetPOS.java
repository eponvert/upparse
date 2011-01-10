package upparse.cli;

import java.io.*;

import upparse.corpus.*;

public class GetPOS {

  public static void main(final String args[]) {
    final CorpusType corpusType = CorpusType.valueOf(args[0]);
    final String[] corpusFiles = middle(args);
    final String output = last(args);
    final Alpha alpha = new Alpha();
    final Iterable<LabeledBracketSet> treeiter;
    final CorpusConstraints cc;
    switch (corpusType) {
      case WSJ:
        treeiter = WSJCorpusTreeIter.fromFiles(corpusFiles, alpha);
        cc = WSJCorpusStandard.instance;
        break;
        
      case CTB:
        treeiter = CTBCorpusTreeIter.fromFiles(corpusFiles, alpha);
        cc = CTBCorpusStandard.instance;
        break;
        
      case NEGRA:
        treeiter = NegraCorpusTreeIter.fromFiles(corpusFiles, alpha);
        cc = NegraCorpusStandard.instance;
        break;
        
      default:
        throw new RuntimeException("Unexpected file type: " + corpusType);
    }
    
    BufferedWriter bw;
    try {
      bw = new BufferedWriter(new FileWriter(new File(output)));
      for (LabeledBracketSet lbs: treeiter) {
        String[] pos = lbs.getPos();
        String[] tok = lbs.getTokensS();
        for (int i = 0; i < pos.length; i++) {
          if (isPunc(tok[i])) 
            bw.write("; ");
          else if (cc.getToken(tok[i], pos[i]).length() > 0) {
            bw.write(pos[i]);
            bw.write(' ');
          }
        }
        bw.write('\n');
      }
      bw.close();
    } catch (IOException e) {
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }

  private static boolean isPunc(String string) {
    return KeepStop.isStoppingPunc(string);
  }

  private static String last(String[] args) {
    return args[args.length - 1];
  }

  private static String[] middle(final String[] all) {
    final String[] mid = new String[all.length - 2];
    for (int i = 0; i < all.length - 2; i++) {
      mid[i] = all[i + 1];
    }
    return mid;
  }
}
