package vertxreproducertcp;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleClient extends AbstractVerticle {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new SimpleClient(), ar1 -> {
      if (ar1.succeeded()) {
        System.out.println("Deployed Client");
      } else {
        ar1.cause().printStackTrace();
        ;
      }
    });
  }

  private static int num = 1000;
  private static byte[] data = new byte[1000];
  private static Stat stat = new Stat();

  static private void println(String s) {
    System.out.println(s);
  }

  @Override
  public void start() throws Exception {
    println("starting");
    start(1, Math.min(1000, num), num);
  }


  private void start(int start, int batch, int max) {
    for (int i = 1; i <= batch; i++) {
      stat.connecting();
//            stat.print()
      connectOneClient(i + start);
    }
    int nextStart = start + batch;
    if (nextStart <= max) {
      vertx.setTimer(10, (t) -> {
        start(nextStart, batch, max);
      });
    }
  }


  private void sendData(NetSocket socket, byte[] data, int num) {
    if (num > 1) {
      socket.write(Buffer.buffer(data), (it) -> {
        if (it.succeeded()) sendData(socket, data, num - 1);
        else it.cause().printStackTrace();
      });
    } else {
      socket.write(Buffer.buffer(data), (it) -> {
        socket.close();
        if (it.failed()) it.cause().printStackTrace();
      });
    }

  }

  private void connectOneClient(int clientNr) {
    NetClient netClient = vertx.createNetClient();
    netClient.connect(25252, "localhost", (asyncResult) -> {
      if (asyncResult.succeeded()) {
        stat.connected();
//                stat.print()

        NetSocket socket = asyncResult.result();
        socket.handler(buffer -> {
          stat.received(clientNr);
          stat.print();
          /* If you want to send some more data, uncomment one of the lines below.*/
//                    sendData(socket, data, 10); //will also close the socket when done
          socket.write("Simple Message from Client\r\n");
          socket.close();
        });
        socket.exceptionHandler(it -> {
          println("socket $clientNr exception");
          it.printStackTrace();
        });
        socket.endHandler(v -> {
          stat.end();
          stat.print();
        });
        /* Uncommenting this part will create the missing connections. Timer is optional */
//                vertx.setTimer(10000, t -> {
//                    socket.write("Hi from client");
//                });
      } else {
        println("Client " + clientNr + " error!");
        asyncResult.cause().printStackTrace();
      }
    });
  }


  private static class Stat {
    private AtomicInteger connecting = new AtomicInteger(0);
    private AtomicInteger connected = new AtomicInteger(0);
    private AtomicInteger end = new AtomicInteger(0);
    private Map<Integer, Boolean> receivedSomething = new HashMap<Integer, Boolean>();

    synchronized public void received(int nr) {
      receivedSomething.put(nr, true);
    }

    synchronized public void connecting() {
      connecting.incrementAndGet();
    }

    synchronized public void connected() {
      connecting.decrementAndGet();
      connected.incrementAndGet();
    }

    synchronized public void end() {
      connected.decrementAndGet();
      end.incrementAndGet();
    }

    synchronized int receivedSomething() {
      int c = 0;
      for (boolean v : receivedSomething.values()) {
        if (v) c++;
      }
      return c;
    }

    synchronized public void print() {
      println("connecting: " + connecting + " connected: " + connected + ", closed: " + end + ", (received some data: " + receivedSomething() + ")");
    }
  }
}
