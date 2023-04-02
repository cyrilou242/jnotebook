package ai.catheu.notebook.evaluate;

import ai.catheu.notebook.parse.StaticParsing;
import ai.catheu.notebook.parse.StaticSnippet;
import ai.catheu.notebook.parse.StaticSnippet.Type;
import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleInterpreter implements Interpreter {

  private static final Logger LOG = LoggerFactory.getLogger(SimpleInterpreter.class);

  private final Map<Path, JShell> fileToShell = new HashMap();
  private final ShellProvider shellProvider;

  public SimpleInterpreter(final ShellProvider shellProvider) {
    this.shellProvider = shellProvider;
  }

  public Interpreted interpret(final StaticParsing staticParsing) {
    final JShell shell =
            fileToShell.computeIfAbsent(staticParsing.path(), this::newShell);
    final List<InterpretedSnippet> interpretedSnippets = new ArrayList<>();
    for (StaticSnippet s : staticParsing.snippets()) {
      if (s.type().equals(Type.JAVA)) {
        final List<SnippetEvent> events = shell.eval(s.completionInfo().source());
        interpretedSnippets.add(new InterpretedSnippet(s, events));
      } else {
        // magic interpretation not implemented
        interpretedSnippets.add(new InterpretedSnippet(s, null));
      }
    }

    return new Interpreted(staticParsing.path(),
                           staticParsing.lines(),
                           interpretedSnippets);
  }

  private JShell newShell(final Path path) {
    LOG.info("Starting new shell for file: {}", path.getFileName());
    return shellProvider.getShell();
  }

  @Override
  public void stop() {
    fileToShell.values().forEach(JShell::close);
  }
}
