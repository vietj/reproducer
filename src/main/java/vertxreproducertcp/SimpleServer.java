package vertxreproducertcp;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleServer extends AbstractVerticle {

    private static Stat stat = new Stat();
    private static AtomicInteger idCounter = new AtomicInteger(0);

    static private void println(String s){
        System.out.println(s);
    }

    @Override
    public void start() throws Exception {
        println("starting");
        NetServer server = vertx.createNetServer(new NetServerOptions().setPort(25252));

        server.connectHandler((socket)->{
            int id= idCounter.incrementAndGet();
            stat.connected();
            stat.print();

            socket.handler((buffer)->{
               stat.received(id);
            });

            socket.exceptionHandler((e)->{
                println("error for client "+id);
                e.printStackTrace();
            });

            socket.endHandler((v)->{
               stat.end();
               stat.print();
            });
            socket.write("Hello");

        });
        server.listen();
    }


    private static class Stat{
        private AtomicInteger connected= new AtomicInteger(0);
        private AtomicInteger end= new AtomicInteger(0);
        private Map<Integer,Boolean> receivedSomething= new HashMap<Integer,Boolean>();

        synchronized public void received(int nr){
            receivedSomething.put(nr,true);
        }

        synchronized public void connected(){
            connected.incrementAndGet();
        }

        synchronized public void end(){
            connected.decrementAndGet();
            end.incrementAndGet();
        }

        synchronized public int receivedSomething(){
            int c=0;
            for(boolean v:receivedSomething.values()){
                if (v) c++;
            }
            return c;
        }

        synchronized public void print(){
            println("connected: "+connected+", closed: "+end+", (received some data: "+receivedSomething()+")");
        }
    }
}
