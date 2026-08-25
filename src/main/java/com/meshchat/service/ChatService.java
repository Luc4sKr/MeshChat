package com.meshchat.service;

import com.meshchat.console.ConsoleCommandHandler;
import com.meshchat.node.ConsoleCommand;
import com.meshchat.peer.PeerConnection;
import com.meshchat.peer.PeerRegistry;
import com.meshchat.protocol.Message;

import java.util.Objects;

public final class ChatService implements ConsoleCommandHandler {

    private final String nickname;
    private final PeerRegistry peerRegistry;
    private final Runnable quitHandler;

    public ChatService(
            String nickname,
            PeerRegistry peerRegistry,
            Runnable quitHandler
    ) {
        this.nickname = Objects.requireNonNull(nickname);
        this.peerRegistry = Objects.requireNonNull(peerRegistry);
        this.quitHandler = Objects.requireNonNull(quitHandler);
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
                    printParticipants();

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
        Message message = new Message.ChatMessage(
                nickname,
                text
        );

        for (PeerConnection connection : peerRegistry.all()) {
            connection.send(message);
        }
    }

    private void sendPrivate(
            String recipient,
            String text
    ) {
        peerRegistry.find(recipient)
                .ifPresentOrElse(
                        connection -> connection.send(
                                new Message.PrivateMessage(
                                        nickname,
                                        recipient,
                                        text
                                )
                        ),
                        () -> console(
                                "Unknown participant: " + recipient
                        )
                );
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