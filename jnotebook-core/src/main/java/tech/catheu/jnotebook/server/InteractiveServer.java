/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook.server;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import tech.catheu.jnotebook.Main;
import tech.catheu.jnotebook.render.Rendering;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class InteractiveServer {

  private static final Logger LOG = LoggerFactory.getLogger(InteractiveServer.class);

  private final Main.InteractiveConfiguration configuration;
  private final Consumer<Path> renderTrigger;

  private Undertow server;
  private final List<WebSocketChannel> channels = new ArrayList<>();
  XnioWorker worker;
  private Rendering lastUpdate;

  public InteractiveServer(final Main.InteractiveConfiguration configuration,
                           Consumer<Path> renderTrigger) {
    this.configuration = configuration;
    this.renderTrigger = renderTrigger;
  }


  public void start() throws IOException {
    RoutingHandler routingHandler = Handlers.routing()
                                            .get("/",
                                                 new TemplatedHttpHandler(configuration))
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

    final HtmlTemplateEngine templateEngine;
    final Main.InteractiveConfiguration configuration;

    TemplatedHttpHandler(final Main.InteractiveConfiguration configuration) {
      this.configuration = configuration;
      this.templateEngine = new HtmlTemplateEngine();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
      final String html = templateEngine.render(this.configuration, true, null);
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
      exchange.getResponseSender().send(html);
    }
  }

  public void sendStatus(NotebookServerStatus status) {
    sendMessage("status_" + status.toString());
  }

  public void sendUpdate(final Rendering rendering) {
    sendStatus(NotebookServerStatus.TRANSFER);
    lastUpdate = rendering;
    sendMessage(rendering.html());
  }

  private void sendMessage(final String message) {
    boolean messageSent = false;
    for (final WebSocketChannel channel : channels) {
      if (channel != null && channel.isOpen()) {
        WebSockets.sendText(message, channel, null);
        messageSent = true;
      }
    }
    if (!messageSent) {
      LOG.error("ERROR: trying to send updates but no client is opened. Go to http://localhost:" + configuration.port);
    }
  }

  public void stop() throws IOException {
    if (worker != null) {
      worker.shutdown();
    }
    if (server != null) {
      server.stop();
    }
    for (final WebSocketChannel channel : channels) {
      if (channel != null) {
        channel.close();
      }
    }
  }

  private class ConnectionCallback implements WebSocketConnectionCallback {
    @Override
    public void onConnect(WebSocketHttpExchange webSocketHttpExchange,
                          WebSocketChannel channel) {
      channels.add(channel);
      // resend to every channel - not very correct but simpler for the moment
      sendStatus(NotebookServerStatus.CONNECTED);
      setupReceiver(channel);
      if (lastUpdate != null) {
        // resend to every channel - not necessary but simpler for the moment
        sendUpdate(lastUpdate);
      }
    }

    private void setupReceiver(WebSocketChannel channel) {
      channel.getReceiveSetter().set(new AbstractReceiveListener() {
        @Override
        protected void onFullTextMessage(WebSocketChannel channel,
                                         BufferedTextMessage message) throws IOException {
          final String messageText = message.getData();
          if (messageText.startsWith("refresh_")) {
            final String substring = messageText.substring(8);
            LOG.info("Triggering refresh for file {}", substring);
            renderTrigger.accept(Path.of(substring));
          } else {
            LOG.error("Received unsupported message from websocket: {}", messageText);
          }
        }
      });
      channel.resumeReceives();
    }
  }
}
