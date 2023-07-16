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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.catheu.jnotebook.evaluate.Interpreted;
import tech.catheu.jnotebook.evaluate.Interpreter;
import tech.catheu.jnotebook.evaluate.SimpleInterpreter;
import tech.catheu.jnotebook.jshell.ShellProvider;
import tech.catheu.jnotebook.parse.StaticParser;
import tech.catheu.jnotebook.parse.StaticParsing;
import tech.catheu.jnotebook.render.Renderer;
import tech.catheu.jnotebook.render.Rendering;
import tech.catheu.jnotebook.server.HtmlTemplateEngine;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static tech.catheu.jnotebook.server.HtmlTemplateEngine.TEMPLATE_KEY_CONFIG;
import static tech.catheu.jnotebook.server.HtmlTemplateEngine.TEMPLATE_KEY_RENDERED;

public class NotebookRenderer {

  private static final Logger LOG = LoggerFactory.getLogger(NotebookRenderer.class);
  private final StaticParser staticParser;
  private final Interpreter interpreter;
  private final Renderer renderer;

  public static NotebookRenderer from(final Main.SharedConfiguration config) {
    final ShellProvider shellProvider = new ShellProvider(config);
    final StaticParser staticParser = new StaticParser(shellProvider);
    final SimpleInterpreter interpreter = new SimpleInterpreter(shellProvider);
    final Renderer renderer = new Renderer(config);
    return new NotebookRenderer(staticParser, interpreter, renderer);
  }

  protected NotebookRenderer(final StaticParser staticParser,
                             final Interpreter interpreter, final Renderer renderer) {
    this.staticParser = staticParser;
    this.interpreter = interpreter;
    this.renderer = renderer;
  }

  public void render(final Main.RenderConfiguration config) {

    final Path filePath = Paths.get(config.inputPath);
    try {
      final StaticParsing staticParsing = staticParser.snippetsOf(filePath);
      final Interpreted interpreted = interpreter.interpret(staticParsing);
      final Rendering render = renderer.render(interpreted);
      final HtmlTemplateEngine templateEngine = new HtmlTemplateEngine();
      String html = templateEngine.render(Map.of(TEMPLATE_KEY_RENDERED,
                                                 render.html(),
                                                 TEMPLATE_KEY_CONFIG,
                                                 config));
      if (!config.noOptimize) {
        html = optimizeHtml(html);
      }

      final File outputFile = FileUtils.getFile(config.outputPath);
      FileUtils.write(outputFile, html, StandardCharsets.UTF_8);
      LOG.info("Notebook rendered successfully and written to {}", outputFile);
    } catch (Exception e) {
      throw new RuntimeException(String.format("Exception rendering notebook %s: ",
                                               filePath) + e);
    }
  }

  private String optimizeHtml(String html) {
    final Document originalDoc = Jsoup.parse(html);
    originalDoc.outputSettings().prettyPrint(false);
    final Elements noOptiScripts = originalDoc.head().select(".jnb-no-opti").remove();
    final String htmlForOpti = originalDoc.outerHtml();

    final byte[] htmlForOptiBytes = htmlForOpti.getBytes(StandardCharsets.UTF_8);
    // serve the page
    final int PORT = 8123; // FIXME CYRIL - pick less used port
    HttpServer server = null;
    try {
      server = HttpServer.create(new InetSocketAddress(PORT), 0);
    } catch (IOException e) {
      throw new RuntimeException();
    }
    server.createContext("/", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, htmlForOptiBytes.length);
        OutputStream responseStream = exchange.getResponseBody();
        responseStream.write(htmlForOptiBytes);
        responseStream.close();
      }
    });
    server.start();

    // render it with the chrome browser
    final WebDriver webDriver = new ChromeDriver();
    webDriver.get("http://localhost:" + PORT);
    String htmlWithOpti = webDriver.getPageSource();
    webDriver.quit();
    server.stop(0);

    final Document notebookWithOpti = Jsoup.parse(htmlWithOpti);
    notebookWithOpti.outputSettings().prettyPrint(false);
    // remove the scripts that are not necessary anymore
    notebookWithOpti.head().select(".jnb-opti").remove();

    // put back scripts that can't be optimized
    // TODO - filter scripts and stylesheet that are not used
    notebookWithOpti.head().appendChildren(noOptiScripts);

    // Get the modified HTML let's go
    return notebookWithOpti.outerHtml();
  }
}
