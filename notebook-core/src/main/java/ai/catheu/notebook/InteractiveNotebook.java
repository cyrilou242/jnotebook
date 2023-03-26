package ai.catheu.notebook;

import ai.catheu.notebook.evaluate.Interpreter;
import ai.catheu.notebook.file.PathObservables;
import ai.catheu.notebook.parse.StaticParser;
import ai.catheu.notebook.render.Renderer;
import ai.catheu.notebook.server.ReloadServer;
import io.methvin.watcher.DirectoryChangeEvent;
import io.reactivex.rxjava3.core.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;

import static io.methvin.watcher.DirectoryChangeEvent.EventType.DELETE;

public class InteractiveNotebook {

  private static final Logger LOG = LoggerFactory.getLogger(InteractiveNotebook.class);

  private final Main.InteractiveConfiguration configuration;
  private static final String JSHELL_SUFFIX = ".jsh";
  private final StaticParser staticParser;
  private final Interpreter interpreter;
  private final Renderer renderer;
  private final ReloadServer server;

  public InteractiveNotebook(final Main.InteractiveConfiguration configuration) {
    this.configuration = configuration;
    this.staticParser = new StaticParser();
    this.interpreter = new Interpreter();
    this.renderer = new Renderer();
    this.server = new ReloadServer();
  }

  public void run() throws IOException {
    server.start();
    final Observable<DirectoryChangeEvent> notebookEvents =
            PathObservables.of(Paths.get(configuration.notebookPath))
                           .filter(e -> e.path().toString().endsWith(JSHELL_SUFFIX));

    final var deletes = notebookEvents.filter(e -> e.eventType().equals(DELETE));
    //.subscribe(s -> server.sendReload()); to subscribe on a side scheduler

    LOG.info("Notebook server started successfully on http://localhost:5002");

    notebookEvents.filter(e -> !e.eventType().equals(DELETE))
                  .doOnError(InteractiveNotebook::logError)
                  .map(staticParser::staticSnippets)
                  .doOnError(InteractiveNotebook::logError)
                  .map(interpreter::interpret)
                  .doOnError(InteractiveNotebook::logError)
                  .map(renderer::render)
                  .doOnError(InteractiveNotebook::logError)
                  .subscribe(server::sendUpdate, InteractiveNotebook::logError);
  }

  private static void logError(Throwable e) {
    LOG.error(e.getMessage());
  }


  public void stop() throws IOException {
    server.stop();
    // todo dispose of all the reactive stream
  }
}
