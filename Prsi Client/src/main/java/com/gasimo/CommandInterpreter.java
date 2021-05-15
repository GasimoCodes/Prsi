package com.gasimo;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Scanner;


public class CommandInterpreter {

    private boolean listenConsole = false;
    boolean showServerRawResponse = false;
    private Gson gson = new Gson();

    /**
     * handle JSON Command requests
     *
     * @param jsonString json string to interpret
     */
    public void parseExternalCommand(String jsonString, Long callerID) {

        ArrayList<Command> cmd = new ArrayList<>();

        if (showServerRawResponse) {
            System.out.println(jsonString);
        }

        try {

            // Dissect possibly malformed request

            int nestedCount = 0;
            String tempString = "";

            ArrayList<String> malFormedCommand = new ArrayList<>();

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
                    cmd.add(x);
                }

            }

            for (Command x : cmd) {
                parseCommand(x);
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
                            return "echo Success - PlayerObject " + gson.toJson(Main.playerInfo);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        return "echo Fail - Player not initialized yet.";
                    }

                } else {
                    return "echo Exception - Bad argument count. Received (" + (tempCmd.size() - 1) + "), " + 2 + " expected.";
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
            // - - - - - - - - - - - - close reason
            case "info": {
                if ((tempCmd.size() - 1) >= 1) {

                    try {

                        if(tempCmd.get(1).compareToIgnoreCase("yourcards") ==0)
                        {
                            Card[] cs = gson.fromJson(x.container, Card[].class);

                            System.out.println("You have these cards: ");
                            // Write all cards we have!
                            for(Card c : cs)
                            {
                                System.out.println(c.color + " " + c.type);
                            }

                        }

                        return "";

                    } catch (Exception e) {
                        e.printStackTrace();
                        return "Exception - " + e.toString();
                    }
                } else {
                 return "Exception - Not enough arguments.";
                }
            }
            // - - - - - - - - - - - - You are on turn.
            case "reqTurn": {
                if ((tempCmd.size() - 1) >= 0) {

                    try {

                        System.out.println("You are on turn.");
                        gson = new Gson();

                        String[] opt = gson.fromJson(x.container, String[].class);
                        boolean waitAnswer = true;
                        int i = 0;
                        for (String s : opt) {

                            // Decode
                            switch (TurnActions.valueOf(s.split(" ")[0])) {
                                case PICK:

                                    // Singular
                                    if (s.split(" ").length == 1)
                                        System.out.println("[" + i + "]" + s);
                                        // More than 1
                                    else
                                        System.out.println("[" + i + "]" + s + " cards.");
                                    break;

                                case SKIP:
                                        System.out.println("[" + i + "]" + s);
                                    break;
                                case PLACE:
                                    Card cardAction;
                                    cardAction = gson.fromJson(s.replace("PLACE ", ""), Card.class);
                                    if (cardAction.type != CardType.SVRSEK)
                                        System.out.println("[" + i + "]" + "Place card: " + cardAction.type + " " + cardAction.color);
                                    else
                                        System.out.println("[" + i + "]" + "Place card: " + cardAction.type + " " + cardAction.color + " and change color.");
                                    break;

                                // We should not receive this action from server. We dynamically send this back when we select Svrsek.
                                case CHANGE_COLOR:
                                    System.out.println("[" + i + "]" + s);
                                    break;

                            }

                            i++;
                        }

                        System.out.println("Use makeTurn command to select your action.");

                        return "Received";
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "Exception - " + e.toString();
                    }
                } else {
                    return "";
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