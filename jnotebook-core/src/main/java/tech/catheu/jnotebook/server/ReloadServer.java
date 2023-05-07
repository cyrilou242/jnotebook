package tech.catheu.jnotebook.server;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import tech.catheu.jnotebook.Main;

import java.io.IOException;

public class ReloadServer {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  private final Main.InteractiveConfiguration configuration;

  private Undertow server;
  private WebSocketChannel webSocketChannel;
  XnioWorker worker;

  public ReloadServer(final Main.InteractiveConfiguration configuration) {
    this.configuration = configuration;
  }


  public void start() throws IOException {
    RoutingHandler routingHandler = Handlers.routing()
                                            .get("/", new TemplatedHttpHandler(configuration))
                                            .get("/websocket",
                                                 new WebSocketProtocolHandshakeHandler(new ConnectionCallback()));
    server = Undertow.builder()
                     .addHttpListener(configuration.port, "localhost")
                     .setHandler(routingHandler)
                     .build();

    // not sure if the config is relevant
    XnioWorker worker = Xnio.getInstance()
                            .createWorker(OptionMap.builder()
                                                   .set(Options.WORKER_IO_THREADS, 10)
                                                   .set(Options.TCP_NODELAY, true)
                                                   .set(Options.CORK, true)
                                                   .set(Options.BACKLOG, 10000)
                                                   .getMap());

    worker.execute(new ServerLauncher());
  }

  private class ServerLauncher implements Runnable {
    @Override
    public void run() {
      try {
        server.start();
      } catch (Exception e) {
        LOG.error("Server error. Shutting down notebook server.", e);
        System.exit(1);
      }
    }
  }

  private static class TemplatedHttpHandler implements HttpHandler {

    final TemplateEngine templateEngine;
    final Context context;
    final Main.InteractiveConfiguration configuration;

    TemplatedHttpHandler(final Main.InteractiveConfiguration configuration) {
      this.configuration = configuration;
      this.templateEngine = createTemplateEngine();
      this.context = new Context();
      this.context.setVariable("config", this.configuration);
    }

    private static TemplateEngine createTemplateEngine() {
      ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
      resolver.setPrefix("frontend/");
      resolver.setSuffix(".html");

      TemplateEngine engine = new TemplateEngine();
      engine.setTemplateResolver(resolver);

      return engine;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
      String html = templateEngine.process("index", context);

      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
      exchange.getResponseSender().send(html);
    }
  }


  public void sendReload() {
    sendMessage("reload");
  }

  public void sendUpdate(final String html) {
    sendMessage(html);
  }

  private void sendMessage(final String message) {
    if (webSocketChannel != null && webSocketChannel.isOpen()) {
      WebSockets.sendText(message, webSocketChannel, null);
    } else {
      LOG.error(
              "ERROR: trying to send updates but no client is opened. Go to http://localhost:" + configuration.port);
    }
  }

  public void stop() throws IOException {
    if (worker != null) {
      worker.shutdown();
    }
    if (server != null) {
      server.stop();
    }
    if (webSocketChannel != null) {
      webSocketChannel.close();
    }
  }

  private class ConnectionCallback implements WebSocketConnectionCallback {
    @Override
    public void onConnect(WebSocketHttpExchange webSocketHttpExchange, WebSocketChannel channel) {
      // send close signal to previous connection
      if (webSocketChannel != null) {
        sendMessage("close");
      }
      webSocketChannel = channel;
    }
  }
}