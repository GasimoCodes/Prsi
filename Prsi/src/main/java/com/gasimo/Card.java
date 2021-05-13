package com.gasimo;

/**
 * Class to contain all necessary card information
 */
public class Card {

    CardColor color = null;
    CardType type = null;
    boolean alreadyTriggered = false;

    public Card(CardColor color, CardType type, boolean alreadyTriggered) {
        this.color = color;
        this.type = type;
        this.alreadyTriggered = alreadyTriggered;

        // Force triggered on generic cards so they dont have to be checked later
        if(type == CardType.DESET || type == CardType.DEVET || type == CardType.OSM || type == CardType.KRAL || type == CardType.SPODEK)
        {
            this.alreadyTriggered = true;
        }
    }

    public Card(CardColor color, CardType type) {
        this.color = color;
        this.type = type;
        this.alreadyTriggered = true;
    }
}
