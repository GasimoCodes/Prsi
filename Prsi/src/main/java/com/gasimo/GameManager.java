package com.gasimo;

import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class GameManager {

    // - - - Game data
    private GameStatus gameStatus;
    String sessionID = "";
    public ArrayList<Card> tableStack = new ArrayList<>();



    int currentPlayer;

    // Number of cards which were called by sedmicka.
    int passingCards;

    public Card top;
    public ArrayList<Card> placedStack;

    ArrayList<Player> players = new ArrayList();
    ArrayList<Player> gamePlayers = new ArrayList();

    // - - - Others
    boolean waitForPlayers;
    public NetworkingInterpreter NI;
    Gson gson = new Gson();
    ArrayList<String> actionsSaved;

    // If we wait the turn until we receive specific player response.
    Boolean listenPlayerWait = false;

    public void init() {

        // Get network session
        try {
            NI = Main.NI;
            gameStatus = GameStatus.noGame;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Creates and sets up new game
     */
    public void newGame() {
        sessionID = "0000";

        // Populate cards
        populateStack();

        // Shuffle them too
        shuffleStack();

        // Wait for enough players
        waitForPlayers = true;
        System.out.println("Waiting for players...");
        Main.CI.broadcastMessage("Waiting for players...", "Server");
        gameStatus = GameStatus.awaitingPlayers;

        while (waitForPlayers) {
            // If all 5 players are joined (Can be overridden by forceStart command)

            if (players.size() == 5) {
                waitForPlayers = false;
            }

            // For some reason, the thread does not update (receive the news that waitForPlayers is no longer true) unless we do this terribleness (Alternative was Print but thats just plain awful and non-performant. This at least saves some CPU ms time).
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // System.out.println("Funny loop go brr " + waitForPlayers);

        }

        // Change stat that we are no longer waiting for players
        gameStatus = GameStatus.inProgress;
        Main.CI.broadcastMessage("Round starting....", "Server");

        // Set the game players to be the copy of all players. Thus we can keep players who already won in the game as spectators.
        gamePlayers = players;

        // Give cards to players, that's 4 according to the rules.
        giveCards(4);

        // Send top deck card as top
        top = tableStack.get(0);
        // Set card to not be active (cant get active Eso right on beginning etc.)
        top.alreadyTriggered = true;
        tableStack.remove(0);

        // Now begin main game loop
        mainGameLoop();

        // Something here?
    }

    /**
     * Resets ongoing game
     */
    public void resetGame() {
        if (gameStatus != GameStatus.noGame)
            newGame();
        else
            System.out.println("No game found to reset.");
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    /**
     * Skips waiting for players as long as at least 2 players are connected
     */
    public void forceStart() {
        switch (gameStatus) {
            case noGame:
                System.out.println("You cannot forceStart because no game was created. Create game with \"newGame\"");
                break;
            case inProgress:
                System.out.println("You cannot forceStart because an game session is already being played.");
                break;
            case awaitingPlayers:
                waitForPlayers = false;
                System.out.println("Game start has been forced. waitForPlayers = " + waitForPlayers);
                break;
            case ended:

        }
    }

    /**
     * Adds player to game session
     *
     * @param p Player to add to session
     */
    public String addPlayer(Player p) {

        // Cannot add new player if an game is already running.
        if (gameStatus == GameStatus.noGame) {
            return "echo Cannot add new player while no game was created. Please create a game with newGame command first.";
        }

        // Check if the player name isn't already used
        for (Player x : players) {
            if (p.playerName.compareTo(x.playerName) == 0 && p.getPlayerSecret().compareTo(x.getPlayerSecret()) == 0) {
                // Subscribe netSession
                x.netSession = p.netSession;

                return "echo Player already exists, we reassigned its net session. You now play as " + x.playerName + ".";

            } else if (p.playerName.compareTo(x.playerName) == 0) {
                return "echo Player with same name already exists. Please provide last-used player password for " + x.playerName + " so we can assign the player subscription to you.";
            }
        }

        // Cannot add new player if an game is already running.
        if (gameStatus == GameStatus.inProgress) {
            return "echo Cannot add new player while an active game is running.";
        }

        players.add(p);
        Main.CI.broadcastMessage(("Players joined: " + players.size() + " out of 5."), "Server");

        // Cannot add new player if an game is already running.
        if (p.netSession == 0) {
            return "echo Warning - Player NetSessionID is invalid, the player will not receive player-specific calls. Perhaps the player was created by server?" + " Player \" + p.playerName + \" has been added to game.\"";
        }

        return "echo Success - Player " + p.playerName + " has been added to game.";

    }

    /**
     * Create and assign all cards in game to deck.
     */
    void populateStack() {
        for (Card c : CardLogic.getAllCards()) {
            tableStack.add(c);
        }
    }

    /**
     * Shuffle current deck
     */
    void shuffleStack() {
        Collections.shuffle(tableStack);
    }

    /**
     * Borrow cards from placed deck and turn them in.
     */
    void turnStacks() {

        int i = 0;
        for (Card c : placedStack) {
            tableStack.add(placedStack.get(i));
            placedStack.remove(c);
            i++;
        }
    }

    /**
     * Give away cards to players from tableDeck
     *
     * @param count The amount of cards each player will receive
     */
    public void giveCards(int count) {

        // For each player we
        for (Player p : gamePlayers) {

            // give count cards from deck
            for (int i = 0; i < count; i++) {
                p.deck.add(tableStack.get(0));
                tableStack.remove(0);
            }
        }


    }


    /*
     *   Valid actions:
     *
     *   place (card)
     *   pick
     *   change (color)
     *
     * */

    /**
     * Contains main game loop. This method is handled in newGame() method
     */
    private void mainGameLoop() {


        // If tableStack is empty, we turn in the placed cards.
        if (tableStack.size() == 0) {
            turnStacks();
        }

        // Actions the player can choose from
        ArrayList<String> actions = new ArrayList<>();


        // Check if we can take a card from the stack
        if (tableStack.size() != 0) {
            actions.add("pick");
        }

        // - - - - - - - - - SEND TIME

        // Inform about the card which is on top.
        Main.CI.broadcastMessage(("topCard " + top.type + ", " + top.color), "Server");

        // Inform who is currently taking (expected to take) a turn
        Main.CI.broadcastMessage(("turn \"" + gamePlayers.get(currentPlayer).playerName + "\" is on turn."), "Server");

        //Inform about other player stats

        String playerStats = "";

        //Todo Replace with more json-ish decode approach here
        for(Player p : gamePlayers)
        {
            playerStats += "Player " + p.playerName + " currently has " + p.deck.size() + " cards." + "\n";
        }

        Main.CI.broadcastMessage(playerStats, "Server");

        // Inform about which cards you have
        Command cmde = new Command();

        cmde.rawCommand = "echo You have these cards: " + gson.toJson(gamePlayers.get(currentPlayer).deck);
        cmde.container = gson.toJson(actions);

        Main.CI.sendCommand(cmde, gamePlayers.get(currentPlayer).netSession);

        // check top card and status

        // Handle available cards
        ArrayList<Card> validCards = CardLogic.CheckLegalMoves(gamePlayers.get(currentPlayer).deck, top);

        // Add cards to plausible selection and register svrsek actions
        for (Card c : validCards) {

            if(c.type != CardType.SVRSEK)
            {
                actions.add("place " + gson.toJson(c));
            }
            else
            {
                // Possibly add color register override for this here?
                actions.add(("place " + gson.toJson(c)));
            }
        }

        // If not triggered, top must be a special card!
        if (!top.alreadyTriggered) {
            // Handle special stuff here, we must anticipate color change request.


        } else {
            // Reserved
        }


        // - - - - - - - - - RESPONSE TIME 1

        String action = listenToAction(actions, gamePlayers.get(currentPlayer));


        // - - - - - - - - - RESPONSE TIME 2 IN CASE WE WANT TO APPEND (Change colors, etc)


        // - - - - - - - - - CHECK IF OUR ACTIONS HAVE NOT RESULTED IN GAME-END SCENARIO HERE?

    }

    /**
     * Wait for specific remote player input before game continues
     *
     * @param actions Actions the player can choose from
     * @param player  Targeted player
     */
    private String listenToAction(ArrayList<String> actions, Player player) {
        // Make it known that right now, we are just waiting for the player to finish turn.
        listenPlayerWait = true;
        actionsSaved = actions;
        Command cmd = new Command();

        cmd.rawCommand = "reqTurn";
        cmd.container = gson.toJson(actions);

        Main.CI.sendCommand(cmd, player.netSession);
        // Initiate waiting period.
        while (listenPlayerWait) {
            // Stuff while we await
        }

        return "";
    }

    // Player, Secret, Action.


    /**
     * Interpret player input into main game loop and stop listenToAction() loop
     *
     * @param x Command the player wishes to do
     */
    public String fireAction(Command x) {
        String[] cmd = x.rawCommand.split(" ");

        /* CHANGED TO SessionID Check
        // Check player name
        if (gamePlayers.get(currentPlayer).playerName.compareTo(cmd[1]) != 0) {
            return "echo This player is not on the turn. The player on turn is: " + gamePlayers.get(currentPlayer).playerName;
        }

        // Validate player secret
        if (gamePlayers.get(currentPlayer).playerName.compareTo(cmd[2]) != 0) {
            return "echo Incorrect player secret received.";
        }
        */
        // Validate valid selection

        // Handle stuff

        listenPlayerWait = false;

        // Respond back with success or error
        return "Result.";
    }


}
