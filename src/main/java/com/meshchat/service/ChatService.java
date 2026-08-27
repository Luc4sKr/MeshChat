package com.meshchat.service;

import com.meshchat.console.ConsoleCommand;
import com.meshchat.console.ConsoleCommandHandler;
import com.meshchat.peer.PeerConnection;
import com.meshchat.peer.PeerRegistry;
import com.meshchat.protocol.Message;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class ChatService implements ConsoleCommandHandler {

    private final String nickname;
    private final PeerRegistry peerRegistry;
    private final Runnable quitHandler;
    private final Consumer<String> seenChatMessageTracker;

    public ChatService(
            String nickname,
            PeerRegistry peerRegistry,
            Runnable quitHandler,
            Consumer<String> seenChatMessageTracker
    ) {
        this.nickname = Objects.requireNonNull(nickname);
        this.peerRegistry = Objects.requireNonNull(peerRegistry);
        this.quitHandler = Objects.requireNonNull(quitHandler);
        this.seenChatMessageTracker = Objects.requireNonNull(seenChatMessageTracker);
    }

    @Override
    public void handle(ConsoleCommand command) {
        switch (command) {
            case ConsoleCommand.Broadcast broadcast ->
                    broadcast(broadcast.text());

            case ConsoleCommand.PrivateMsg privateMessage ->
                    sendPrivate(
                            privateMessage.recipient(),
                            privateMessage.text()
                    );

            case ConsoleCommand.ListPeers ignored ->
                    listPeers();

            case ConsoleCommand.Quit ignored ->
                    quit();

            case ConsoleCommand.Invalid invalid ->
                    console(invalid.reason());

            case ConsoleCommand.Empty ignored -> {
                // Nothing to do.
            }
        }
    }

    private void broadcast(String text) {
        Message.ChatMessage message = new Message.ChatMessage(
                nickname,
                text
        );
        seenChatMessageTracker.accept(message.messageId());

        for (PeerConnection connection : peerRegistry.all()) {
            connection.send(message);
        }
    }

    private void sendPrivate(
            String recipient,
            String text
    ) {
        Message.PrivateMessage message = new Message.PrivateMessage(
                nickname,
                recipient,
                text,
                java.util.List.of(nickname)
        );

        var directConnection = peerRegistry.find(recipient);
        if (directConnection.isPresent()) {
            directConnection.get().send(message);
            return;
        }

        if (peerRegistry.all().isEmpty()) {
            console("Unknown participant: " + recipient);
            return;
        }

        for (PeerConnection connection : peerRegistry.all()) {
            connection.send(message);
        }
    }

    private void listPeers() {
        printParticipants();

        var discoveredNodes = new LinkedHashSet<String>(peerRegistry.nicknames());
        discoveredNodes.add(nickname);

        Message listRequest = new Message.ListRequestMessage(
                nickname,
                UUID.randomUUID().toString(),
                discoveredNodes
        );

        for (PeerConnection connection : peerRegistry.all()) {
            connection.send(listRequest);
        }
    }

    private void printParticipants() {
        var nicknames = peerRegistry.nicknames();

        if (nicknames.isEmpty()) {
            console("No other participants known.");
            return;
        }

        console(
                "Known participants: "
                        + String.join(", ", nicknames)
        );
    }

    private void quit() {
        console("Leaving the chat...");

        Message message =
                new Message.LeaveMessage(nickname);

        for (PeerConnection connection : peerRegistry.all()) {
            connection.send(message);
        }

        quitHandler.run();
    }

    private void console(String message) {
        System.out.println(message);
    }
}