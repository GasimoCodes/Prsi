package com.gasimo;

public class Main {

    public static Player playerInfo;
    public static CommandInterpreter CI = new CommandInterpreter();
    public static NetworkingInterpreter NI = new NetworkingInterpreter();

    // R, V, D
    public static final String clientVersion = "0.0.1";

    public static void main(String[] args) throws Exception {

        // Set local player
        playerInfo = new Player("Name", null, 0);
        playerInfo.setPlayerSecret("1234");

        // Let console execute commands
        CI.listenToConsole();

    }

    public static void tryConnection(String s)
    {
        System.out.println("Attempting connection...");
        try {
            String[] sd = new String[]{s};
            Client.main(sd);
        } catch (Exception e)
        {
            System.out.println("Error occurred: Cannot reach server.");
            e.printStackTrace();
        }

    }
}
