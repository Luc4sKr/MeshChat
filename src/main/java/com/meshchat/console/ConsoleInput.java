package com.meshchat.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

public final class ConsoleInput {

    private final ConsoleCommandHandler commandHandler;

    public ConsoleInput(ConsoleCommandHandler commandHandler) {
        this.commandHandler = Objects.requireNonNull(commandHandler);
    }

    public void run() throws IOException {
        try (var reader = new BufferedReader(
                new InputStreamReader(System.in))) {

            String line;

            while ((line = reader.readLine()) != null) {
                ConsoleCommand command = ConsoleCommand.parse(line);
                commandHandler.handle(command);
            }
        }
    }
}