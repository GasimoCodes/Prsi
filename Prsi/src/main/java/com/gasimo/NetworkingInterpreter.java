package com.gasimo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;

import org.snf4j.core.SelectorLoop;
import org.snf4j.core.factory.AbstractSessionFactory;
import org.snf4j.core.handler.IStreamHandler;


public class NetworkingInterpreter {

    boolean logClientRequests = true;

    static final String PREFIX = "org.gasimo.";
    static final int PORT = Integer.getInteger(PREFIX+"Port", 8002);

    public void init() throws IOException, InterruptedException, ExecutionException {
        SelectorLoop loop = new SelectorLoop();

        try {
            loop.start();

            // Initialize the listener
            ServerSocketChannel channel = ServerSocketChannel.open();
            channel.configureBlocking(false);
            channel.socket().bind(new InetSocketAddress(PORT));

            // Register the listener
            loop.register(channel, new AbstractSessionFactory() {

                @Override
                protected IStreamHandler createHandler(SocketChannel channel) {

                    return new ServerHandler();
                }
            }).sync();

            // Wait till the loop ends
            loop.join();
        }
        finally {

            // Gently stop the loop
            loop.stop();
        }
    }

}
