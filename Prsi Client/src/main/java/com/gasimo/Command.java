package com.gasimo;

public class Command {

    /**
     * Human readable identifier
     */
    String identifier = "";

    /**
     * Defines the caller we reply to
     */
    long caller = 0;


    /**
     * Raw command in text form
     */
    String rawCommand = "";


    /**
     * Container for additional data, specified by rawCommand
     */
    String container = "";

    /**
     * Result of call
     */
    CommandResult result = CommandResult.Success;

    public Command(String rawCommand) {
        this.rawCommand = rawCommand;
    }

    public Command() {
    }

}
