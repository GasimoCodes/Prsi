package com.gasimo;

import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class GameManager {

    // - - - Game data
    private GameStatus gameStatus;
    String sessionID = "";
    public ArrayList<Card> tableStack = new ArrayList<>();

    int currentPlayer;

    public Card top;
    public ArrayList<Card> placedStack;

    ArrayList<Player> players = new ArrayList();
    ArrayList<Player> gamePlayers = new ArrayList();

    // - - - Others
    boolean waitForPlayers =  true;
    public NetworkingInterpreter NI;
    Gson gson = new Gson();


    public void init(){

        // Get network session
        try {
            NI = Main.NI;
            gameStatus = GameStatus.noGame;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void newGame() {
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
        while(waitForPlayers)
        {
            // If all 5 players are joined (Can be overridden by forceStart command)
            if(players.size() == 5)
                waitForPlayers = false;

        }

        // Change stat that we are no longer waiting for players
        gameStatus = GameStatus.inProgress;
        Main.CI.broadcastMessage("Round starting....", "Server");

        // Give cards to players, thats 4 according to the rules.
        giveCards(4);

        // Send top deck card as top
        top = tableStack.get(0);
        // Set card to not be active (cant get active Eso right on beginning etc.)
        top.alreadyTriggered = true;
        tableStack.remove(0);

        // Set the game players to be the copy of all players. Thus we can keep players who already won in the game as spectators.
        gamePlayers = players;

        // Now begin main game loop
        mainGameLoop();

        // Something here?
    }

    void resetGame() {
        if (gameStatus != GameStatus.noGame)
            newGame();
        else
            System.out.println("No game found to reset.");
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    void forceStart() {
        switch (gameStatus)
        {
            case noGame:
                System.out.println("You cannot forceStart because no game was created. Create game with \"newGame\"");
                break;
            case inProgress:
                System.out.println("You cannot forceStart because an game session is already being played.");
                break;
            case awaitingPlayers:
                waitForPlayers = false;
                break;
            case ended:

        }
    }

    String addPlayer(Player p) {

        // Cannot add new player if an game is already running.
        if(gameStatus == GameStatus.inProgress) {
            return "echo Cannot add new player while an active game is running.";
        }

        // Cannot add new player if an game is already running.
        if(gameStatus == GameStatus.noGame) {
            return "echo Cannot add new player while no game was created. Please create a game with newGame command first.";
        }

        // Check if the player name isn't already used
        for(Player x : players)
        {
            if(p.playerName.compareTo(x.playerName) == 0 && p.getPlayerSecret().compareTo(x.getPlayerSecret()) == 0)
            {
                return "echo Player is already in list. This can happen if your connection was interrupted during waiting for players.";

            } else if (p.playerName.compareTo(x.playerName) == 0)
            {
                return "echo Player with same name already exists.";
            }
        }

        players.add(p);
        Main.CI.broadcastMessage(("Players joined: " + players.size()  + " out of 5."),"Server");
        return "echo Success - Player " + p.playerName + " has been added to game.";

    }

    void populateStack() {
        for (Card c : CardLogic.getAllCards()) {
            tableStack.add(c);
        }
    }

    void shuffleStack() {
        Collections.shuffle(tableStack);
    }

    void turnStacks() {

        int i = 0;
        for(Card c : placedStack)
        {
            tableStack.add(placedStack.get(i));
            placedStack.remove(c);
            i++;
        }
    }

    void giveCards(int count) {

        // For each player we
        for (Player p : gamePlayers)
        {
            // give count cards from deck
            for (int i = 0; i < count; i++)
            {
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

    void mainGameLoop()
    {
        // If tableStack is empty, we turn in the placed cards.
        if(tableStack.size() == 0)
        {
            turnStacks();
        }


        // Actions the player can choose from
        ArrayList<String> Actions = new ArrayList<>();


        // Check if we can take a card from the stack
        if(tableStack.size() != 0)
        {
            Actions.add("pick");
        }


        // Inform who is currently taking (expected to take) a turn
        Main.CI.broadcastMessage(("Player \"" + gamePlayers.get(currentPlayer) + "\" is on turn."), "Server");
        // check top card and status

        // If not triggered, top must be a special card!
        if(!top.alreadyTriggered)
        {
            // Handle special stuff here


        } else {
            // Handle basic cards
            ArrayList<Card> validCards = CardLogic.CheckLegalMoves(gamePlayers.get(currentPlayer).deck, top);

            for(Card c : validCards){

                Actions.add("place " + gson.toJson(c));

            }

        }
    }


}
