package com.gasimo;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import com.sun.jna.Native;

public class Main {

    public static GameManager gm;
    public static CommandInterpreter CI = new CommandInterpreter();
    public static  NetworkingInterpreter NI = new NetworkingInterpreter();
    public static ServerHandler SH = new ServerHandler();
    Kernel32 k = Native.load("kernel32", Kernel32.class);

    public static void main(String[] args) {
        // Let console execute commands
        try
        {
            gm = new GameManager();
            gm.init();
            CI.listenToConsole();
            System.out.println("\033[36mServer is online at 10.0.0.127\033[0m");
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
