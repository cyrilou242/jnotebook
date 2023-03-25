package ai.catheu.notebook;

import ai.catheu.notebook.evaluate.Interpreter;
import ai.catheu.notebook.file.PathObservables;
import ai.catheu.notebook.parse.StaticParser;
import ai.catheu.notebook.render.Renderer;
import ai.catheu.notebook.server.ReloadServer;
import io.reactivex.rxjava3.core.Observable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

public class InteractiveNotebook {

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

    final Observable<WatchEvent<Path>> notebookEvents =
            PathObservables.watchRecursive(Paths.get(configuration.notebookPath))
                           .filter(e -> e.context().toString().endsWith(JSHELL_SUFFIX))
            // add some mechanism to control multiple events
            ;

    final var deletes = notebookEvents.filter(e -> e.kind().equals(ENTRY_DELETE));
    //.subscribe(s -> server.sendReload()); to subscribe on a side scheduler

    System.out.println("Notebook server started successfully on http://localhost:5002");

    notebookEvents.filter(e -> !e.kind().equals(ENTRY_DELETE))
                                      .doOnError(InteractiveNotebook::logError)
                                      .map(staticParser::staticSnippets)
                                      .doOnError(InteractiveNotebook::logError)
                                      .map(interpreter::interpret)
                                      .doOnError(InteractiveNotebook::logError)
                                      .map(renderer::render)
                                      .doOnError(InteractiveNotebook::logError)
                                      .subscribe(server::sendUpdate,
                                                 InteractiveNotebook::logError);
  }

  private static void logError(Throwable e) {
    System.out.println(
            "An eror happened: " + e);
  }


  public void stop() throws IOException {
    server.stop();
    // todo dispose of all the reactive stream
  }
}
