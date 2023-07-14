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
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import tech.catheu.jnotebook.Main;
import tech.catheu.jnotebook.render.Rendering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static tech.catheu.jnotebook.server.HtmlTemplateEngine.TEMPLATE_KEY_INTERACTIVE;
import static tech.catheu.jnotebook.server.HtmlTemplateEngine.TEMPLATE_KEY_CONFIG;

public class InteractiveServer {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  private final Main.InteractiveConfiguration configuration;

  private Undertow server;
  private final List<WebSocketChannel> channels = new ArrayList<>();
  XnioWorker worker;
  private Rendering lastUpdate;

  public InteractiveServer(final Main.InteractiveConfiguration configuration) {
    this.configuration = configuration;
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
    final Map<String, Object> context;
    final Main.InteractiveConfiguration configuration;

    TemplatedHttpHandler(final Main.InteractiveConfiguration configuration) {
      this.configuration = configuration;
      this.templateEngine = new HtmlTemplateEngine();
      this.context = Map.of(TEMPLATE_KEY_INTERACTIVE, true,
                            TEMPLATE_KEY_CONFIG, this.configuration);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
      final String html = templateEngine.render(context);
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
      if (lastUpdate != null) {
        // resend to every channel - not necessary but simpler for the moment
        sendUpdate(lastUpdate);
      }
    }
  }
}
