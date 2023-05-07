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
