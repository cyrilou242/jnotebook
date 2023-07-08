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
package tech.catheu.jnotebook;

import tech.catheu.jnotebook.localstorage.LocalStorage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class Constants {

  private static final String RESOURCES_VERSION_FILE = "/version.txt";
  public static final String VERSION = readResourcesFile(RESOURCES_VERSION_FILE).trim();

  private static String readResourcesFile(final String resourcesPath) {
    try (InputStream inputStream = LocalStorage.class
            .getResourceAsStream(resourcesPath); Scanner scanner = new Scanner(
            inputStream).useDelimiter("\\A")) {
      if (scanner.hasNext()) {
        return scanner.next();
      }
      throw new IOException();
    } catch (IOException e) {
      throw new RuntimeException(String.format(
              "Failed reading jnotebook version from resources %s",
              RESOURCES_VERSION_FILE));
    }
  }
}
