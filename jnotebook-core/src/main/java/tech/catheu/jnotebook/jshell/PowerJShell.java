package tech.catheu.jnotebook.jshell;

import jdk.jshell.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * An opinionated wrapping of JShell that exposes more info about snippets
 */
public class PowerJShell {

  public static final String JSHELL_DEFAULT_JSH = """
          import java.io.*;
          import java.math.*;
          import java.net.*;
          import java.nio.file.*;
          import java.util.*;
          import java.util.concurrent.*;
          import java.util.function.*;
          import java.util.prefs.*;
          import java.util.regex.*;
          import java.util.stream.*;""";

  public static final String JSHELL_PRINTING_JSH = """
          void print(boolean b) { System.out.print(b); }
          void print(char c) { System.out.print(c); }
          void print(int i) { System.out.print(i); }
          void print(long l) { System.out.print(l); }
          void print(float f) { System.out.print(f); }
          void print(double d) { System.out.print(d); }
          void print(char s[]) { System.out.print(s); }
          void print(String s) { System.out.print(s); }
          void print(Object obj) { System.out.print(obj); }
          void println() { System.out.println(); }
          void println(boolean b) { System.out.println(b); }
          void println(char c) { System.out.println(c); }
          void println(int i) { System.out.println(i); }
          void println(long l) { System.out.println(l); }
          void println(float f) { System.out.println(f); }
          void println(double d) { System.out.println(d); }
          void println(char s[]) { System.out.println(s); }
          void println(String s) { System.out.println(s); }
          void println(Object obj) { System.out.println(obj); }
          void printf(java.util.Locale l, String format, Object... args) { System.out.printf(l, format, args); }
          void printf(String format, Object... args) { System.out.printf(format, args); }
          """;
  private static final List<String> initScripts = List.of(JSHELL_DEFAULT_JSH, JSHELL_PRINTING_JSH);

  private final JShell delegate;
  private final ByteArrayOutputStream out;
  private final ByteArrayOutputStream err;
  private final PrintStream outPrintStream;
  private final PrintStream errPrintStream;

  public PowerJShell(final Configuration configuration) {
    out = new ByteArrayOutputStream();
    outPrintStream = new PrintStream(out);
    err = new ByteArrayOutputStream();
    errPrintStream = new PrintStream(err);
    this.delegate = JShell.builder()
            .executionEngine("local")
                          .out(outPrintStream).err(errPrintStream).build();
    this.delegate.addToClasspath(configuration.classpath);
    for (final String script : initScripts) {
      for (final String statement: script.split("\n")) {
       this.delegate.eval(statement);
      }
    }
  }

  public EvalResult eval(String input) throws IllegalStateException {
    final List<SnippetEvent> eval = delegate.eval(input);
    List<List<Diag>> diagnostics = new ArrayList<>();
    List<List<String>> unresolvedDeps = new ArrayList<>();
    for (final SnippetEvent se : eval) {
      final Snippet snippet = se.snippet();
      diagnostics.add(delegate.diagnostics(snippet).toList());
      if (snippet instanceof DeclarationSnippet ds) {
        unresolvedDeps.add(delegate.unresolvedDependencies(ds).toList());
      }

    }
    return new EvalResult(eval, popOut(), popErr(), diagnostics, unresolvedDeps);
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
