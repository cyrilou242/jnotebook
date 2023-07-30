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

import jdk.jshell.Diag;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import tech.catheu.jnotebook.Main;
import tech.catheu.jnotebook.jshell.ShellProvider;
import tech.catheu.jnotebook.parse.StaticParser;
import tech.catheu.jnotebook.parse.StaticParsing;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;


public class GreedyInterpreterTest {

  private static final ShellProvider shellProvider = getTestShellProvider();
  private final StaticParser staticParser = new StaticParser(shellProvider);

  // todo basic asserts of input/output
  //  basic assert on error case
  // comments, all kind of ops (import, function, class, etc...)

  @Test
  public void testValueStateIsDeletedCorrectlyWhenForwardReferenceBecomesCorrectReference() {
    final GreedyInterpreter interpreter = new GreedyInterpreter(shellProvider);
    // should not conflict with other tests
    final Path filePath = Paths.get("testValueStateIsDeletedCorrectlyWhenForwardReferenceBecomesCorrectReference");

    final String edit1 = """
            int z = x + 5;
            int x = 4;
            int y = z + 3;
            """;
    final StaticParsing staticParsing1 =
            staticParser.snippetsOf(filePath, edit1.lines().toList());
    final Interpreted out1 = interpreter.interpret(staticParsing1);
    assertThat(out1.interpretedSnippets()).hasSize(3);
    assertThat(firstDiagnosticMessage(out1.interpretedSnippets().get(0))).contains("cannot find symbol");
    assertThat(firstDiagnostics(out1.interpretedSnippets().get(1))).isEmpty();
    assertThat(firstDiagnosticMessage(out1.interpretedSnippets().get(2))).contains("cannot find symbol");

    // fix order
    final String edit2 = """
            int x = 4;
            int z = x + 5;
            int y = z + 3;
            """;
    final StaticParsing staticParsing2 =
            staticParser.snippetsOf(filePath, edit2.lines().toList());
    final Interpreted out2 = interpreter.interpret(staticParsing2);
    assertThat(out2.interpretedSnippets()).hasSize(3);
    assertThat(firstDiagnostics(out2.interpretedSnippets().get(0))).isEmpty();
    assertThat(firstDiagnostics(out2.interpretedSnippets().get(1))).isEmpty();
    assertThat(firstDiagnostics(out2.interpretedSnippets().get(2))).isEmpty();

    // put back the bad order - ensure states are deleted correctly
    final String edit3 = """
            int z = x + 5;
            int x = 4;
            int y = z + 3;
            """;
    final StaticParsing staticParsing3 =
            staticParser.snippetsOf(filePath, edit3.lines().toList());
    final Interpreted out3 = interpreter.interpret(staticParsing3);
    assertThat(firstDiagnosticMessage(out3.interpretedSnippets().get(0))).contains("cannot find symbol");
    assertThat(firstDiagnostics(out3.interpretedSnippets().get(1))).isEmpty();
    assertThat(firstDiagnosticMessage(out3.interpretedSnippets().get(2))).contains("cannot find symbol");
  }

  // this behaviour may change in the future
  // eg we could delete the snippet in the JShell instance if we find the result of
  // the execution mentions that a variable is not defined.
  // for the moment we keep the behaviour of jshell
  // TODO CYRIL - return the jshell info message instead of the unresolved dependencies info
  //  eg return created method doSomething(), however, it cannot be invoked until variable text is declared
  //  instead of Unresolved dependencies: variable text
  @Test
  public void testMethodStateBehavesLikeJShellWhenVariableIsUndeclared() {
    final GreedyInterpreter interpreter = new GreedyInterpreter(shellProvider);
    // should not conflict with other tests
    final Path filePath = Paths.get("testMethodStateBehavesLikeJShellWhenVariableIsUndeclared");

    final String edit1 = """
            int doSomething() {
              return text1.length();
            }
            String text1 = "blabla";
            doSomething();
            """;
    final StaticParsing staticParsing1 =
            staticParser.snippetsOf(filePath, edit1.lines().toList());
    final Interpreted out1 = interpreter.interpret(staticParsing1);
    assertThat(out1.interpretedSnippets()).hasSize(3);
    assertThat(firstUnresolvedDepsMessage(out1.interpretedSnippets().get(0))).contains("variable text1");
    assertThat(firstDiagnostics(out1.interpretedSnippets().get(1))).isEmpty();
    // the function can run because text1 is defined at the time of the call
    assertThat(firstDiagnostics(out1.interpretedSnippets().get(1))).isEmpty();
  }

