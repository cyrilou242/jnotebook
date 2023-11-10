/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook.jshell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jnotebook.Main;
import tech.catheu.jnotebook.localstorage.LocalStorage;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static tech.catheu.jnotebook.Main.SharedConfiguration.AUTO_CLASSPATH;
import static tech.catheu.jnotebook.utils.JavaUtils.optional;

public class ShellProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ShellProvider.class);
  private static final String MAVEN_PROJECT_FILE = "pom.xml";
  private static final String MAVEN_WRAPPER_FILE =
          IS_OS_WINDOWS ? "mvnw.cmd" : "mvnw";
  private static final String GRADLE_PROJECT_FILE = "build.gradle";
  public static final String MAVEN_DEPENDENCY_COMMAND =
          " -q exec:exec -Dexec.executable=echo -Dexec.args=\"%classpath\"";
  public static final String MAVEN_DEPENDENCY_COMMAND_WINDOWS =
          " -q exec:exec -Dexec^.executable=cmd -Dexec^.args=\"/c echo %classpath\"";
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
    return optional(preparedShells.poll()).orElse(this.newShell());
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
    if (!AUTO_CLASSPATH.equals(configuration.classPath)) {
      LOG.info("Injecting provided classpath: " + configuration.classPath);
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
    final String mavenCommand =
            IS_OS_WINDOWS ? MAVEN_DEPENDENCY_COMMAND_WINDOWS : MAVEN_DEPENDENCY_COMMAND;
    final String cmd = mavenExecutable + mavenCommand;
    final Runtime run = Runtime.getRuntime();
    final Process pr = run.exec(cmd);
    final int exitCode = pr.waitFor();
    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
      if (exitCode != 0) {
        throw new RuntimeException(("""
                Failed to add maven dependencies to the classpath.
                Maven command finished with exit code %s.
                Maven command: %s.
                Error: %s""").formatted(
                exitCode,
                cmd, reader.lines().collect(Collectors.joining("\n"))));
      }
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
