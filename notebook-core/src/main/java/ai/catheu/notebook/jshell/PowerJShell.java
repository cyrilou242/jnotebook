package ai.catheu.notebook.jshell;

import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * bundles all resources necessary to manipulate a jshell
 */
// todo
public class PowerJShell {

  private final JShell delegate;
  private final ByteArrayOutputStream out;
  private final ByteArrayOutputStream err;
  private final PrintStream outPrintStream;
  private final PrintStream errPrintStream;

  // will implement options as arguments later
  public PowerJShell() {
    out = new ByteArrayOutputStream();
    outPrintStream = new PrintStream(out);
    err = new ByteArrayOutputStream();
    errPrintStream = new PrintStream(err);
    this.delegate = JShell.builder().out(outPrintStream).err(errPrintStream).build();
  }

  public EvalResult eval(String input) throws IllegalStateException {
    final List<SnippetEvent> eval = delegate.eval(input);
    return new EvalResult(eval, popOut(), popErr());
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
}