  @Test
  public void testDuplicatedNonReturningCalls() {
    final GreedyInterpreter interpreter = new GreedyInterpreter(shellProvider);
    // should not conflict with other tests
    final Path filePath = Paths.get("testDuplicatedNonReturningCalls");
    final String edit1 = """
            int lol = 7;
            System.out.println(lol);
            lol = 11;
            System.out.println(lol);
            lol = 5;
            System.out.println("Response: " + lol);
            lol;
            """;
    final StaticParsing staticParsing1 =
            staticParser.snippetsOf(filePath, edit1.lines().toList());
    final Interpreted out1 = interpreter.interpret(staticParsing1);
    assertThat(out1.interpretedSnippets()).hasSize(7);
    assertThat(out1.interpretedSnippets().get(0).evalResult().events().get(0).value()).isEqualTo("7");
    assertThat(out1.interpretedSnippets().get(1).evalResult().out().trim()).isEqualTo("7");
    assertThat(out1.interpretedSnippets().get(2).evalResult().events().get(0).value()).isEqualTo("11");
    assertThat(out1.interpretedSnippets().get(3).evalResult().out().trim()).isEqualTo("11");
    assertThat(out1.interpretedSnippets().get(4).evalResult().events().get(0).value()).isEqualTo("5");
    assertThat(out1.interpretedSnippets().get(5).evalResult().out().trim()).isEqualTo("Response: 5");
    assertThat(out1.interpretedSnippets().get(6).evalResult().events().get(0).value()).isEqualTo("5");

    // update the value of the first instantiation of lol
    final String edit2 = """
            int lol = 0;
            System.out.println(lol);
            lol = 11;
            System.out.println(lol);
            lol = 5;
            System.out.println("Response: " + lol);
            lol;
            """;
    final StaticParsing staticParsing2 =
            staticParser.snippetsOf(filePath, edit2.lines().toList());
    final Interpreted out2 = interpreter.interpret(staticParsing2);
    assertThat(out2.interpretedSnippets()).hasSize(7);
    assertThat(out2.interpretedSnippets().get(0).evalResult().events().get(0).value()).isEqualTo("0");
    assertThat(out2.interpretedSnippets().get(1).evalResult().out().trim()).isEqualTo("0");
    assertThat(out2.interpretedSnippets().get(2).evalResult().events().get(0).value()).isEqualTo("11");
    assertThat(out2.interpretedSnippets().get(3).evalResult().out().trim()).isEqualTo("11");
    assertThat(out2.interpretedSnippets().get(4).evalResult().events().get(0).value()).isEqualTo("5");
    assertThat(out2.interpretedSnippets().get(5).evalResult().out().trim()).isEqualTo("Response: 5");
    assertThat(out2.interpretedSnippets().get(6).evalResult().events().get(0).value()).isEqualTo("5");

    // update the value of lol in the middle
    final String edit3 = """
            int lol = 0;
            System.out.println(lol);
            lol = 100;
            System.out.println(lol);
            lol = 5;
            System.out.println("Response: " + lol);
            lol;
            """;
    final StaticParsing staticParsing3 =
            staticParser.snippetsOf(filePath, edit3.lines().toList());
    final Interpreted out3 = interpreter.interpret(staticParsing3);
    assertThat(out3.interpretedSnippets()).hasSize(7);
    assertThat(out3.interpretedSnippets().get(0).evalResult().events().get(0).value()).isEqualTo("0");
    assertThat(out3.interpretedSnippets().get(1).evalResult().out().trim()).isEqualTo("0");
    assertThat(out3.interpretedSnippets().get(2).evalResult().events().get(0).value()).isEqualTo("100");
    assertThat(out3.interpretedSnippets().get(3).evalResult().out().trim()).isEqualTo("100");
    assertThat(out3.interpretedSnippets().get(4).evalResult().events().get(0).value()).isEqualTo("5");
    assertThat(out3.interpretedSnippets().get(5).evalResult().out().trim()).isEqualTo("Response: 5");
    assertThat(out3.interpretedSnippets().get(6).evalResult().events().get(0).value()).isEqualTo("5");

    // update the value of lol at the end
    final String edit4 = """
            int lol = 0;
            System.out.println(lol);
            lol = 100;
            System.out.println(lol);
            lol = 667;
            System.out.println("Response: " + lol);
            lol;
            """;
    final StaticParsing staticParsing4 =
            staticParser.snippetsOf(filePath, edit4.lines().toList());
    final Interpreted out4 = interpreter.interpret(staticParsing4);
    assertThat(out4.interpretedSnippets()).hasSize(7);
    assertThat(out4.interpretedSnippets().get(0).evalResult().events().get(0).value()).isEqualTo("0");
    assertThat(out4.interpretedSnippets().get(1).evalResult().out().trim()).isEqualTo("0");
    assertThat(out4.interpretedSnippets().get(2).evalResult().events().get(0).value()).isEqualTo("100");
    assertThat(out4.interpretedSnippets().get(3).evalResult().out().trim()).isEqualTo("100");
    assertThat(out4.interpretedSnippets().get(4).evalResult().events().get(0).value()).isEqualTo("667");
    assertThat(out4.interpretedSnippets().get(5).evalResult().out().trim()).isEqualTo("Response: 667");
    assertThat(out4.interpretedSnippets().get(6).evalResult().events().get(0).value()).isEqualTo("667");
  }

