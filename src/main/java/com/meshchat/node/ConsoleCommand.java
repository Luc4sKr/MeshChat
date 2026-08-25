package com.meshchat.node;

public sealed interface ConsoleCommand {

    record Broadcast(String text) implements ConsoleCommand {
    }

    record PrivateMsg(String recipient, String text) implements ConsoleCommand {
    }

    record ListPeers() implements ConsoleCommand {
    }

    record Quit() implements ConsoleCommand {
    }

    record Invalid(String reason) implements ConsoleCommand {
    }

    record Empty() implements ConsoleCommand {
    }

    static ConsoleCommand parse(String line) {
        if (line == null || line.isBlank()) {
            return new Empty();
        }
        String trimmed = line.strip();

        if (trimmed.equals("/quit")) {
            return new Quit();
        }
        if (trimmed.equals("/list")) {
            return new ListPeers();
        }
        if (trimmed.startsWith("/msg ")) {
            String rest = trimmed.substring("/msg ".length()).stripLeading();
            int separator = rest.indexOf(' ');
            if (separator <= 0) {
                return new Invalid("Usage: /msg <nickname> <message>");
            }
            String recipient = rest.substring(0, separator);
            String text = rest.substring(separator + 1).stripLeading();
            if (text.isBlank()) {
                return new Invalid("Usage: /msg <nickname> <message>");
            }
            return new PrivateMsg(recipient, text);
        }
        if (trimmed.startsWith("/")) {
            return new Invalid("Unknown command: " + trimmed);
        }
        return new Broadcast(trimmed);
    }
}
