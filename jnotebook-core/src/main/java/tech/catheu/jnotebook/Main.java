/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Paths;

import org.checkerframework.checker.nullness.qual.Nullable;

import static tech.catheu.jnotebook.Constants.VERSION;

@CommandLine.Command(
        name = "jnotebook",
        subcommands = {Main.InteractiveServerCommand.class,
                       Main.RenderCommand.class})
public class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);
  public static final String USER_HOME = System.getProperty("user.home");

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }

  @CommandLine.Command(name = "server", mixinStandardHelpOptions = true,
                       description = "Start the interactive notebook server.",
                       versionProvider = Main.VersionProvider.class)
  public static class InteractiveServerCommand implements Runnable {
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
    public static final String AUTO_CLASSPATH = "JNOTEBOOK_AUTO_CLASSPATH";
    // notebook runtime configs
    @CommandLine.Option(names = {"-cp", "-classpath", "--class-path"},
                        paramLabel = "<CLASSPATH>",
                        description = "A : separated list of directories, JAR archives,\n and ZIP archives to search for class files. \nIf not set, jnotebook looks for build system (eg maven) project files.")
    public String classPath = AUTO_CLASSPATH;

    @CommandLine.Option(names = {"--local-storage-path"}, paramLabel = "<PATH>",
                        description = "The fullpath to a folder to use as jnotebook local storage, where extensions and states are cached.")
    public String localStoragePath = Paths.get(USER_HOME, ".jnotebook").toString();

    @CommandLine.Option(names = {"--no-utils"},
                        description = "If passed, disable the injection of jnotebook-utils jar.")
    public boolean noUtils = false;
  }


  public static class InteractiveConfiguration extends SharedConfiguration {
    @CommandLine.Parameters(index = "0", description = "The notebook folder to watch.",
                            defaultValue = "notebooks")
    public String notebookPath = "notebooks";

    @CommandLine.Option(names = {"-p", "--port"}, paramLabel = "<PORT>",
                        description = "Port of the notebook server",
                        defaultValue = "5002")
    public Integer port;
  }


  @CommandLine.Command(name = "render", mixinStandardHelpOptions = true,
                       description = "Render a notebook in a publishable format.",
                       versionProvider = Main.VersionProvider.class)
  public static class RenderCommand implements Runnable {
    @CommandLine.Mixin
    private RenderConfiguration config;

    public void run() {
      final NotebookRenderer renderer = NotebookRenderer.from(config);
      renderer.render(config);
    }
  }

  public static class RenderConfiguration extends SharedConfiguration {

    @CommandLine.Parameters(index = "0",
                            description = "The path to the notebook to render.")
    public String inputPath;

    
    @Nullable
    @CommandLine.Parameters(index = "1", arity = "0..1", description = "The output path.")
    public String outputPath;

    @CommandLine.Option(names= {"--no-optimize"},
                        description = "If passed, skips the html optimization. The optimization uses a headless browser to pre-render some components, and attempts to remove unused libraries.")
    public boolean noOptimize = false;
  }

  protected static class VersionProvider implements CommandLine.IVersionProvider {
    public String[] getVersion() {
      return new String[]{"Jnotebook: " + VERSION,
                          "Picocli: " + picocli.CommandLine.VERSION,
                          "JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
                          "OS: ${os.name} ${os.version} ${os.arch}"};
    }
  }
}
