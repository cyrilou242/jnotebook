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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import tech.catheu.jnotebook.Main;
import tech.catheu.jnotebook.jshell.ShellProvider;
import tech.catheu.jnotebook.parse.StaticParser;
import tech.catheu.jnotebook.parse.StaticParsing;

import java.nio.file.Path;
import java.nio.file.Paths;


public class GreedyInterpreterTest {

  @Test
  public void testTruc() {
    final ShellProvider shellProvider = getTestShellProvider();
    final GreedyInterpreter interpreter = new GreedyInterpreter(shellProvider);
    final StaticParser staticParser = new StaticParser(shellProvider);
    final Path filePath = Paths.get("");

    final String edit1 = """
            int doSomething() {
              return text.length();
            }
            String text = "blabla";
            """;
    final StaticParsing staticParsing1 = staticParser.snippetsOf(filePath, edit1.lines().toList());
    final Interpreted res1 = interpreter.interpret(staticParsing1);
    // assert that there is one error
    final String edit2 = """
            String text = "blabla";
            int doSomething() {
              return text.length();
            }
            """;
    final StaticParsing staticParsing2 = staticParser.snippetsOf(filePath, edit2.lines().toList());
    final Interpreted res2 = interpreter.interpret(staticParsing2);

    // same as edit 1 - it should fail again
    // harder version will have blablou - meaning the forward reference refresh has to be performed based on the simple name
    final String edit3 = """
            int doSomething() {
              return text.length();
            }
            String text = "blabla";
            """;
    final StaticParsing staticParsing3 = staticParser.snippetsOf(filePath, edit3.lines().toList());
    final Interpreted res3 = interpreter.interpret(staticParsing3);

  }

  // todo add recursive function

  @NotNull
  private static ShellProvider getTestShellProvider() {
    Main.SharedConfiguration configuration = new Main.SharedConfiguration();
    configuration.classPath = "\"\"";
    configuration.noUtils = true;
    return new ShellProvider(configuration);
  }
}
