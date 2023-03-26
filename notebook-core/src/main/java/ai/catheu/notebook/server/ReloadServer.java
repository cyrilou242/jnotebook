package ai.catheu.notebook.server;

import ai.catheu.notebook.Main;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
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

import java.io.IOException;

//TODO CYRIL a full reload is ok to start with
// partial reload can be implemented later using ids and replacing ids by ids?
// not sure how to insert between ids yet - maybe a linked list, or a notion of preceding node
// but preceding is annoying at delete time - maybe soft delete by emptying
public class ReloadServer {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  private Undertow server;
  private WebSocketChannel webSocketChannel;
  XnioWorker worker;

  public void start() throws IOException {
    // Create the resource handler for serving static files
    ResourceHandler resourceHandler = new ResourceHandler(new ClassPathResourceManager(
            getClass().getClassLoader(),
            "frontend"));

    // Create the routing handler for handling WebSocket requests
    RoutingHandler routingHandler = Handlers.routing()
                                            .get("/", resourceHandler)
                                            .get("/{path}", resourceHandler)
                                            .get("/websocket",
                                                 new WebSocketProtocolHandshakeHandler((WebSocketConnectionCallback) (webSocketHttpExchange, channel) ->
                                                         webSocketChannel = channel));

    // Create the Undertow server and start it
    server = Undertow.builder()
                     .addHttpListener(5002, "localhost")
                     .setHandler(routingHandler)
                     .build();

    // Create a worker thread pool with 10 threads
    XnioWorker worker = Xnio.getInstance()
                            .createWorker(OptionMap.builder()
                                                   .set(Options.WORKER_IO_THREADS, 10)
                                                   .set(Options.TCP_NODELAY, true)
                                                   .set(Options.CORK, true)
                                                   .set(Options.BACKLOG, 10000)
                                                   .getMap());

    worker.execute(server::start);
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
      LOG.error("ERROR: trying to send updates but no client is opened. Go to http://localhost:5002");
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
      // last connection opened takes precedence
      // would be nice to instruct the previous one to close itself
      webSocketChannel = channel;
    }
  }
}
