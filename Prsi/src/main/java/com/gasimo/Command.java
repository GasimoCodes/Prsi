package com.gasimo;

/**
 * Contains necessary fields to define all local and remote command calls
 */
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

    public Command(String identifier, long caller, String rawCommand, String container, CommandResult result) {
        this.identifier = identifier;
        this.caller = caller;
        this.rawCommand = rawCommand;
        this.container = container;
        this.result = result;
    }
}
