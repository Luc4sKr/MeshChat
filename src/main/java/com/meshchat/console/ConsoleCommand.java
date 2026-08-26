package com.meshchat.console;

public sealed interface ConsoleCommand {

    String MSG_COMMAND = "/msg";
    String QUIT_COMMAND = "/quit";
    String LIST_COMMAND = "/list";

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

        String input = line.strip();

        if (!input.startsWith("/")) {
            return new Broadcast(input);
        }

        String[] parts = input.split("\\s+", 2);

        return switch (parts[0]) {
            case QUIT_COMMAND -> parseQuit(parts);
            case LIST_COMMAND -> parseList(parts);
            case MSG_COMMAND -> parsePrivateMessage(parts);
            default -> new Invalid("Unknown command: " + parts[0]);
        };
    }

    private static ConsoleCommand parseQuit(String[] parts) {
        if (parts.length > 1) {
            return new Invalid("Usage: /quit");
        }

        return new Quit();
    }

    private static ConsoleCommand parseList(String[] parts) {
        if (parts.length > 1) {
            return new Invalid("Usage: /list");
        }

        return new ListPeers();
    }

    private static ConsoleCommand parsePrivateMessage(String[] parts) {
        if (parts.length < 2 || parts[1].isBlank()) {
            return invalidPrivateMessageUsage();
        }

        String[] arguments = parts[1].strip().split("\\s+", 2);

        if (arguments.length < 2 || arguments[1].isBlank()) {
            return invalidPrivateMessageUsage();
        }

        String recipient = arguments[0];
        String message = arguments[1];

        return new PrivateMsg(recipient, message);
    }

    private static Invalid invalidPrivateMessageUsage() {
        return new Invalid("Usage: /msg <nickname> <message>");
    }
}