package com.gasimo;

import org.snf4j.core.SelectorLoop;
import org.snf4j.core.factory.AbstractSessionFactory;
import org.snf4j.core.handler.IStreamHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;

/**
 * Handles networking initialisation and loop
 */
public class NetworkingInterpreter {

    boolean logClientRequests = true;

    static final String PREFIX = "org.gasimo.";
    static final int PORT = Integer.getInteger(PREFIX + "Port", Main.SP.PORT);

    public void init() throws IOException, InterruptedException, ExecutionException {
        SelectorLoop loop = new SelectorLoop();

        try {
            loop.start();

            // Initialize the listener
            ServerSocketChannel channel = ServerSocketChannel.open();
            channel.configureBlocking(false);
            channel.socket().bind(new InetSocketAddress(PORT));

            if(Main.enableConsoleColors)
                System.out.println("\033[36mServer is online at: " + getDefaultAddress() + "\033[0m");
            else
                System.out.println("Server is online at: " +  getDefaultAddress());


            // Register the listener
            loop.register(channel, new AbstractSessionFactory() {

                @Override
                protected IStreamHandler createHandler(SocketChannel channel) {

                    return new ServerHandler();
                }
            }).sync();

            // Wait till the loop ends
            loop.join();
        } finally {

            // Gently stop the loop
            loop.stop();
        }


    }


    /**
     * Get default IP address for remote connection.
     * @return Public IP Address
     */
    private String getDefaultAddress() {

        String sysIP = "";
        // Modified from https://www.geeksforgeeks.org/java-program-find-ip-address-computer/
        try
        {
            URL url_name = new URL("http://bot.whatismyipaddress.com");
            BufferedReader sc =
                    new BufferedReader(new InputStreamReader(url_name.openStream()));

            // reads system IPAddress
            sysIP = sc.readLine().trim();
            return sysIP;

        }
        catch (Exception e)
        {
            // In an unlikely scenario we are offline or the service above does not work.
            System.out.println("Could not obtain your public IP address, perhaps you are running offline?");
            return "127.0.0.1";
        }

    } // getDefaultAddress




}
