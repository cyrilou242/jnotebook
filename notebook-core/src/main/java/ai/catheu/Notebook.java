package ai.catheu;

import java.io.IOException;

public class Notebook {

  private final NotebookConfiguration configuration;
  private final FileWatcher watcher;

  public Notebook(final NotebookConfiguration configuration) {
    this.configuration = configuration;
    this.watcher = new FileWatcher(configuration.notebookRootPath);

  }

  public void run() throws IOException {
    watcher.run();
    // launch file watcher
    // launch html renderer
    // launch server
    // who is calling who. reactive streams? etc... start simple
  }

  public void stop() throws IOException {
    watcher.stop();
  }
}
