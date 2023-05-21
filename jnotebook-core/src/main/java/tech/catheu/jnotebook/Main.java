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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;

public class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    final CommandLine commandLine = new CommandLine(new InteractiveCommand());
    commandLine.addSubcommand(new RenderCommand());
    int exitCode = commandLine.execute(args);
    System.exit(exitCode);
  }

  @CommandLine.Command(name = "start", mixinStandardHelpOptions = true,
                       description = "Start the interactive notebook server.")
  public static class InteractiveCommand implements Runnable {
    @CommandLine.Mixin
    private InteractiveConfiguration config;

    @Override
    public void run() {
      final InteractiveNotebook notebook = new InteractiveNotebook(config);
      try {
        notebook.run();
      } catch (Exception e) {
        try {
          notebook.stop();
        } catch (IOException ex) {
          LOG.error(ex.getMessage());
        } finally {
          LOG.error(e.getMessage());
        }
      }
    }
  }

  public static class SharedConfiguration {
    // notebook runtime configs
    @CommandLine.Option(names = {"-cp", "-classpath", "--class-path"},
                        paramLabel = "class search path of directories and zip/jar files",
                        description = "A : separated list of directories, JAR archives,\n and ZIP archives to search for class files.",
                        defaultValue = "")
    public String classPath = "";
  }


  public static class InteractiveConfiguration extends SharedConfiguration {
    @CommandLine.Parameters(index = "0", description = "The notebook folder to watch.",
                            defaultValue = "notebooks")
    public String notebookPath = "notebooks";

    @CommandLine.Option(names = {"-p", "--port"},
                        paramLabel = "server port",
                        description = "Port of the notebook server",
                        defaultValue = "5002")
    public Integer port;
  }


  @CommandLine.Command(name = "render", mixinStandardHelpOptions = true,
                       description = "Render a notebook in a publishable format.")
  public static class RenderCommand implements Runnable {
    @CommandLine.Mixin
    private RenderConfiguration config;

    public void run() {
      final NotebookRenderer renderer = NotebookRenderer.from(config);
      renderer.render(config);
    }
  }

  public static class RenderConfiguration extends SharedConfiguration {

    @CommandLine.Parameters(index = "0", description = "The path to the notebook to render.")
    public String inputPath;

    @CommandLine.Parameters(index = "1", description = "The output path.")
    public String outputPath;
  }
}
