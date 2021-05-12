package com.gasimo;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class CardLogic {
    // Implement rules here


    /**
     * Return arraylist of all legal moves for a given deck based on the current card.
     *
     * @param deck players card deck
     * @param top  current card on top of stack player has to respond on
     * @return arrayList of legal cards player cna use in given situation
     */
    public static ArrayList<Card> CheckLegalMoves(ArrayList<Card> deck, Card top) {

        // This contains all the possible moves we can make given the situation
        ArrayList<Card> legals = new ArrayList<>();

        // Current card can always be replaced by its own type regardless of color, here we gather all colors of our type.
        for (Card c : getAllColors(top.type)) {
            legals.add(c);
        }

        // Check if the top card is of special type and has been triggered
        if (top.alreadyTriggered) {
            // Add all same color. Because no special effects are present, we can use all cards available.
            for (Card c : getAllTypes(top.color)) {
                legals.add(c);
            }

        } else {
            try {
                switch (top.type) {

                    // Specials
                    case SEDM:
                        break;
                    case ESO:
                        break;
                    case SVRSEK:
                        throw new InvalidCardException("Svrsek is not valid for not yet trigerred state", top);
                    default:
                        throw new InvalidCardException("", top);
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        // Filter out unused from deck

        ArrayList<Card> filtered = new ArrayList<>();

        for(Card c : deck)
        {
            for (Card l : legals)
            {
                if(l.color == c.color && l.type == c.type)
                {
                    filtered.add(c);
                    break;
                }
            }
        }

        return filtered;
    }


    /**
     * Returns an array of all cards filtered by type.
     *
     * @param type the type of cards to be added into array
     * @return the array of cards with all the colors for given type
     */
    static Card[] getAllColors(CardType type) {
        // Generate array with space for all color types
        Card[] tempCards = new Card[CardColor.values().length];

        // Temp index of color we are assigning
        int i = 0;
        // Assign all card colors into tempCards
        for (Card c : tempCards) {
            tempCards[i] = new Card(CardColor.values()[i], type);
            i++;
        }

        return tempCards;
    }

    /**
     * Returns an array of all cards filtered by type.
     *
     * @param color the color of cards to be added into array
     * @return the array of cards with all the colors for given type
     */
    static Card[] getAllTypes(CardColor color) {

        // Generate array with space for all color types
        Card[] tempCards = new Card[CardType.values().length];

        // Temp index of color we are assigning
        int i = 0;
        // Assign all card types into tempCards
        for (Card c : tempCards) {
            tempCards[i] = new Card(color, CardType.values()[i]);
            i++;
        }
        return tempCards;
    }

    /**
     * Returns an array of all cards (32).
     *
     * @return the array of cards with all the colors for given type
     */
    static Card[] getAllCards() {

        // Generate array with space for all color types
        Card[] tempCards = new Card[CardType.values().length * CardColor.values().length];

        int i = 0;

        // Assign all card types into tempCards
        for (CardType x : CardType.values()) {
            for (Card c : getAllColors(x)) {
                tempCards[i] = c;
                i++;
            }
        }
        return tempCards;
    }
}

class InvalidCardException extends Exception {

    public InvalidCardException(String message, Card crd) {
        super(message + "\nRequested card: \"" + crd.toString() + "\" is invalid for given operation.");
    }
}
