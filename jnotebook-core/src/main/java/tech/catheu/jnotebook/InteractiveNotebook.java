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

import java.io.IOException;
import java.nio.file.Paths;

public class InteractiveNotebook {

  private static final Logger LOG = LoggerFactory.getLogger(InteractiveNotebook.class);

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
