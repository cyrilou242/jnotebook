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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jnotebook.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class LocalStorage {

  private static final Logger LOG = LoggerFactory.getLogger(LocalStorage.class);
  private static final String RESOURCES_UTILS_FOLDER = "/utils/";
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
    final String resourceUtilsJarName = getUtilsJarName();
    final Path localStorageUtilsJarPath = Paths.get(config.localStoragePath,
                                                    LOCAL_STORAGE_UTILS_FOLDER,
                                                    resourceUtilsJarName);
    final boolean isSnapshot =
            resourceUtilsJarName.toLowerCase(Locale.ENGLISH).contains("snapshot");
    final boolean isInLocalStorage = Files.exists(localStorageUtilsJarPath);
    if (isSnapshot || !isInLocalStorage) {
      LOG.info("Copying {} to local storage", resourceUtilsJarName);
      final Path resourcesUtilsJarPath =
              Paths.get(RESOURCES_UTILS_FOLDER, resourceUtilsJarName);
      copyResourcesToFile(resourcesUtilsJarPath, localStorageUtilsJarPath);
    }
    return localStorageUtilsJarPath.toString();
  }

  private static void copyResourcesToFile(final Path resourcesPath, final Path filePath) {
    try (final InputStream in = LocalStorage.class.getResourceAsStream(resourcesPath.toString())) {
      checkState(in != null, "Failed reading %s from resources", resourcesPath);
      createIfNotExists(filePath.getParent());
      Files.copy(in, filePath, REPLACE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException(String.format("Failed copying resource %s to file %s: %s",
                                               resourcesPath,
                                               filePath,
                                               e));
    }
  }

  private String getUtilsJarName() {
    // the resources utils folder should contain a single file jnotebook-utils-VERSION.jar
    final List<String> files = listFiles(RESOURCES_UTILS_FOLDER);
    checkState(files.size() == 1);
    return files.get(0);
  }

  private static void createIfNotExists(final Path folderPath) {
    try {
      if (!Files.exists(folderPath)) {
        LOG.info("{} folder does not exist. Creating folder.", folderPath);
        Files.createDirectories(folderPath);
      }
    } catch (Exception e) {
      LOG.error("Failed creating folder {}. Error: {}", folderPath, e.getMessage());
      throw new RuntimeException(e); // don't try to recover if the system is not able to create folders
    }
  }

  @NonNull
  private static List<String> listFiles(final String resourcePath) {
    final List<String> files = new ArrayList<>();
    try (final InputStream in = LocalStorage.class.getResourceAsStream(resourcePath); final BufferedReader br = new BufferedReader(
            new InputStreamReader(in))) {
      String resource;
      while ((resource = br.readLine()) != null) {
        files.add(resource);
      }
    } catch (IOException e) {
      throw new RuntimeException(String.format("Could not load alert json: %s", e));
    }
    return files;
  }

}
