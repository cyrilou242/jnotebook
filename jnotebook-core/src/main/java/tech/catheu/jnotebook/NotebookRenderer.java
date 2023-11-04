/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
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
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
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
    // remove scripts that cannot be optimized
    final Document originalDoc = Jsoup.parse(html);
    originalDoc.outputSettings().prettyPrint(false);
    final Elements noOptiScripts = originalDoc.head().select(".jnb-no-opti").remove();
    final String htmlForOpti = originalDoc.outerHtml();

    // serve file
    HtmlFileServer miniServer = getMiniServer(htmlForOpti);
    miniServer.server.start();
    // use selenium to render the file with javascript in a browser and parse the content
    ChromeOptions options = new ChromeOptions().addArguments("--headless=new");
    WebDriverManager.chromedriver().setup();
    final WebDriver webDriver = new ChromeDriver(options);
    webDriver.get(miniServer.url);
    final String htmlWithOpti = webDriver.getPageSource();
    webDriver.quit();
    miniServer.server().stop(0);

    // remove scripts that were optimized
    final Document notebookWithOpti = Jsoup.parse(htmlWithOpti);
    notebookWithOpti.outputSettings().prettyPrint(false);
    // remove the scripts that are not necessary anymore
    notebookWithOpti.head().select(".jnb-opti").remove();

    // put back scripts that cannot be optimized
    // TODO - filter scripts and stylesheet that are not used
    notebookWithOpti.head().appendChildren(noOptiScripts);

    return "<!DOCTYPE html>\n" + notebookWithOpti.outerHtml();
  }

  @NotNull
  private NotebookRenderer.HtmlFileServer getMiniServer(final String htmlFile) {
    final byte[] htmlBytes = htmlFile.getBytes(StandardCharsets.UTF_8);
    try {
      final int port = getFreePort();
      final HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
      server.createContext("/", new HttpHandler() {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
          exchange.getResponseHeaders().set("Content-Type", "text/html");
          exchange.sendResponseHeaders(200, htmlBytes.length);
          OutputStream responseStream = exchange.getResponseBody();
          responseStream.write(htmlBytes);
          responseStream.close();
        }
      });
      final HtmlFileServer result = new HtmlFileServer("http://localhost:" + port, server);
      return result;
    } catch (Exception e) {
      LOG.error("Failed to create a server: ", e);
      throw new RuntimeException("Failed to create a server.", e);
    }
  }

  private int getFreePort() {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      checkNotNull(serverSocket,
                   "Failed to find a free port: failed creating a ServerSocket instance.");
      checkState(serverSocket.getLocalPort() > 0, "Failed to find a free port.");
      final int localPort = serverSocket.getLocalPort();
      serverSocket.close();
      return localPort;
    } catch (IOException e) {
      throw new RuntimeException("Failed to find a free port", e);
    }
  }

  private record HtmlFileServer(String url, HttpServer server) {
  }
}
