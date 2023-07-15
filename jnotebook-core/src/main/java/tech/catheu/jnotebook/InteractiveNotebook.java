/*
 * Copyright Cyril de Catheu, 2023
 *
 * Licensed under the JNOTEBOOK LICENSE 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at https://raw.githubusercontent.com/cyrilou242/jnotebook/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT * WARRANTIES OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and limitations under
 * the License.
 */
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

  private static final String RESOURCES_HELLO_WORLD_NOTEBOOK = "/jnb_interactive/hello_world.jsh";
  private static final String FILESYSTEM_HELLO_WORLD_NAME = "hello_world.jsh";

  private final Main.InteractiveConfiguration configuration;
  private static final String JSHELL_SUFFIX = ".jsh";
  private final StaticParser staticParser;
  private final Interpreter interpreter;
  private final Renderer renderer;
  private final InteractiveServer server;

  public InteractiveNotebook(final Main.InteractiveConfiguration configuration) {
    this.configuration = configuration;
    final ShellProvider shellProvider = new ShellProvider(configuration);
    this.staticParser = new StaticParser(shellProvider);
    this.interpreter = new GreedyInterpreter(shellProvider);
    this.renderer = new Renderer(configuration);
    this.server = new InteractiveServer(configuration);
  }

  public void run() throws IOException {
    prepare();
    server.start();
    final Observable<DirectoryChangeEvent> notebookEvents =
            PathObservables.of(Paths.get(configuration.notebookPath))
                           .filter(e -> e.path().toString().endsWith(JSHELL_SUFFIX));
    //.subscribe(s -> server.sendReload()); to subscribe on a side scheduler

    LOG.info("Notebook server started on http://localhost:" + configuration.port);

    notebookEvents
            .doOnEach(e -> server.sendStatus(NotebookServerStatus.COMPUTE))
            .map(staticParser::staticSnippets)
            .doOnError(InteractiveNotebook::logError)
            .map(interpreter::interpret)
            .doOnError(InteractiveNotebook::logError)
            .map(renderer::render)
            .doOnError(InteractiveNotebook::logError)
            .subscribe(server::sendUpdate, InteractiveNotebook::logError);
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
    server.stop();
    staticParser.stop();
    interpreter.stop();
    renderer.stop();
  }
}
