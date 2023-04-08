package tech.catheu.jnotebook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;

public class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    CommandLine commandLine = new CommandLine(new InteractiveCommand());
    // add command here
    //commandLine.addSubcommand("command2", new Command2());
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


  public static class InteractiveConfiguration {

    @CommandLine.Parameters(index = "0", description = "The notebook folder to watch.",
                            defaultValue = "notebooks")
    public String notebookPath = "notebooks";

    @CommandLine.Option(names = {"-cp", "-classpath", "--class-path"},
                        paramLabel = "class search path of directories and zip/jar files",
                        description = "A : separated list of directories, JAR archives,\n and ZIP archives to search for class files.",
                        defaultValue = "")
    public String classPath = "";
  }
}
