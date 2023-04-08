package tech.catheu.jnotebook;

import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static j2html.TagCreator.*;

public class Nb {

  public static DivTag vega(final Map<String, Object> data) {
    final JSONObject json = new JSONObject(data);
    return vega(json);
  }

  public static DivTag vega(final JSONObject jsonData) {
    final DivTag chartContainer = div().withClasses("vega-lite vega-embed has-actions")
                                       .withData("config", jsonData.toString());
    return div(chartContainer).withClasses("overflow-x-auto");
  }

  public static DivTag plotly(final List<Object> data, final Map<String, Object> layout, final Map<String, Object> config) {
    final JSONArray jsonData = new JSONArray(data);
    final JSONObject jsonLayout = new JSONObject(layout);
    final JSONObject jsonConfig = new JSONObject(config);
    return plotly(jsonData, jsonLayout, jsonConfig);
  }

  public static DivTag plotly(JSONArray jsonData, JSONObject jsonLayout, JSONObject jsonConfig) {
    final DivTag chartContainer = div().withClasses("plotly js-plotly-plot")
                                       .withData("data", jsonData.toString())
                                       .withData("layout", jsonLayout.toString())
                                       .withData("config", jsonConfig.toString());
    return div(chartContainer).withClasses("overflow-x-auto");
  }

  public static DomContent row(final Object... objects) {
    return div(each(Arrays.asList(objects), Nb::html)).withClasses("grid", "grid-flow-row");
  }

  public static DomContent col(final Object... objects) {
    return div(each(Arrays.asList(objects), Nb::html)).withClasses("grid", "grid-flow-col");
  }

  public static DomContent grid(final int maxCols, final Object... objects) {
    if (maxCols < 1 || maxCols > 12) {
      throw new IllegalArgumentException(String.format("Invalid maxCols value: %s. maxCols must be between 1 and 12", maxCols));
    }
    return div(each(Arrays.asList(objects), Nb::html)).withClasses("grid", "grid-cols-" + maxCols);
  }

  public static DomContent html(final Object obj) {
    if (obj instanceof DomContent content) {
      return content;
    }
    return div(rawHtml(obj.toString()));
  }
}
