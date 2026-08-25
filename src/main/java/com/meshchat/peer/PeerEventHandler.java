package com.meshchat.peer;

import com.meshchat.protocol.Message;

import java.util.Objects;

public final class PeerEventHandler implements PeerConnectionListener {

    private final PeerRegistry peerRegistry;

    public PeerEventHandler(PeerRegistry peerRegistry) {
        this.peerRegistry = Objects.requireNonNull(peerRegistry);
    }

    @Override
    public void onMessage(
            PeerConnection connection,
            Message message
    ) {
        switch (message) {
            case Message.JoinMessage join -> handleJoin(connection, join);

            case Message.LeaveMessage leave -> handleLeave(connection, leave);

            case Message.ChatMessage chat -> handleChat(chat);

            case Message.PrivateMessage privateMessage -> handlePrivateMessage(privateMessage);

            case Message.HeartbeatMessage ignored -> handleHeartbeat();
        }
    }

    @Override
    public void onDisconnect(PeerConnection connection) {
        String nickname = connection.nickname();

        if (nickname == null) {
            return;
        }

        peerRegistry.remove(connection);

        console("* " + nickname + " disconnected");
    }

    private void handleJoin(
            PeerConnection connection,
            Message.JoinMessage message
    ) {
        boolean isNew =
                !peerRegistry.isKnown(message.nickname());

        connection.setNickname(message.nickname());

        peerRegistry.register(
                message.nickname(),
                connection
        );

        if (isNew) {
            console(
                    "* " + message.nickname()
                            + " joined the chat"
            );
        }
    }

    private void handleLeave(
            PeerConnection connection,
            Message.LeaveMessage message
    ) {
        peerRegistry.remove(message.nickname());

        console(
                "* " + message.nickname()
                        + " left the chat"
        );

        connection.setNickname(null);

        connection.close();
    }

    private void handleChat(Message.ChatMessage message) {
        console(
                message.sender()
                        + ": "
                        + message.text()
        );
    }

    private void handlePrivateMessage(
            Message.PrivateMessage message
    ) {
        console(
                "(private) "
                        + message.sender()
                        + ": "
                        + message.text()
        );
    }

    private void handleHeartbeat() {
        // Keep-alive only.
    }

    private void console(String message) {
        System.out.println(message);
    }
}