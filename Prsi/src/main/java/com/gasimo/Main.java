package com.gasimo;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

/**
 * Server initialization
 */
public class Main {

    /**
     * GameManager Object which controls main game loop
     */
    public static GameManager GM;

    /**
     * CommandInterpreter Object which controls Command calls, Interpretations and execution
     */
    public static CommandInterpreter CI = new CommandInterpreter();

    /**
     * NetworkingInterpreter Object Initializes necessary networking back-end
     */
    public static  NetworkingInterpreter NI;

    /**
     * ServerHandler handles Incoming Network requests from packets into string which is sent to CommandInterpreter
     */
    public static ServerHandler SH = new ServerHandler();

    /**
     * Contains Server Properties which are loaded at runtime.
     */
    public static ServerProperties SP;

    /**
     * Whether we enable colored console output
     */
    public static boolean enableConsoleColors = true;

    /**
     * The starting point of the server. Handles creation and initialization of all necessary objects.
     * @param args Server start arguments
     */
    public static void main(String[] args) {

        // In case we do not support colors in console (like cmd)
        if(args.length >= 1 && args[0].compareTo("-noColor") == 0)
        {
            System.out.println("Colored output has been disabled.");
            enableConsoleColors = false;
        }

        // Init
        try
        {
            // Load Server Properties
            SP = loadSP();

            // Create and init game object
            GM = new GameManager();
            GM.init();
            NI = new NetworkingInterpreter();

            // Autostart game in another thread based on serverProperty fields
            if(SP.autoStartNewGame)
            {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                            newGame();
                        }
                }).start();
            }

            // Initialize listening to console (separate thread)
            CI.listenToConsole();
            NI.init();
        }
        catch (Exception e)
        {
            try {
                throw(e);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            } catch (ExecutionException executionException) {
                executionException.printStackTrace();
            }
        }
    }

    /**
     * Creates a new game session on GameManager
     * @return Result message
     */
    public static String newGame(){
        // Replace with new / load later
        GM.newGame();
        return "Success - new game created";
    }

    /**
     * Loads Server Properties from file
     * @return Loaded ServerProperties
     */
    public static ServerProperties loadSP(){

        ServerProperties tempSP = new ServerProperties();
        String data = "";

        try {
            File myObj = new File(System.getProperty("user.dir") + "/server.properties");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                data = myReader.nextLine();
            }
            myReader.close();

            Gson gson = new Gson();

            tempSP = gson.fromJson(data, ServerProperties.class);

            return tempSP;
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred while loading serverProperties file. A blank file will be generated.");
            saveProperties(tempSP);
            tempSP = new ServerProperties();


            return tempSP;
        }
    }

    /**
     * Saves Server Properties into file
     * @param saveSP ServerProperties object to be saved
     */
    public static void saveProperties(ServerProperties saveSP) {
        try {
            FileWriter myWriter = new FileWriter(System.getProperty("user.dir") + "/server.properties");
            Gson gson = new Gson();
            myWriter.write(gson.toJson(saveSP));
            myWriter.close();
            System.out.println("Saved ServerProperties.");
        } catch (IOException e) {
            System.out.println("An error occurred while saving ServerProperties.");
            e.printStackTrace();
        }
    }

}
