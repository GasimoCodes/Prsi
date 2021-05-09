package com.gasimo;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Scanner;

import com.google.gson.Gson;

import java.rmi.*;

public class CommandInterpreter {

    private boolean listenConsole = false;
    private Gson gson = new Gson();
    boolean showServerRawResponse = false;


    /**
     * Interpret Incoming Traffic
     *
     * @param jsonString string to interpret
     * @param callerID   the SessionID of the remote caller
     */
    public String parseExternalCommand(String jsonString, Long callerID) {
        ArrayList<Command> cmd = new ArrayList<>();
        ArrayList<Command> outCmd = new ArrayList<>();
        ArrayList<String> malFormedCommand = new ArrayList<>();

        try {
            // Dissect possibly malformed request

            int nestedCount = 0;
            String tempString = "";

            for (char c : jsonString.toCharArray()) {
                if (c == '[')
                    nestedCount++;

                if (c == ']')
                    nestedCount--;

                tempString += c;

                // End one string
                if (nestedCount == 0) {
                    malFormedCommand.add(tempString);
                    tempString = "";
                }
            }

            for (String strT : malFormedCommand) {

                for (Command x : gson.fromJson(strT, Command[].class)) {
                    x.caller = callerID;
                    cmd.add(x);
                }

                if (showServerRawResponse) {
                    System.out.println(jsonString);
                }

            }

            for (Command c : cmd) {
                String reply = parseCommand(c);

                if (reply == "" || reply == null) {
                    // Possibly add acknowledge response to bodyless responses.
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

                outCmd.add(x);

            }

        } catch (Exception e) {

            outCmd.add(new Command("Server", 0, "echo There was an exception parsing your request.", gson.toJson(e), CommandResult.Exception));
            System.out.println("Error while parsing occurred.");
            //e.printStackTrace();
        }

        return gson.toJson(outCmd.toArray());
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
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Main.newGame();
                            }
                        }).start();

                        return "echo Success - New game created";
                    } catch (Exception e) {

                        return e.toString();
                    }

                } else {
                    return "echo Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + 2 + " expected.";
                }
            }

            // - - - - - - - - - - - - gameStatus
            case "gameStatus": {
                if ((tempCmd.size() - 1) == 0) {
                    try {
                        if (Main.gm != null)
                            return "echo " + Main.gm.getGameStatus().toString();
                        else {
                            return "echo Game object not initialised.";
                        }

                    } catch (Exception e) {

                        return e.toString();
                    }

                } else {
                    return "echo Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + 0 + " expected.";
                }
            }

            // - - - - - - - - - - - - forceStart
            case "forceStart": {
                if ((tempCmd.size() - 1) == 0) {
                    try {
                        if (Main.gm != null) {
                            if (Main.gm.getGameStatus() == GameStatus.awaitingPlayers) {

                                if (Main.gm.players.size() >= 2) {
                                    Main.gm.forceStart();
                                    return "echo Forced game start for " + Main.gm.players.size() + " players.";
                                } else {
                                    return "echo Cannot force game when less than 2 players are connected.";
                                }


                            } else {
                                return "echo Game is not awaiting players and thus cannot be forceStarted.";
                            }
                        } else {
                            return "echo Game object not initialised.";
                        }

                    } catch (Exception e) {

                        return e.toString();
                    }

                } else {
                    return "echo Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + 0 + " expected.";
                }
            }


            // - - - - - - - - - - - - addPlayer name secret
            case "addPlayer": {
                if ((tempCmd.size() - 1) == 2) {
                    if (Main.gm != null) {
                        try {
                            Player p = new Player(tempCmd.get(1), null, command.caller);
                            p.setPlayerSecret(tempCmd.get(2));
                            return Main.gm.addPlayer(p);

                        } catch (Exception e) {
                            throw (e);
                        }
                    } else {
                        return "echo Fail - game not created yet. Create a game first before adding players.";
                    }

                } else {
                    return "echo Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + "At least 2 " + " expected.";
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
                                        return "echo Success PlayerObject " + gson.toJson(x);
                                    else
                                        return "echo Fail - " + "Incorrect player secret.";
                                }
                            }
                            return "echo Fail - No player found with name: " + tempCmd.get(1);
                        } catch (Exception e) {
                            throw (e);
                            //return e.toString();
                        }
                    } else {
                        return "echo Fail - game not created yet. Create a game first before adding players.";
                    }

                } else {
                    return "echo Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + 2 + " expected.";
                }
            }

            /*
            // - - - - - - - - - - - - Send message to a client
            case "sendMessage": {
                if ((tempCmd.size() - 1) >= 1) {
                    try {
                        sendMessage(new Command(tempCmd.get(1)));
                        return "echo Success - Message: \"" + tempCmd.get(1) + "\" has been sent.";
                    } catch (Exception e) {
                        throw (e);
                        //return e.toString();
                    }

                } else {
                    return "echo Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + "at least 1" + " expected.";
                }
            }
            */
            // - - - - - - - - - - - - Send message to client
            case "cs": {
                if ((tempCmd.size() - 1) >= 0) {
                    try {
                        return "echo CS CLIENT.";
                    } catch (Exception e) {
                        throw (e);
                    }

                } else {
                    return "echo Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + "at least 1" + " expected.";
                }
            }
            // Broadcasts message to all players
            case "broadcast": {
                if ((tempCmd.size() - 1) >= 1) {
                    try {
                        broadcastMessage(command.rawCommand.replace("broadcast ", ""), command.identifier);
                        return "echo Success - Message: " + command.rawCommand.replaceAll("broadcast ", "") + " has been sent.";
                    } catch (Exception e) {
                        e.printStackTrace();
                        return e.toString();
                    }

                } else {
                    return "echo Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + "at least 1" + " expected.";
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
                        return "echo Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + "at least 1" + " expected.";
                    }
                } else
                    return "You cannot execute close on local caller.";
            }
            // - - - - - - - - - - - - echo
            case "echo": {
                if ((tempCmd.size() - 1) >= 1) {

                    try {
                        System.out.println(command.rawCommand.replace("echo ", ""));
                        return "echo " + command.rawCommand.replace("echo ", "");
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "echo Exception - " + e.toString();
                    }
                } else {
                    return "echo Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + 1 + " or higher expected.";
                }
            }


            // - - - - - - - - - - - - turn PlayerName PlayerSecret ChoosedTurn Args
            case "turn": {
                if ((tempCmd.size() - 1) >= 3) {
                    try {

                        // Call the GameManager that we have received a game turn request

                        // If its not out turn or the turn is out of bounds, we send the error back to player, otherwise game continues.

                        return "";

                    } catch (Exception e) {
                        e.printStackTrace();
                        return "Exception - " + e.toString();
                    }
                } else {
                    return "Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + 3 + " or higher expected.";
                }
            }


        }
        return "echo Unknown command: \"" + tempCmd.get(0) + "\" ";
    }

    /**
     * Sends command to a specific client
     *
     * @param cmd      rawCommand to interpret and send to desired client
     * @param Receiver ID of the receiver session who will receive the message.
     */
    public void sendCommand(String cmd, Long Receiver) {
        Command x = new Command();
        x.rawCommand = cmd;
        Main.SH.send(x, Receiver);
    }

    /**
     * Sends command to a specific client
     *
     * @param cmd      Command to send to desired client
     * @param Receiver ID of the receiver session who will receive the message.
     */
    public void sendCommand(Command cmd, Long Receiver) {
        if (cmd.identifier == null)
            cmd.identifier = "";

        Main.SH.send(cmd, Receiver);
    }


    /**
     * Nroadcasts message to all clients
     *
     * @param cmd string to interpret
     */
    public void broadcastMessage(String cmd, String identifier) {
        Command x = new Command();
        x.rawCommand = "echo " + cmd;
        Main.SH.broadcast(gson.toJson(new Command[]{x}));
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