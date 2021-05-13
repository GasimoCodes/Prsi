package com.gasimo;

import com.google.gson.Gson;
import com.sun.jna.Native;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class Main {

    public static GameManager gm;
    public static CommandInterpreter CI = new CommandInterpreter();
    public static  NetworkingInterpreter NI = new NetworkingInterpreter();
    public static ServerHandler SH = new ServerHandler();
    public static ServerProperties SP;
    public static boolean enableConsoleColors = true;
    Kernel32 k = Native.load("kernel32", Kernel32.class);

    public static void main(String[] args) {

        // In case we do not support colors in console (like cmd)
        if(args.length >= 1 && args[1].compareTo("-noColor") == 0)
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
            gm = new GameManager();
            gm.init();

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
    public static String newGame(){
        // Replace with new / load later
        gm.newGame();
        return "Success - new game created";
    }


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
