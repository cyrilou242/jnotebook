package tech.catheu.jnotebook;

import io.methvin.watcher.DirectoryChangeEvent;
import io.reactivex.rxjava3.core.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jnotebook.evaluate.GreedyInterpreter;
import tech.catheu.jnotebook.evaluate.Interpreter;
import tech.catheu.jnotebook.file.PathObservables;
import tech.catheu.jnotebook.jshell.ShellProvider;
import tech.catheu.jnotebook.parse.StaticParser;
import tech.catheu.jnotebook.render.Renderer;
import tech.catheu.jnotebook.server.ReloadServer;

import java.io.IOException;
import java.nio.file.Paths;

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
    final ShellProvider shellProvider = new ShellProvider(configuration);
    this.staticParser = new StaticParser(shellProvider);
    this.interpreter = new GreedyInterpreter(shellProvider);
    this.renderer = new Renderer();
    this.server = new ReloadServer(configuration);
  }

  public void run() throws IOException {
    server.start();
    final Observable<DirectoryChangeEvent> notebookEvents =
            PathObservables.of(Paths.get(configuration.notebookPath))
                           .filter(e -> e.path().toString().endsWith(JSHELL_SUFFIX));
    //.subscribe(s -> server.sendReload()); to subscribe on a side scheduler

    LOG.info("Notebook server started on http://localhost:" + configuration.port);

    notebookEvents.map(staticParser::staticSnippets)
                  .doOnError(InteractiveNotebook::logError)
                  .map(interpreter::interpret)
                  .doOnError(InteractiveNotebook::logError)
                  .map(renderer::render)
                  .doOnError(InteractiveNotebook::logError)
                  .subscribe(server::sendUpdate, InteractiveNotebook::logError);
  }

  private static void logError(Throwable e) {
    LOG.error(e.getMessage(), e);
  }


  public void stop() throws IOException {
    server.stop();
    staticParser.stop();
    interpreter.stop();
    renderer.stop();
  }
}
