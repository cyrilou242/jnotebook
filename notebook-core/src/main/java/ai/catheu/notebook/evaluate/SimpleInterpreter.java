package ai.catheu.notebook.evaluate;

import ai.catheu.notebook.jshell.EvalResult;
import ai.catheu.notebook.jshell.PowerJShell;
import ai.catheu.notebook.jshell.ShellProvider;
import ai.catheu.notebook.parse.StaticParsing;
import ai.catheu.notebook.parse.StaticSnippet;
import ai.catheu.notebook.parse.StaticSnippet.Type;
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
      if (s.type().equals(Type.JAVA)) {
        final EvalResult res = shell.eval(s.completionInfo().source());
        interpretedSnippets.add(new InterpretedSnippet(s, res));
      } else {
        // magic interpretation not implemented
        interpretedSnippets.add(new InterpretedSnippet(s, null));
      }
    }

    return new Interpreted(staticParsing.path(),
                           staticParsing.lines(),
                           interpretedSnippets);
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
