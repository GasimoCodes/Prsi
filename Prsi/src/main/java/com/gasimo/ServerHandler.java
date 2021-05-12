package com.gasimo;

import java.util.HashMap;
import java.util.Map;
import com.diogonunes.jcolor.*;
import com.google.gson.Gson;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.IStreamSession;

import javax.swing.*;

import static com.diogonunes.jcolor.Ansi.colorize;
import static com.diogonunes.jcolor.Attribute.*;

public class ServerHandler extends AbstractStreamHandler {

    private static Integer USERID = 0;

    private static String YOUID = "[you]";

    static final Map<Long, IStreamSession> sessions = new HashMap<Long, IStreamSession>();

    Gson gson = new Gson();

    // Received from client
    @Override
    public void read(Object msg) {

        String s = new String((byte[]) msg);

        // Log into console if required
        //Todo Surround with try-catch to prevent possible exceptions with malformed packets or move to CommandInterpreter.js
        if (Main.NI.logClientRequests) {
            for (Command x : gson.fromJson(s, Command[].class)) {
                System.out.println(colorize("[" + getSession().getRemoteAddress() + "]", YELLOW_TEXT()) + colorize(" -> ", YELLOW_TEXT()) + colorize("[Server]", YELLOW_TEXT()) + colorize(": ", YELLOW_TEXT()) + x.rawCommand.replace("echo ", ""));
            }
        }

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
                getSession().getAttributes().put(USERID, "[" + getSession().getRemoteAddress() + "]");
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

        // Takes all sessions

        for (IStreamSession session : sessions.values()) {
            if (session.getId() == youId) {
                Command x = new Command();
                x.rawCommand = (message);
                x.identifier = (session.getId() == youId ? "Server" : userId);

                if (Main.NI.logClientRequests)
                    System.out.println(colorize("[Server]" + " -> " + userId + ": ", YELLOW_TEXT()) + x.rawCommand.replace("echo ", ""));


                session.write(gson.toJson(new Command[]{x}).getBytes());
            }
        }
    }


    // Sent to client (Commanded)
    public void send(String message) {
        Gson gson = new Gson();

        long youId = getSession().getId();
        String userId = (String) getSession().getAttributes().get(USERID);

        // Replace command with small version of just rawCommand for clarity.
        if (Main.NI.logClientRequests)


        //Todo Surround with try-catch to prevent possible exceptions with malformed packets or move to CommandInterpreter.js
        if (Main.NI.logClientRequests) {
            for (Command x : gson.fromJson(message, Command[].class)) {
                System.out.println(colorize("[Server]", YELLOW_TEXT()) + colorize(" -> ", YELLOW_TEXT()) + colorize(userId, YELLOW_TEXT()) + colorize(": ", YELLOW_TEXT()) + x.rawCommand.replace("echo ", ""));
            }
        }


        // Take all sessions
        for (IStreamSession session : sessions.values()) {
            if (session.getId() == youId)
                session.write(message.getBytes());
        }
    }

    // Sent to client (Commanded)
    public void broadcast(String message) {
        Gson gson = new Gson();

        //Todo Surround with try-catch to prevent possible exceptions with malformed packets or move to CommandInterpreter.js
        if (Main.NI.logClientRequests) {
            for (Command x : gson.fromJson(message, Command[].class)) {
                System.out.println(colorize("[Server]", YELLOW_TEXT()) + colorize(" -> ", YELLOW_TEXT()) + colorize("[All]", YELLOW_TEXT()) + colorize(": ", YELLOW_TEXT()) + x.rawCommand.replace("echo ", ""));
            }
        }


        // Take all sessions
        for (IStreamSession session : sessions.values()) {
            session.write(message.getBytes());
        }
    }


    // Sent to a specific client (Commanded)
    public void send(Command command, Long receiver) {
        Gson gson = new Gson();

        long youId = receiver;
        String userId = "";

        try {

        } catch (Exception e) {
            e.printStackTrace();
        }

        Command[] cArray = new Command[]{command};

        String message = gson.toJson(cArray);

        // Only selected sessions
        for (IStreamSession session : sessions.values()) {
            if (session.getId() == youId) {
                userId = (String) session.getAttributes().get(USERID);

                if (Main.NI.logClientRequests){

                    for(Command x : gson.fromJson(message, Command[].class))
                    {
                        System.out.println(colorize("[Server]" + " -> " + userId + ": ", YELLOW_TEXT()) + x.rawCommand);
                    }

                }
                session.write(message.getBytes());
            }
        }
    }


    // Remove client (Not yet implemented?)
    public void close(String reason) {
        Gson gson = new Gson();

        long youId = getSession().getId();
        String userId = (String) getSession().getAttributes().get(USERID);

        if (Main.NI.logClientRequests)
            System.out.println("[Server]" + " -> " + userId + ": " + reason);

        // Take all sessions
        for (IStreamSession session : sessions.values()) {
            if (session.getId() == youId)
                session.write(reason.getBytes());
        }
    }

}