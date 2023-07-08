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
package tech.catheu.jnotebook.jshell;

import tech.catheu.jnotebook.Main;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jnotebook.localstorage.LocalStorage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ShellProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ShellProvider.class);
  private static final String MAVEN_PROJECT_FILE = "pom.xml";
  private static final String MAVEN_WRAPPER_FILE =
          SystemUtils.IS_OS_WINDOWS ? "mvnw.cmd" : "mvnw";
  private static final String GRADLE_PROJECT_FILE = "build.gradle";
  public static final String MAVEN_DEPENDENCY_COMMAND =
          " -q exec:exec -Dexec.executable=echo -Dexec.args=\"%classpath\"";
  private final Deque<PowerJShell> preparedShells;
  private final Main.SharedConfiguration configuration;

  private String resolvedClasspath = null;
  private final LocalStorage localStorage;

  public ShellProvider(final Main.SharedConfiguration configuration) {
    this.configuration = configuration;
    this.preparedShells = new ArrayDeque<>(2);
    this.localStorage = LocalStorage.instanceFor(configuration);

    warmUp();
  }

  private void warmUp() {
    // pre-init two shells to make the first rendering feel faster
    this.preparedShells.add(newShell());
    this.preparedShells.add(newShell());
  }

  public PowerJShell getShell() {
    if (!preparedShells.isEmpty()) {
      return preparedShells.pop();
    }
    return newShell();
  }

  private PowerJShell newShell() {
    final String classPath = getClassPath();
    final PowerJShell.Configuration powerJShellConfig =
            new PowerJShell.Configuration(classPath);
    return new PowerJShell(powerJShellConfig);
  }

  private String getClassPath() {
    if (resolvedClasspath != null) {
      return resolvedClasspath;
    }
    if (!configuration.classPath.isEmpty()) {
      resolvedClasspath = configuration.classPath;
    } else if (new File(MAVEN_PROJECT_FILE).exists()) {
      try {
        LOG.info(
                "Found a pom.xml file. Trying to add maven dependencies to the classpath...");
        resolvedClasspath = computeMavenClasspath();
        LOG.info("Maven dependencies added to the classpath successfully");
      } catch (IOException | InterruptedException e) {
        LOG.error("Failed resolving maven dependencies in pom.xml.", e);
        resolvedClasspath = configuration.classPath;
      }
    } else if (new File(GRADLE_PROJECT_FILE).exists()) {
      LOG.warn(
              "Automatic inclusion of classpath with gradle is not implemented. Use --class-path argument to pass manually.");
      resolvedClasspath = configuration.classPath;
    }

    if (configuration.noUtils) {
      LOG.info("Skipping injection of notebook utils in the classpath.");
    } else {
      LOG.info("Injecting notebook utils in the classpath.");
      resolvedClasspath += ":" + localStorage.getUtilsPath();
    }

    return resolvedClasspath;
  }

  private String computeMavenClasspath() throws IOException, InterruptedException {
    final File mavenWrapper = lookForFile(MAVEN_WRAPPER_FILE, new File(""), 0);
    final String mavenExecutable;
    if (mavenWrapper != null) {
      mavenExecutable = mavenWrapper.getAbsolutePath();
    } else {
      LOG.warn("Maven wrapper not found. Trying to use `mvn` directly.");
      mavenExecutable = "mvn";
    }
    final String cmd = mavenExecutable + MAVEN_DEPENDENCY_COMMAND;
    final Runtime run = Runtime.getRuntime();
    final Process pr = run.exec(cmd);
    pr.waitFor();
    final BufferedReader reader =
            new BufferedReader(new InputStreamReader(pr.getInputStream()));
    final List<String> classpaths = reader.lines().toList();
    if (classpaths.isEmpty()) {
      LOG.warn("Maven dependencies command ran successfully, but classpath is empty");
      return "";
    } else if (classpaths.size() == 1) {
      return classpaths.get(0);
    } else {
      LOG.warn(
              "Maven dependencies command ran successfully, but multiple classpath were returned. This can happen with multi-modules projects. Combining all classpath.");
      return String.join(":", classpaths);
    }
  }

  /**
   * Look for a file. If not found, look into the parent.
   */
  private static File lookForFile(final String filename, final File startDirectory,
                                  final int depthLimit) {
    final File absoluteDirectory = startDirectory.getAbsoluteFile();
    if (new File(absoluteDirectory, filename).exists()) {
      return new File(absoluteDirectory, filename);
    } else {
      File parentDirectory = absoluteDirectory.getParentFile();
      if (parentDirectory != null && depthLimit > 0) {
        return lookForFile(filename, parentDirectory, depthLimit - 1);
      } else {
        return null;
      }
    }
  }
}