  @Test
  public void testRecursiveFunction() {
    final GreedyInterpreter interpreter = new GreedyInterpreter(shellProvider);
    // should not conflict with other tests
    final Path filePath = Paths.get("testMethodStateBehavesLikeJShellWhenVariableIsUndeclared");

    final String edit1 = """
            static int recursive(int n) {
            if (n == 1) {
              return 1;
              } else {
                return n * recursive(n-1);
              }
            }
            recursive(4);
            """;
    final StaticParsing staticParsing1 =
            staticParser.snippetsOf(filePath, edit1.lines().toList());
    final Interpreted out1 = interpreter.interpret(staticParsing1);
    assertThat(out1.interpretedSnippets()).hasSize(2);
    assertThat(out1.interpretedSnippets().get(1).evalResult().events().get(0).value()).isEqualTo("24");

    // edit the recursive function
    final String edit2 = """
            static int recursive(int n) {
            if (n == 1) {
              return 1;
              } else {
                return n * recursive(n-1) + 1;
              }
            }
            recursive(4);
            """;
    final StaticParsing staticParsing2 =
            staticParser.snippetsOf(filePath, edit2.lines().toList());
    final Interpreted out2 = interpreter.interpret(staticParsing2);
    assertThat(out2.interpretedSnippets()).hasSize(2);
    assertThat(out2.interpretedSnippets().get(1).evalResult().events().get(0).value()).isEqualTo("41");
  }

  private static List<Diag> firstDiagnostics(InterpretedSnippet interpretedSnippet) {
    return interpretedSnippet.evalResult()
                             .diagnostics()
                             .get(0);
  }

  private static String firstDiagnosticMessage(
          final InterpretedSnippet interpretedSnippet) {
    return firstDiagnostics(interpretedSnippet)
            .get(0)
            .getMessage(Locale.ENGLISH);
  }

  private static List<String> firstUnresolvedDeps(InterpretedSnippet interpretedSnippet) {
    return interpretedSnippet.evalResult()
                             .unresolvedDeps()
                             .get(0);
  }

  private static String firstUnresolvedDepsMessage(InterpretedSnippet interpretedSnippet) {
    return firstUnresolvedDeps(interpretedSnippet).get(0);
  }

  @NotNull
  private static ShellProvider getTestShellProvider() {
    Main.SharedConfiguration configuration = new Main.SharedConfiguration();
    configuration.classPath = "\"\"";
    configuration.noUtils = true;
    return new ShellProvider(configuration);
  }
}
