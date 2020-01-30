package vertxreproducertcp;

import io.vertx.core.Vertx;

public class Main {

  public static void main(String[] args) {

    Vertx vertx = Vertx.vertx();

    vertx.deployVerticle(new SimpleServer(), ar1 -> {
      if (ar1.succeeded()) {
        System.out.println("Deployed 1");
        vertx.deployVerticle(new SimpleClient(), ar2 -> {
          if (ar2.succeeded()) {
            System.out.println("Deployed 2");
          } else {
            ar2.cause().printStackTrace();
          }
        });
      } else {
        ar1.cause().printStackTrace();
        ;
      }
    });
  }
}
