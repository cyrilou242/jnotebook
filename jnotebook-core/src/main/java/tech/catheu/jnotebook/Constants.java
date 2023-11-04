/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook;

import tech.catheu.jnotebook.localstorage.LocalStorage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import static com.google.common.base.Preconditions.checkState;

public class Constants {

  private static final String RESOURCES_VERSION_FILE = "/version.txt";
  public static final String VERSION = readResourcesFile(RESOURCES_VERSION_FILE).trim();

  private static String readResourcesFile(final String resourcesPath) {
    try (InputStream inputStream = LocalStorage.class.getResourceAsStream(resourcesPath)) {
      checkState(inputStream != null,
                 "Failed reading resources file in path %s",
                 resourcesPath);
      try (Scanner scanner = new Scanner(inputStream).useDelimiter("\\A")) {
        if (scanner.hasNext()) {
          return scanner.next();
        }
        throw new IOException();
      }
    } catch (IOException e) {
      throw new RuntimeException(String.format(
              "Failed reading jnotebook version from resources %s",
              RESOURCES_VERSION_FILE));
    }
  }
}
