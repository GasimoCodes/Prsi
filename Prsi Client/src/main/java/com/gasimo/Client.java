package com.gasimo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import com.google.gson.Gson;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.session.IStreamSession;

    public class Client {
        static final String PREFIX = "org.gasimo.";
        static String HOST = System.getProperty(PREFIX+"Host", "127.0.0.1");
        static final int PORT = Integer.getInteger(PREFIX+"Port", 8002);
        static final Integer BYE_TYPED = 0;
        static Gson gsn = new Gson();
        public static IStreamSession session;

        public static void main(String[] args) throws Exception {
            SelectorLoop loop = new SelectorLoop();

            if(args != null && args.length == 1 && args[0] != "")
            {
                // IP Regex patch (([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])

                if (args[0].matches("(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}"))
                {
                    HOST = System.getProperty(PREFIX+"Host", args[0]);
                } else {
                    System.out.println("Invalid IP Format for: " +  args[0]);
                }
            }


            try {
                loop.start();

                // Initialize the connection
                SocketChannel channel = SocketChannel.open();
                channel.configureBlocking(false);
                channel.connect(new InetSocketAddress(InetAddress.getByName(HOST), PORT));

                // Register the channel
                session = (IStreamSession) loop.register(channel, new ClientHandler()).sync().getSession();

                // Confirm that the connection was successful
                session.getReadyFuture().sync();

                // Read commands from the standard input
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                String line;
                while ((line = in.readLine()) != null) {
                    if (session.isOpen()) {
                        Command x = new Command();
                        x.rawCommand = line;
                        x.identifier = session.getName();
                        session.write((gsn.toJson(new Command[]{x})).getBytes());
                    }
                    if ("bye".equalsIgnoreCase(line)) {
                        session.getAttributes().put(BYE_TYPED, BYE_TYPED);
                        break;
                    }
                }
            }
            finally {

                // Gently stop the loop
                loop.stop();
            }
        }



}
