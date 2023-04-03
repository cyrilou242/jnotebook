package ai.catheu.notebook.jshell;

import jdk.jshell.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * An opinionated wrapping of JShell that exposes more info about snippets
 */
public class PowerJShell {

  private final JShell delegate;
  private final ByteArrayOutputStream out;
  private final ByteArrayOutputStream err;
  private final PrintStream outPrintStream;
  private final PrintStream errPrintStream;

  // will implement options as arguments later
  public PowerJShell(final Configuration configuration) {
    out = new ByteArrayOutputStream();
    outPrintStream = new PrintStream(out);
    err = new ByteArrayOutputStream();
    errPrintStream = new PrintStream(err);
    this.delegate = JShell.builder().out(outPrintStream).err(errPrintStream).build();
    this.delegate.addToClasspath(configuration.classpath);
  }

  public EvalResult eval(String input) throws IllegalStateException {
    final List<SnippetEvent> eval = delegate.eval(input);
    List<List<Diag>> diagnostics = new ArrayList<>();
    List<List<String>> unresolvedDeps = new ArrayList<>();
    for (final SnippetEvent se: eval) {
      final Snippet snippet = se.snippet();
      diagnostics.add(delegate.diagnostics(snippet).toList());
      if (snippet instanceof DeclarationSnippet ds) {
        unresolvedDeps.add(delegate.unresolvedDependencies(ds).toList());
      }

    }
    return new EvalResult(eval,
                          popOut(),
                          popErr(),
                          diagnostics,
                          unresolvedDeps);
  }

  public void close() {
    delegate.close();
    outPrintStream.close();
    errPrintStream.close();
  }

  private String popErr() {
    final String res = err.toString();
    err.reset();
    return res;
  }

  private String popOut() {
    final String res = out.toString();
    out.reset();
    return res;
  }


  public SourceCodeAnalysis sourceCodeAnalysis() {
    return delegate.sourceCodeAnalysis();
  }

  public void drop(Snippet snippet) {
    delegate.drop(snippet);
  }

  public record Configuration(String classpath) {
  }
}
