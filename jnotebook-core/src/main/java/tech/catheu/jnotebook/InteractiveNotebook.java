/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.hashing.FileHash;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jnotebook.evaluate.GreedyInterpreter;
import tech.catheu.jnotebook.evaluate.Interpreter;
import tech.catheu.jnotebook.file.PathObservables;
import tech.catheu.jnotebook.jshell.ShellProvider;
import tech.catheu.jnotebook.parse.StaticParser;
import tech.catheu.jnotebook.render.Renderer;
import tech.catheu.jnotebook.server.InteractiveServer;
import tech.catheu.jnotebook.server.NotebookServerStatus;
import tech.catheu.jnotebook.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static tech.catheu.jnotebook.utils.FileUtils.writeResourceToFile;

public class InteractiveNotebook {

  private static final Logger LOG = LoggerFactory.getLogger(InteractiveNotebook.class);

  private static final String RESOURCES_HELLO_WORLD_NOTEBOOK =
          "/jnb_interactive/hello_world.jsh";
  private static final String FILESYSTEM_HELLO_WORLD_NAME = "hello_world.jsh";

  private final Main.InteractiveConfiguration configuration;
  private static final String JSHELL_SUFFIX = ".jsh";
  private final StaticParser staticParser;
  private final Interpreter interpreter;
  private final Renderer renderer;
  private InteractiveServer server;

  public InteractiveNotebook(final Main.InteractiveConfiguration configuration) {
    this.configuration = configuration;
    final ShellProvider shellProvider = new ShellProvider(configuration);
    this.staticParser = new StaticParser(shellProvider);
    this.interpreter = new GreedyInterpreter(shellProvider);
    this.renderer = new Renderer(configuration);
  }

  public void run() throws IOException {
    prepare();
    final Observable<DirectoryChangeEvent> fileChangeEvents =
            PathObservables.of(Paths.get(configuration.notebookPath))
                           .filter(e -> e.path().toString().endsWith(JSHELL_SUFFIX));
    final PublishSubject<DirectoryChangeEvent> manualTriggers = PublishSubject.create();
    this.server = new InteractiveServer(configuration,
                                        path -> manualTriggers.onNext(
                                                directoryChangeEvent(path)));
    server.start();
    LOG.info("Notebook server started. Go to http://localhost:" + configuration.port);
    manualTriggers
            .mergeWith(fileChangeEvents)
            .doOnEach(e -> server.sendStatus(NotebookServerStatus.COMPUTE))
            .map(staticParser::staticSnippets)
            .doOnError(InteractiveNotebook::logError)
            .map(interpreter::interpret)
            .doOnError(InteractiveNotebook::logError)
            .map(renderer::render)
            .doOnError(InteractiveNotebook::logError)
            .subscribe(server::sendUpdate, InteractiveNotebook::logError);
  }

  @NonNull
  private static DirectoryChangeEvent directoryChangeEvent(final Path path) {
    return new DirectoryChangeEvent(
            DirectoryChangeEvent.EventType.MODIFY,
            false,
            path,
            FileHash.fromLong(0),
            0,
            path.getRoot());
  }

  private void prepare() {
    // ensure notebook folder exists, if not, create it.
    final Path notebooksFolder = Paths.get(configuration.notebookPath);
    if (!Files.exists(notebooksFolder)) {
      LOG.info("Notebook folder {} does not exist. Creating it.",
               notebooksFolder.toAbsolutePath());
      FileUtils.createDirectoriesUnchecked(notebooksFolder);
      LOG.info("Adding an example notebook to the {} folder.", notebooksFolder);
      writeResourceToFile(RESOURCES_HELLO_WORLD_NOTEBOOK,
                          notebooksFolder.resolve(FILESYSTEM_HELLO_WORLD_NAME));
    }
  }

  private static void logError(Throwable e) {
    LOG.error(e.getMessage(), e);
  }


  public void stop() throws IOException {
    if (server != null) {
      server.stop();
    }
    staticParser.stop();
    interpreter.stop();
    renderer.stop();
  }
}
