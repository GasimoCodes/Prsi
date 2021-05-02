package com.gasimo;

import org.snf4j.core.session.IStreamSession;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class GameManager {

    private GameStatus gameStatus;
    String sessionID = "";
    public ArrayList<Card> tableStack = new ArrayList<>();
    ArrayList<Player> players = new ArrayList();
    public NetworkingInterpreter NI;


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
        boolean waitForPlayers = true;
        // Wait for enough players

        System.out.println("Waiting for players.");

        while(waitForPlayers)
        {
            // If all 5 players are joined (Can be overridden by forceStart command)
            if(players.size() == 5)
                waitForPlayers = false;
        }

        gameStatus = GameStatus.inProgress;



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

                break;
            case ended:

        }
    }

    String addPlayer(Player p) {

        // Cannot add player if an game is already running.
        if(gameStatus == GameStatus.inProgress) {
            return "echo Cannot add player while an active game is running.";
        }

        // Check if the player name isnt already used
        for(Player x : players)
        {
            if(p.playerName.compareTo(x.playerName) == 0 && p.getPlayerSecret().compareTo(x.getPlayerSecret()) == 0)
            {
                return "echo Player is already in list. This can happen if your connection was interrupted during waiting for palyers.";

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


}
