package com.meshchat.console;

@FunctionalInterface
public interface ConsoleCommandHandler {

    void handle(ConsoleCommand command);
}