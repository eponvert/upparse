package upparse;

/**
 * @author ponvert@mail.utexas.edu (Elias Ponvert)
 */
public abstract class KeepStop implements CorpusConstraints {
  
  String[] STOPPING_PUNC = new String[] {
    ".", "?", "!", ";", ",", "--", 
    "\uE38081", "\uEFBC8C", "\uE38082"  }; // Chinese stop, comma, enumeration
  
  static final String STOP = "__stop__";
  
  public boolean isStoppingPunc(final String w) {
    for (String p: STOPPING_PUNC)
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
}
