package com.meshchat.console;

import com.meshchat.node.ConsoleCommand;

@FunctionalInterface
public interface ConsoleCommandHandler {

    void handle(ConsoleCommand command);
}