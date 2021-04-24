package com.gasimo;

import java.util.ArrayList;
import java.util.Scanner;

import com.google.gson.Gson;

import javax.sql.XAConnection;


public class CommandInterpreter {

    private boolean listenConsole = false;
    boolean showServerRawResponse = true;
    private Gson gson = new Gson();
    private Command cmd;

    /**
     * handle JSON Command requests
     *
     * @param jsonString json string to interpret
     */
    public void parseExternalCommand(String jsonString, Long callerID) {

        if (showServerRawResponse) {
            System.out.println(jsonString);
        }

        try {

            jsonString.replace("}", "} ");
            String[] malFormedCommand = jsonString.split(" ");

            for (String strT : malFormedCommand) {
                cmd = gson.fromJson(strT, Command.class);

                if (cmd.identifier == null || cmd.identifier.compareTo("") == 0)
                    cmd.identifier = "Unknown";

                if (showServerRawResponse) {
                    System.out.println(jsonString);
                    System.out.println("[" + cmd.identifier + "] " + cmd.rawCommand);
                }

                parseCommand(cmd);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Interpret commands into main game loop
     *
     * @param x string to interpret
     */
    public String parseCommand(Command x) {

        ArrayList<String> tempCmd = new ArrayList<>();
        for (String s : x.rawCommand.split(" ")) {
            tempCmd.add(s.replace(" ", ""));
        }

        // Find which command is in question
        switch (tempCmd.get(0).replace(" ", "")) {
            // - - - - - - - - - - - - getPlayerData name secret
            case "getPlayer": {
                if ((tempCmd.size() - 1) == 2) {
                    if (Main.playerInfo != null) {
                        try {
                            return "Success PlayerObject " + gson.toJson(Main.playerInfo);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        return "Fail - Player not initialized yet.";
                    }

                } else {
                    return "Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + 2 + " expected.";
                }
            }
            // - - - - - - - - - - - - getPlayerData name secret
            case "connect": {
                if ((tempCmd.size() - 1) == 1) {

                    try {
                        Main.tryConnection(tempCmd.get(1));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "Exception - " + e.toString();
                    }
                } else {
                    Main.tryConnection("");
                    return "127.0.0.1";
                }
            }
            // - - - - - - - - - - - - write message
            case "echo": {
                //if (x.identifier.compareTo("Local") == 0 || x.identifier.isBlank()) {
                if ((tempCmd.size() - 1) >= 1) {

                    try {
                        System.out.println(x.rawCommand.replace("echo ", ""));
                        return "";
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "Exception - " + e.toString();
                    }
                } else {
                    return "Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + 1 + " or higher expected.";
                }
                //}
            }
            // - - - - - - - - - - - - close reason
            case "close": {
                if ((tempCmd.size() - 1) >= 0) {

                    try {
                        //Client.main();
                        System.out.println("");
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "Exception - " + e.toString();
                    }
                } else {
                    Main.tryConnection("");
                    return "127.0.0.1";
                }
            }
        }
        return "Unknown command: \"" + tempCmd.get(0) + "\" ";
    }

    /**
     * Sends messages
     *
     * @param receiver string to interpret
     * @return idk
     */
    public void sendMessage(String message, String receiver) {
    }

    /**
     * Initiates listening to console input
     */
    public void listenToConsole() {
        listenConsole = true;
        Scanner sc = new Scanner(System.in);
        Command x = new Command();
        x.identifier = "Local";
        x.caller = 0;

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (listenConsole) {
                    x.rawCommand = sc.nextLine();
                    System.out.println(parseCommand(x));
                }
                sc.close();
            }
        }).start();

    }


    /**
     * Ceases listening to console input
     */
    public void stopListeningToConsole() {
        listenConsole = false;
    }

}