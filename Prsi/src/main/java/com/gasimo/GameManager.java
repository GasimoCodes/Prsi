package com.gasimo;

import org.snf4j.core.session.IStreamSession;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class GameManager {

    private boolean gameRunning;
    String sessionID = "";
    public ArrayList<Card> tableStack = new ArrayList<>();
    ArrayList<Player> players = new ArrayList();
    public NetworkingInterpreter NI;

    public void init(){

        // Get network session
        try {
            NI = Main.NI;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void newGame() {
        sessionID = "0000";
        populateStack();
    }

    void resetGame() {
        if (gameRunning)
            newGame();
        else
            System.out.println("No game found to reset.");
    }

    String addPlayer(Player p) {
        players.add(p);
        return "Player added.";
    }

    void populateStack() {
        for (Card c : CardLogic.getAllCards()) {
            tableStack.add(c);
        }
    }


}
