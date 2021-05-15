package com.gasimo;

import com.google.gson.Gson;

import java.lang.invoke.SwitchPoint;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Main Game Logic Class where all the game is running at.
 */
public class GameManager {

    // - - - Game data
    private GameStatus gameStatus;
    String sessionID = "";
    public ArrayList<Card> tableStack = new ArrayList<>();


    int currentPlayer;

    // Number of cards which were called by sedmicka.
    int passingCards;

    public Card top;
    public ArrayList<Card> placedStack = new ArrayList<>();

    ArrayList<Player> players = new ArrayList();
    ArrayList<Player> gamePlayers = new ArrayList();

    // - - - Others
    boolean waitForPlayers;
    public NetworkingInterpreter NI;
    Gson gson = new Gson();
    ArrayList<String> actionsSaved;

    // If we wait the turn until we receive specific player response.
    Boolean listenPlayerWait = false;
    private String chosenAction = "";
    private boolean concurrentAction = false;

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
            return "echo Warning - Player NetSessionID is invalid, the player will not receive player-specific calls. Perhaps the player was created by server?" + " Player \"" + p.playerName + "\" has been added to game.\"";
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

        concurrentAction = true;
        System.out.println("Stack will now be turned.");
        try {
            int i = 0;
            for (Card c : placedStack) {
                tableStack.add(placedStack.get(i));
                placedStack.remove(c);
                i++;
            }
            concurrentAction = false;
        } catch (Exception e )
        {
            //Todo !!IMPORTANT!!: Make a cleaner synchronization of this or check when we are / are not writing into this array and delay any actions before this is done!!
            System.err.println("ConcurrentModificationException has surfaced, the turning will be delayed to next frame.");
            turnStacks();
        }
        concurrentAction = false;

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

    /**
     * Make player take cards from deck
     *
     * @param p The player who will be taking these cards
     */
    public void pickCard(Player p, int number) {


        // In case we ran out of deck cards
        if (tableStack.size() < number) {
            turnStacks();
        }



        // give n cards from deck to selected player
        for (int i = 0; i < number; i++) {

            // In case concurrence exists we desync threads.
            while(concurrentAction)
            {
            }

            p.deck.add(tableStack.get(0));
            tableStack.remove(0);
        }

    }


