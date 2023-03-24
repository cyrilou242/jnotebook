package ai.catheu;

import java.io.IOException;

public class Main {

  public static void main(String[] args) throws IOException {
    // bunch of parameter parsing and building
    final Notebook notebook = new Notebook(new NotebookConfiguration());
    try {
      notebook.run();
    } catch (Exception e) {
      try {
        notebook.stop();
      } finally {
        throw e;
      }
    }
  }
}
