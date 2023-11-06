/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook;

import com.microsoft.jfr.*;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static j2html.TagCreator.*;
import static tech.catheu.jnotebook.FlameGraph.flameGraphD3;
import static tech.catheu.jnotebook.FlameGraph.flameGraphLog;

public class Nb {

  private final static DateTimeFormatter DATE_TIME_FORMATTER =
          DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH-mm-ss-SSS");

  private Nb() {
  }

  public static DivTag vega(final Map<String, Object> data) {
    final JSONObject json = new JSONObject(data);
    return vega(json);
  }

  public static DivTag vega(final JSONObject jsonData) {
    final DivTag chartContainer = div().withClasses("vega-lite vega-embed has-actions")
                                       .withData("config", jsonData.toString());
    return div(chartContainer).withClasses("overflow-x-auto");
  }

  public static DivTag plotly(final List<Object> data, final Map<String, Object> layout,
                              final Map<String, Object> config) {
    return plotly(data, layout, config, null);
  }

  public static DivTag plotly(final List<Object> data, final Map<String, Object> layout,
                              final Map<String, Object> config, final List<Map<String, List<Map<String, ?>>>> frames) {
    final JSONArray jsonData = new JSONArray(data);
    final JSONObject jsonLayout = new JSONObject(layout);
    final JSONObject jsonConfig = new JSONObject(config);
    final JSONArray jsonFrames = Optional.ofNullable(frames).map(JSONArray::new).orElse(null);
    return plotly(jsonData, jsonLayout, jsonConfig, jsonFrames);
  }

  public static DivTag plotly(JSONArray jsonData, JSONObject jsonLayout,
                              JSONObject jsonConfig) {
    return plotly(jsonData, jsonLayout, jsonConfig);
  }

  public static DivTag plotly(JSONArray jsonData, JSONObject jsonLayout,
                              JSONObject jsonConfig, JSONArray jsonFrames) {
    DivTag chartContainer = div().withClasses("plotly js-plotly-plot")
                                       .withData("data", jsonData.toString())
                                       .withData("layout", jsonLayout.toString())
                                       .withData("config", jsonConfig.toString());
    if (jsonFrames != null) {
      chartContainer = chartContainer.withData("frames", jsonFrames.toString());
    }
    return div(chartContainer).withClasses("overflow-x-auto");
  }

  public static DomContent row(final Object... objects) {
    return div(each(Arrays.asList(objects), Nb::html)).withClasses("grid",
                                                                   "grid-flow-row");
  }

  public static DomContent col(final Object... objects) {
    int numCols = objects.length;
    if (numCols > 12) {
      throw new IllegalArgumentException(String.format(
              "Invalid number of inputs: %s. The maximum number is 12. Please use a grid for more elements",
              objects.length));
    }
    return div(each(Arrays.asList(objects), Nb::html)).withClasses("grid",
                                                                   "grid-cols-" + numCols);
  }

  public static DomContent grid(final int maxCols, final Object... objects) {
    if (maxCols < 1 || maxCols > 12) {
      throw new IllegalArgumentException(String.format(
              "Invalid maxCols value: %s. maxCols must be between 1 and 12",
              maxCols));
    }
    return div(each(Arrays.asList(objects), Nb::html)).withClasses("grid",
                                                                   "grid-cols-" + maxCols);
  }

  public static DomContent html(final Object obj) {
    if (obj instanceof DomContent content) {
      return content;
    }
    return div(rawHtml(obj.toString()));
  }

  public static Path profile(final Runnable runnable) {
    MBeanServerConnection mBeanServer = ManagementFactory.getPlatformMBeanServer();
    RecordingOptions recordingOptions =
            new RecordingOptions.Builder().disk("true").build();
    RecordingConfiguration recordingConfiguration =
            RecordingConfiguration.PROFILE_CONFIGURATION;

    final String filename =
            "notebook-profile-" + DATE_TIME_FORMATTER.format(LocalDateTime.now()) + ".jfr";
    final Path savePath = Paths.get(System.getProperty("user.dir"), filename);
    return profile(runnable,
                   mBeanServer,
                   recordingOptions,
                   recordingConfiguration,
                   savePath);
  }

  public static Path profile(final Runnable runnable,
                             final MBeanServerConnection mBeanServer,
                             final RecordingOptions recordingOptions,
                             final RecordingConfiguration recordingConfiguration,
                             final Path savePath) {
    final ProfiledRunnable profiledRunnable = new ProfiledRunnable(runnable);
    try {
      FlightRecorderConnection flightRecorderConnection =
              FlightRecorderConnection.connect(mBeanServer);
      try (Recording recording = flightRecorderConnection.newRecording(recordingOptions,
                                                                       recordingConfiguration)) {
        recording.start();
        profiledRunnable.run();
        recording.stop();
        recording.dump(savePath.toString());
        return savePath;
      }
    } catch (InstanceNotFoundException | IOException | JfrStreamingException e) {
      throw new RuntimeException(e);
    }
  }

  public static DomContent flame(final Path path) {
    final Map<String, Object> profile = flameGraphD3(flameGraphLog(path));
    final JSONObject jsonProfile = new JSONObject(profile);

    final DivTag chartContainer =
            div().withClasses("flame").withData("profile", jsonProfile.toString());

    return div(chartContainer).withClasses("overflow-x-auto");
  }

  /**
   * Wrapper class that makes it easier to find the Runnable being profiled by {@link #profile(Runnable)}
   * when inspecting the jfr logs.
   */
  private static class ProfiledRunnable implements Runnable {

    private final Runnable delegate;

    private ProfiledRunnable(final Runnable delegate) {
      this.delegate = delegate;
    }

    @Override
    public void run() {
      delegate.run();
    }
  }
}
