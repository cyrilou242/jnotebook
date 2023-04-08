package tech.catheu.jnotebook.file;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryWatcher;
import io.reactivex.rxjava3.core.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class PathObservables {

  private static final Logger LOG = LoggerFactory.getLogger(PathObservables.class);

  public static Observable<DirectoryChangeEvent> of(final Path path) {
    return Observable.create(subscriber -> DirectoryWatcher.builder()
                                                           .path(path)
                                                           .listener(e -> {
                                                             LOG.info(
                                                                     "Change detected in file: {}",
                                                                     e.path());
                                                             subscriber.onNext(e);
                                                           })
                                                           .build()
                                                           .watch());
  }
}
