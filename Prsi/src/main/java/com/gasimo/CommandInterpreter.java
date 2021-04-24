package com.gasimo;

import java.util.ArrayList;
import java.util.Scanner;

import com.google.gson.Gson;

import java.rmi.*;

public class CommandInterpreter {

    private boolean listenConsole = false;
    private Gson gson = new Gson();


    public String parseExternalCommand(String jsonString, Long callerID) {
        Command cmd;
        try {
            cmd = gson.fromJson(jsonString, Command.class);
        } catch (Exception e) {
            return "echo Request could not be parsed." + e.getCause();
        }

        String reply = parseCommand(cmd);

        if (reply == "" || reply == null) {
            // sendMessage(new Command("RECEIVED"));
        }

        Command x = new Command();

        x.rawCommand = reply;

        // Fail occurred during request
        if (reply.split(" ")[0].compareTo("Fail") == 0) {
            x.result = CommandResult.Failed;
        }

        // Exception occurred during request
        if (reply.split(" ")[0].compareTo("Exception") == 0) {
            x.result = CommandResult.Exception;
        }

        // Request was ignored
        if (reply.split(" ")[0].compareTo("Ignored") == 0) {
            x.result = CommandResult.Ignored;
        }

        x.identifier = "Server";

        return gson.toJson(x);
    }

    /**
     * Interpret commands into main game loop
     *
     * @param command string to interpret
     */
    public String parseCommand(Command command) {
        ArrayList<String> tempCmd = new ArrayList<>();
        for (String s : command.rawCommand.split(" ")) {
            tempCmd.add(s.replace(" ", ""));
        }

        // Find which command is in question
        switch (tempCmd.get(0).replace(" ", "")) {
            // - - - - - - - - - - - - newGame
            case "newGame": {
                if ((tempCmd.size() - 1) == 0) {
                    try {
                        Main.newGame();
                        return "echo Success - New game created";
                    } catch (Exception e) {

                        return e.toString();
                    }

                } else {
                    return "echo Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + 2 + " expected.";
                }
            }
            // - - - - - - - - - - - - addPlayer playerJSON
            case "addPlayer": {
                if ((tempCmd.size() - 1) >= 1) {
                    if (Main.gm != null) {
                        try {
                            String json;
                            int i;
                            tempCmd.remove(0);

                            json = command.rawCommand.replace("addPlayer ", "");

                            Main.gm.addPlayer(gson.fromJson(json, Player.class));

                            return "Success - Player \"" + tempCmd.get(1) + "\" added.";

                        } catch (Exception e) {
                            throw (e);
                        }
                    } else {
                        return "Fail - game not created yet. Create a game first before adding players.";
                    }

                } else {
                    return "Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + "At least 2 " + " expected.";
                }
            }
            // - - - - - - - - - - - - getPlayerData name secret
            case "getPlayer": {
                if (Main.gm != null) {
                    if ((tempCmd.size() - 1) == 2) {
                        try {
                            for (Player x : Main.gm.players) {
                                if (x.playerName.compareTo(tempCmd.get(1)) == 0) {
                                    if (x.getPlayerSecret().compareTo(tempCmd.get(2)) == 0)
                                        return "Success PlayerObject " + gson.toJson(x);
                                    else
                                        return "Fail - " + "Incorrect player secret.";
                                }
                            }
                            return "Fail - No player found with name: " + tempCmd.get(1);
                        } catch (Exception e) {
                            throw (e);
                            //return e.toString();
                        }
                    } else {
                        return "Fail - game not created yet. Create a game first before adding players.";
                    }

                } else {
                    return "Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + 2 + " expected.";
                }
            }

            // - - - - - - - - - - - - Send message to client
            case "sendMessage": {
                if ((tempCmd.size() - 1) >= 1) {
                    try {
                        sendMessage(new Command(tempCmd.get(1)));
                        return "Success - Message: \"" + tempCmd.get(1) + "\" has been sent.";
                    } catch (Exception e) {
                        throw (e);
                        //return e.toString();
                    }

                } else {
                    return "Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + "at least 1" + " expected.";
                }
            }
            // Broadcasts message to all players
            case "broadcast": {
                if ((tempCmd.size() - 1) >= 1) {
                    try {
                        broadcastMessage(command.rawCommand.replace("broadcast ", ""), command.identifier);
                        return "Success - Message: " + command.rawCommand.replaceAll("broadcast ", "") + " has been sent.";
                    } catch (Exception e) {
                        e.printStackTrace();
                        return e.toString();
                    }

                } else {
                    return "Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + "at least 1" + " expected.";
                }
            }
            // Close connection with reason
            case "close": {
                if (command.identifier == "Local" || command.identifier.isBlank()) {
                    if ((tempCmd.size() - 1) >= 1) {
                        try {
                            broadcastMessage(command.rawCommand.replace("broadcast ", ""), "IDENTIFIER");
                            return "Success - Message: \"" + tempCmd.get(1) + "\" has been sent.";
                        } catch (Exception e) {
                            e.printStackTrace();
                            return e.toString();
                        }

                    } else {
                        return "Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + "at least 1" + " expected.";
                    }
                } else
                    return "You cannot execute close on local caller.";
            }
            // - - - - - - - - - - - - getPlayerData name secret
            case "echo": {
                if ((tempCmd.size() - 1) >= 1) {

                    try {
                        System.out.println(command.rawCommand.replace("echo ", ""));
                        return "";
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "Exception - " + e.toString();
                    }
                } else {
                    return "Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + 1 + " or higher expected.";
                }
            }

        }
        return "echo Unknown command: \"" + tempCmd.get(0) + "\" ";
    }

    /**
     * Sends messages
     *
     * @param cmd string to interpret
     * @return idk
     */
    public void sendMessage(Command cmd) {
        Main.SH.getSession().write(gson.toJson(cmd));
    }

    /**
     * Sends messages
     *
     * @param cmd string to interpret
     * @return idk
     */
    public void broadcastMessage(String cmd, String identifier) {
        Command x = new Command();
        x.rawCommand = "echo " + cmd;

        //x.identifier = identifier;

        Main.SH.broadcast(gson.toJson(x));
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
                    System.out.println(parseCommand(x).replaceAll("echo ", ""));
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