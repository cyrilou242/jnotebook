/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook.localstorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jnotebook.Main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;
import static tech.catheu.jnotebook.Constants.VERSION;
import static tech.catheu.jnotebook.utils.FileUtils.createDirectoriesIfNotExists;
import static tech.catheu.jnotebook.utils.FileUtils.writeResourceToFile;

public class LocalStorage {

  private static final Logger LOG = LoggerFactory.getLogger(LocalStorage.class);
  private static final String RESOURCES_JNOTEBOOK_UTILS_PATH =
          "/utils/jnotebook-utils.jar";
  private static final String LOCAL_STORAGE_UTILS_FOLDER = "utils";

  private static LocalStorage instance;
  private final Main.SharedConfiguration config;

  private LocalStorage(final Main.SharedConfiguration config) {
    this.config = config;
  }

  public static LocalStorage instanceFor(final Main.SharedConfiguration configuration) {
    if (instance == null) {
      instance = new LocalStorage(configuration);
    }
    checkArgument(configuration.localStoragePath.equals(instance.config.localStoragePath));
    return instance;
  }

  /**
   * Returns the path of the jnotebook-utils jar.
   * If it does not exist in the local storage, attempts to create it.
   */
  public String getUtilsPath() {
    final String localStorageUtilsJarName = "jnotebook-utils-" + VERSION + ".jar";
    final Path localStorageUtilsJarPath = Paths.get(config.localStoragePath,
                                                    LOCAL_STORAGE_UTILS_FOLDER,
                                                    localStorageUtilsJarName);
    final boolean isSnapshot = VERSION.toLowerCase(Locale.ENGLISH).contains("snapshot");
    final boolean isInLocalStorage = Files.exists(localStorageUtilsJarPath);
    if (isSnapshot || !isInLocalStorage) {
      LOG.info("Copying {} to local storage", localStorageUtilsJarName);
      createDirectoriesIfNotExists(localStorageUtilsJarPath.getParent());
      writeResourceToFile(RESOURCES_JNOTEBOOK_UTILS_PATH,
                         localStorageUtilsJarPath);
    }
    return localStorageUtilsJarPath.toString();
  }
}
