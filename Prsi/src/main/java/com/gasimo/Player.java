package com.gasimo;

import org.snf4j.core.session.IStreamSession;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Random;

/**
 * Defines Player object used by main game loop
 */
public class Player {

    private String playerSecret;
    String playerName;
    ArrayList<Card> deck = new ArrayList<>();
    public long netSession;

    public Player(String playerName, ArrayList<Card> deck, long netSession) {
        this.playerName = playerName;
        this.deck = deck;
        this.netSession = netSession;
        generateNewSecret();
    }

    public void generateNewSecret() {
        byte[] array = new byte[7];
        new Random().nextBytes(array);
        String generatedString = new String(array, Charset.forName("UTF-8"));
    }

    public String getPlayerSecret() {
        return playerSecret;
    }

    public void setPlayerSecret(String playerSecret) {
        this.playerSecret = playerSecret;
    }
}
