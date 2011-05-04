package upparse.model;

import java.io.*;

/**
 * Simple class for creating CSV files
 * @author eponvert@utexas.edu (Elias Ponvert)
 */
public class CSVFileWriter {

  private BufferedWriter out;

  public CSVFileWriter(String fname) throws IOException {
    out = new BufferedWriter(new FileWriter(fname));
  }
  
  public void write(int i, double... doubles) throws IOException {
    out.write(Integer.toString(i));
    for (double d: doubles) {
      out.write(',');
      out.write(Double.toString(d));
    }
    out.write('\n');
  }
  
  public void close() throws IOException {
    out.close();
  }
}
