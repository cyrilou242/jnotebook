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
