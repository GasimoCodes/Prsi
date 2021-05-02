package com.gasimo;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Main {

    public static GameManager gm;
    public static CommandInterpreter CI = new CommandInterpreter();
    public static  NetworkingInterpreter NI = new NetworkingInterpreter();
    public static ServerHandler SH = new ServerHandler();

    public static void main(String[] args) {
        // Let console execute commands
        try
        {
            gm = new GameManager();
            gm.init();
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

        newGame();
    }

    public static String newGame(){
        // Replace with new / load later
        gm.newGame();
        return "Success - new game created";
    }

}