    /**
     * Contains main game loop. This method is handled in newGame() method
     */
    private void mainGameLoop() {

        while (gameStatus != GameStatus.ended) {

            // If tableStack is empty or not big enough, we turn in the placed cards.
            if (tableStack.size() == 0 || tableStack.size() < passingCards) {
                // Catch up with possible concurrent threads
                while (concurrentAction){};
                turnStacks();

                // If we simply still do not have enough cards...
                if(passingCards > tableStack.size())
                {
                    System.out.println("There are not enough cards left to fulfill the passingCards.");
                    passingCards = tableStack.size();
                }

            }

            // Actions the player can choose from
            ArrayList<String> actions = new ArrayList<>();

            // In case we get sedmicka, we add cards to passingCards
            if (top.alreadyTriggered == false && top.type == CardType.SEDM) {
                passingCards += 2;
            }

            // Check whether we can even pick a card in a given scenario (ESO) so we can replace pick with skip
            if (top.alreadyTriggered == false && top.type == CardType.ESO) {

                // We add the skip turn option in case ESO was called upon us
                actions.add(TurnActions.SKIP.name());

            } else {

                // Check if we can take a card from the stack (Not enough cards)
                if (tableStack.size() != 0) {
                    actions.add(TurnActions.PICK.name() + ((passingCards != 0) ? (" " + passingCards) : ""));
                } else {

                }
            }

            // Inform who is currently taking (expected to take) a turn
            Main.CI.broadcastMessage(("turn \"" + gamePlayers.get(currentPlayer).playerName + "\" is on turn."), "Server");

            //Inform about other player stats

            String playerStats = "";

            //Todo Replace with more json-ish decode approach here
            for (Player p : gamePlayers) {
                playerStats += "Player " + p.playerName + " currently has " + p.deck.size() + " cards.";

                if (gamePlayers.get(gamePlayers.size() - 1) != p)
                    playerStats += "\n";

            }

            Main.CI.broadcastMessage(playerStats, "Server");

            //region inform section
            // Inform about the card which is on top.
            Main.CI.broadcastMessage(("topCard " + top.type + " " + top.color + " (Inactive: " + top.alreadyTriggered + ")"), "Server");


            // Inform about which cards you have
            Command cmde = new Command();
            cmde.rawCommand = "info yourCards";
            cmde.container = gson.toJson(gamePlayers.get(currentPlayer).deck);
            //cmde.container = gson.toJson(actions);
            Main.CI.sendCommand(cmde, gamePlayers.get(currentPlayer).netSession);

            //endregion inform section

            // Get legal card moves
            ArrayList<Card> validCards = CardLogic.CheckLegalMoves(gamePlayers.get(currentPlayer).deck, top);

            // Add cards to plausible selection
            for (Card c : validCards) {

                if (c.type != CardType.SVRSEK) {
                    actions.add(TurnActions.PLACE.name() + " " + gson.toJson(c));
                } else {
                    // Possibly add color register override for this here?
                    actions.add((TurnActions.PLACE.name() + " " + gson.toJson(c)));
                }
            }

            // In the rare case we ran out of cards and actions to do, we must skip for now to not break the game.
            if(actions.size() == 0)
            {
                actions.add(TurnActions.SKIP.name());
            }


            // - - - - - - - - - RESPONSE TIME 1

            // Wait for response from player


            String action = listenToAction(actions, gamePlayers.get(currentPlayer));


            // Good, now to decode it. We know first piece of the string is the action name.

            boolean changeColors = false;
            switch (TurnActions.valueOf(action.split(" ")[0])) {

                case PICK:

                    // No passing cards
                    if (passingCards == 0) {
                        pickCard(gamePlayers.get(currentPlayer), 1);
                    } else

                    // We are receiving n cards
                    {
                        pickCard(gamePlayers.get(currentPlayer), passingCards);
                        top.alreadyTriggered = true;
                        passingCards = 0;
                    }
                    break;
                case PLACE:
                    // Move current TOP to placedCards
                    placedStack.add(top);

                    // Read card we want to place
                    Card c = gson.fromJson(action.replace("PLACE ", ""), Card.class);

                    //Enable possible special action
                    if (c.type == CardType.SVRSEK || c.type == CardType.SEDM || c.type == CardType.ESO) {
                        c.alreadyTriggered = false;
                    }

                    if (c.type == CardType.SVRSEK)
                        changeColors = true;


                    // Place the card as top
                    top = c;


                    // Remove the card from player
                    gamePlayers.get(currentPlayer).deck.remove(c);

                    for (Card cx : gamePlayers.get(currentPlayer).deck) {
                        if (cx.type == c.type && cx.color == c.color) {
                            System.out.println("Success - Specified card " + c.type.name() + " " + c.color.name() + " has been removed from player deck.");
                            gamePlayers.get(currentPlayer).deck.remove(cx);
                            break;
                        }
                    }

                    // This was Cave Johnson, we are done here.
                    break;

                case CHANGE_COLOR:
                    // SHOULD NOT BE POSSIBLE IN FIRST CALL
                    break;

                case SKIP:

                    top.alreadyTriggered = true;

                    break;

            }


            // - - - - - - - - - RESPONSE TIME 2 IN CASE WE WANT TO APPEND (Change colors, etc)

            if (changeColors) {

                actions = new ArrayList<>();

                for (CardColor x : CardColor.values()) {
                    actions.add(TurnActions.CHANGE_COLOR.name() + " " + x.name());
                }


                // - - - - - - - - - RESPONSE TIME 2

                // Wait for response from player
                action = listenToAction(actions, gamePlayers.get(currentPlayer));
                top.color = CardColor.valueOf(action.split(" ")[1]);

                // Make sure the top is no longer in triggered state.
                top.alreadyTriggered = true;

                // Update clients visually on color change around here

            }

            // - - - - - - - - - CHECK IF OUR ACTIONS HAVE NOT RESULTED IN GAME-END SCENARIO HERE? (Do not forget we can still bring them back using Srdvova Sedmicka

            //Todo Make this thing a bit better-ish
            if (gamePlayers.get(currentPlayer).deck.size() == 0) {
                Main.CI.broadcastMessage("Player " + gamePlayers.get(currentPlayer).playerName + " managed to get rid of all their cards and thus won!" + ((gamePlayers.size() >= 2) ? " Now its between rest of the " + (gamePlayers.size() - 1) + " players" : ""), "Server");

                if (currentPlayer > 0) {
                    currentPlayer--;
                } else {
                    currentPlayer = gamePlayers.size() - 1;
                }

                gamePlayers.remove(gamePlayers.get(currentPlayer));
            }

            // If we have 1 player left
            if (gamePlayers.size() == 1) {
                gameStatus = GameStatus.ended;
                Main.CI.broadcastMessage("Game has ended", "Server");
            }

            // Select next player
            if (currentPlayer < (gamePlayers.size() - 1)) {
                currentPlayer++;

            } else {
                currentPlayer = 0;
            }

        }


        //Todo Write some winner board here and things.

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
        cmd.container = gson.toJson(actions.toArray());

        Main.CI.sendCommand(cmd, player.netSession);
        // Initiate waiting period.
        while (listenPlayerWait) {

            // For some reason, the thread does not update unless we do this terribleness.
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // we have the desired action in our grasp at this point


        return chosenAction;
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

        // Check whether we expect an turn action at all at this moment
        if (!listenPlayerWait)
            return "There is no turn to be currently made.";

        // Validate valid selection
        try {
            if (Integer.parseInt(x.rawCommand.split(" ")[1]) <= actionsSaved.size() && Integer.parseInt(x.rawCommand.split(" ")[1]) >= 0) {
                chosenAction = actionsSaved.get(Integer.parseInt(x.rawCommand.split(" ")[1]));
            }
        } catch (Exception e) {
            listenPlayerWait = true;
            return "An exception occurred, perhaps " + x.rawCommand.split(" ")[1] + " is not a number?";

        }


        // Handle stuff
        listenPlayerWait = false;

        // Respond back with success or error
        return "Result.";
    }


}
