package upparse.cli;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import upparse.corpus.*;

public class GetPOS {

  public static String[][] getPos(
      Alpha alpha,
      String[] corpusFiles,
      CorpusType corpusType,
      String puncSymbol, 
      int filterLength) {
    List<String[]> posSent = new ArrayList<String[]>();
    final CorpusConstraints cc;
    final Iterable<LabeledBracketSet> treeiter;
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

    for (LabeledBracketSet lbs: treeiter) {
      String[] pos = lbs.getPos();
      String[] tok = lbs.getTokensS();

      int count = 0;

      List<String> keepPos = new ArrayList<String>();
      for (int i = 0; i < pos.length; i++) {
        if (isPunc(tok[i]) && puncSymbol != null)
          keepPos.add(puncSymbol);
        else if (cc.getToken(tok[i], pos[i]).length() > 0) { 
          keepPos.add(pos[i]);
          count++;
        }
      }

      if (filterLength < 0 || count <= filterLength) {
        String[] posA = keepPos.toArray(new String[0]); 
        posSent.add(posA);
      }
    }
    return posSent.toArray(new String[0][]);
  }

  public static void main(final String args[]) {
    final CorpusType corpusType = CorpusType.valueOf(args[0]);
    final String[] corpusFiles = middle(args);
    final String output = last(args);
    final Alpha alpha = new Alpha();
    BufferedWriter bw;
    try {
      bw = new BufferedWriter(new FileWriter(new File(output)));
      for (String[] posSent: getPos(alpha, corpusFiles, corpusType, ";", -1)) {
        for (String pos: posSent) {
          bw.write(pos);
          bw.write(' ');
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
