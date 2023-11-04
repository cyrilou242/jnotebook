/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
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
