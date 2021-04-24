package com.gasimo;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.IStreamSession;

public class ServerHandler extends AbstractStreamHandler {

    private static Integer USERID = 0;

    private static String YOUID = "[you]";

    static final Map<Long, IStreamSession> sessions = new HashMap<Long, IStreamSession>();

    Gson gson = new Gson();

    // Received from client
    @Override
    public void read(Object msg) {

        String s = new String((byte[])msg);

        // Log into console if required
        if(Main.NI.logClientRequests)
            System.out.println("["+getSession().getRemoteAddress()+"] -> [Server]: " + s);


        // Parse
        try {
        send(Main.CI.parseExternalCommand(s, getSession().getId()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Exit session
        if ("bye".equalsIgnoreCase(s)) {
            getSession().close();
        }
    }

    // Events
    @SuppressWarnings("incomplete-switch")
    @Override
    public void event(SessionEvent event) {
        switch (event) {
            case OPENED:
                sessions.put(getSession().getId(), getSession());
                // Set name of session (e.g. 127.0.0.1:78)
                getSession().getAttributes().put(USERID, "["+getSession().getRemoteAddress()+"]");
                sendCommand("echo Connected");
                break;

            case CLOSED:
                sessions.remove(getSession().getId());
                sendCommand("echo Disconnected");
                break;
        }
    }

    // Sent to client (RawText)
    public void sendCommand(String message) {

        long youId = getSession().getId();
        String userId = (String) getSession().getAttributes().get(USERID);

        if(Main.NI.logClientRequests)
            System.out.println("[Server]"+ " -> " + userId + ": "+ message.replaceAll("echo ", ""));

        // Takes all sessions

        for (IStreamSession session: sessions.values()) {
            if(session.getId() == youId)
            {
                Command x = new Command();
                x.rawCommand = (message);
                x.identifier = (session.getId() == youId ? "Server" : userId);

                System.err.println(gson.toJson(x));

                session.write(gson.toJson(x).getBytes());
            }
        }
    }


    // Sent to client (Commanded)
    public void send(String message) {
        Gson gson = new Gson();

        long youId = getSession().getId();
        String userId = (String) getSession().getAttributes().get(USERID);


        if(Main.NI.logClientRequests)
            System.out.println("[Server]"+ " -> " + userId + ": "+ message.replaceAll("echo ", ""));

        // Take all sessions
        for (IStreamSession session: sessions.values()) {
            if(session.getId() == youId)
            session.write(message.getBytes());
        }
    }

    // Sent to client (Commanded)
    public void broadcast(String message) {
        Gson gson = new Gson();

        if(Main.NI.logClientRequests)
            System.out.println("[Server]"+ " -> " + "[All]" + ": "+ message.replaceAll("echo ", ""));

        // Take all sessions
        for (IStreamSession session: sessions.values()) {

            session.write(message.getBytes());
        }
    }

    // Remove client
    public void close(String reason) {
        Gson gson = new Gson();

        long youId = getSession().getId();
        String userId = (String) getSession().getAttributes().get(USERID);

        if(Main.NI.logClientRequests)
            System.out.println("[Server]"+ " -> " + userId + ": "+ reason);

        // Take all sessions
        for (IStreamSession session: sessions.values()) {
            if(session.getId() == youId)
                session.write(reason.getBytes());
        }
    }

}