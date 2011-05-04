package upparse.corpus;

/**
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class KeepStop implements CorpusConstraints {

  private final CorpusConstraints standard;

  private KeepStop(CorpusConstraints _standard) {
    standard = _standard;
  }

  public static final KeepStop wsjKeepStop = new KeepStop(
      WSJCorpusStandard.instance);

  public static final KeepStop negraKeepStop = new KeepStop(
      NegraCorpusStandard.instance);

  public static final KeepStop ctbKeepStop = new KeepStop(
      CTBCorpusStandard.instance);

  static String[] STOPPING_PUNC = new String[] { ".", "?", "!", ";", ",", "--",
      "\u3002", "\u3001", "\uFF0C" }; // Chinese ideographic stop, ideographic
                                      // comma, fullwidth comma

  public static final String STOP = "__stop__";

  public static boolean isStoppingPunc(final String w) {
    for (String p : STOPPING_PUNC)
      if (p.equals(w))
        return true;
    return false;
  }

  @Override
  public String wrap(final String s) {
    StringBuffer sb = new StringBuffer();
    if (!s.startsWith(STOP)) {
      sb.append(STOP);
      sb.append(" ");
    }
    sb.append(s);
    if (!s.endsWith(STOP)) {
      if (!s.endsWith(" "))
        sb.append(" ");
      sb.append(STOP);
    }

    return sb.toString();
  }

  @Override
  public String getToken(String token, String pos) {
    if (isStoppingPunc(token))
      return STOP;
    else
      return standard.getToken(token, pos);
  }
}
