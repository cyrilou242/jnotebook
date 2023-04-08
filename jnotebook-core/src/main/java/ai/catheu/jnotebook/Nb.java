package ai.catheu.jnotebook;

import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import static j2html.TagCreator.div;

// FIXME CYRIL move to another package
public class Nb {

  public static void vegaLite(final Map<String, Object> data) {
    final JSONObject json = new JSONObject(data);
    vegaLite(json);
  }

  public static void vegaLite(final JSONObject jsonData) {
    final DomContent res = vegaLiteHtml(jsonData);
    System.out.println(res);
  }

  private static DomContent vegaLiteHtml(JSONObject jsonData) {
    final DivTag chartContainer = div().withClasses("vega-lite vega-embed has-actions")
                                       .withData("config", jsonData.toString());
    return div(chartContainer).withClasses("overflow-x-auto");
  }

  public static void plotly(final List<Object> data, final Map<String, Object> layout, final Map<String, Object> config) {
    final JSONArray jsonData = new JSONArray(data);
    final JSONObject jsonLayout = new JSONObject(layout);
    final JSONObject jsonConfig = new JSONObject(config);
    plotly(jsonData, jsonLayout, jsonConfig);
  }

  private static void plotly(JSONArray jsonData, JSONObject jsonLayout, JSONObject jsonConfig) {
    final DomContent res = plotlyHtml(jsonData, jsonLayout, jsonConfig);
    System.out.println(res);
  }

  private static DomContent plotlyHtml(JSONArray jsonData, JSONObject jsonLayout, JSONObject jsonConfig) {
    final DivTag chartContainer = div().withClasses("plotly js-plotly-plot")
                                       .withData("data", jsonData.toString())
                                       .withData("layout", jsonLayout.toString())
                                       .withData("config", jsonConfig.toString());
    return div(chartContainer).withClasses("overflow-x-auto");
  }

  // tables
  // code
  // images
  // markdown
  // row
  // col
}
