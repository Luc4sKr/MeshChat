package com.meshchat.connection;

import com.meshchat.peer.PeerConnection;
import com.meshchat.peer.PeerConnectionListener;
import com.meshchat.protocol.Message;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

public final class ConnectionManager implements AutoCloseable {

    private final String nickname;
    private final int port;
    private final PeerConnectionListener listener;

    public ConnectionManager(
            String nickname,
            int port,
            PeerConnectionListener listener
    ) {
        this.nickname = Objects.requireNonNull(nickname);
        this.port = port;
        this.listener = Objects.requireNonNull(listener);
    }

    public void accept(Socket socket) {
        Objects.requireNonNull(socket);

        try {
            PeerConnection connection =
                    new PeerConnection(socket, listener);

            connection.start();

            sendJoinMessage(connection);

        } catch (IOException e) {
            closeQuietly(socket);

            System.err.println(
                    "Failed to initialize connection with %s: %s"
                            .formatted(
                                    socket.getRemoteSocketAddress(),
                                    e.getMessage()
                            )
            );
        }
    }

    private void sendJoinMessage(PeerConnection connection) {
        connection.send(
                new Message.JoinMessage(
                        nickname,
                        port
                )
        );
    }

    private void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Nothing useful to do during cleanup.
        }
    }

    @Override
    public void close() {
        // Connection ownership will be moved here in a later step.
    }
}