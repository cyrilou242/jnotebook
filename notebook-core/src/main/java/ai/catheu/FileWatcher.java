package ai.catheu;

import com.sun.nio.file.SensitivityWatchEventModifier;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileWatcher {

  private final static String JSHELL_SUFFIX = ".jsh";
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  private final Path notebookFolderPath;
  private final WatchService watcher;

  public FileWatcher(final String filepath) {
    notebookFolderPath = Paths.get(filepath);
    try {
      this.watcher = notebookFolderPath.getFileSystem().newWatchService();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void run() throws IOException {
    notebookFolderPath.register(watcher, new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_MODIFY}, SensitivityWatchEventModifier.HIGH);
    final Runnable watchTask = buildWatchTask(watcher);
    executorService.submit(watchTask);
  }

  private Runnable buildWatchTask(final WatchService watcher) {
    return () -> {
      while (true) {
        WatchKey key;
        try {
          key = watcher.take();
        } catch (InterruptedException ex) {
          return;
        }
        for (WatchEvent<?> event : key.pollEvents()) {
          String fileName = event.context().toString();
          if (fileName.endsWith(JSHELL_SUFFIX)) {
            System.out.println("HAHA THE FILE HAS CHANGED");
            // TODO CYRIL do some interesting stuff here
            //List<String> newLines = readLines(inputFile).values()
            //    .stream()
            //    .collect(Collectors.toList());
            //List<Integer> changedLines = computeChangedLines(newLines);
            //lines = readLines(inputFile);
            //eval(changedLines);
          }
        }
        key.reset();
      }
    };
  }

  public void stop() throws IOException {
    watcher.close();
  }
}
