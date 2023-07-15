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
package tech.catheu.jnotebook.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jnotebook.localstorage.LocalStorage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileUtils {

  public static final Logger LOG = LoggerFactory.getLogger(LocalStorage.class);

  public static Path createDirectoriesUnchecked(final Path dir,
                                                final FileAttribute<?>... attrs) {
    try {
      return Files.createDirectories(dir, attrs);
    } catch (IOException e) {
      LOG.error("Failed creating folder {}. Error: {}",
                dir.toAbsolutePath(),
                e.getMessage());
      throw new RuntimeException(e);
    }
  }

  public static void createDirectoriesIfNotExists(final Path folderPath) {
    if (!Files.exists(folderPath)) {
      LOG.info("{} folder does not exist. Creating folder.", folderPath.toAbsolutePath());
      FileUtils.createDirectoriesUnchecked(folderPath);
    }
  }

  // assumes the filePath exists
  public static void writeResourceToFile(final String resourcePath, final Path filePath) {
    try (final InputStream in = LocalStorage.class.getResourceAsStream(resourcePath)) {
      checkState(in != null, "Failed reading %s from resources", resourcePath);
      Files.copy(in, filePath, REPLACE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException(java.lang.String.format(
              "Failed copying resource %s to file %s: %s",
              resourcePath,
              filePath,
              e));
    }
  }
}
