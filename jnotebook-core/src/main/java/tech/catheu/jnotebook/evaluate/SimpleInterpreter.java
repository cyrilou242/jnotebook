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
package tech.catheu.jnotebook.evaluate;

import tech.catheu.jnotebook.ExecutionStatus;
import tech.catheu.jnotebook.jshell.EvalResult;
import tech.catheu.jnotebook.jshell.PowerJShell;
import tech.catheu.jnotebook.jshell.ShellProvider;
import tech.catheu.jnotebook.parse.StaticParsing;
import tech.catheu.jnotebook.parse.StaticSnippet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleInterpreter implements Interpreter {

  private static final Logger LOG = LoggerFactory.getLogger(SimpleInterpreter.class);

  private final Map<Path, PowerJShell> fileToShell = new HashMap();
  private final ShellProvider shellProvider;

  public SimpleInterpreter(final ShellProvider shellProvider) {
    this.shellProvider = shellProvider;
  }

  public Interpreted interpret(final StaticParsing staticParsing) {
    final PowerJShell shell =
            fileToShell.computeIfAbsent(staticParsing.path(), this::newShell);
    final List<InterpretedSnippet> interpretedSnippets = new ArrayList<>();
    for (StaticSnippet s : staticParsing.snippets()) {
      if (s.type().equals(StaticSnippet.Type.JAVA)) {
        final EvalResult res = shell.eval(s.completionInfo().source());
        interpretedSnippets.add(new InterpretedSnippet(s, res));
      } else {
        // magic interpretation not implemented
        interpretedSnippets.add(new InterpretedSnippet(s, null));
      }
    }

    return new Interpreted(staticParsing.path(),
                           staticParsing.lines(),
                           interpretedSnippets,
                           ExecutionStatus.ok());
  }

  private PowerJShell newShell(final Path path) {
    LOG.info("Starting new shell for file: {}", path.getFileName());
    return shellProvider.getShell();
  }

  @Override
  public void stop() {
    fileToShell.values().forEach(PowerJShell::close);
  }
}
