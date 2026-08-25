package com.meshchat.node;

import com.meshchat.config.NodeConfig;
import com.meshchat.config.PeerAddress;
import com.meshchat.connection.ConnectionManager;
import com.meshchat.connection.PeerConnector;
import com.meshchat.console.ConsoleInput;
import com.meshchat.peer.PeerConnection;
import com.meshchat.peer.PeerConnectionListener;
import com.meshchat.peer.PeerRegistry;
import com.meshchat.protocol.Message;
import com.meshchat.service.ChatService;
import com.meshchat.server.PeerServer;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ChatNode implements PeerConnectionListener {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final int CONNECT_MAX_RETRIES = 5;
    private static final Duration CONNECT_RETRY_DELAY = Duration.ofSeconds(3);

    private final NodeConfig config;
    private final ConsoleInput consoleInput;
    private final ChatService chatService;
    private final PeerServer peerServer;
    private final PeerConnector peerConnector;
    private final ConnectionManager connectionManager;

    private final PeerRegistry registry = new PeerRegistry();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private volatile boolean running = true;

    public ChatNode(NodeConfig config) {
        this.config = config;

        this.chatService = new ChatService(
                config.nickname(),
                registry,
                this::shutdown
        );

        this.consoleInput = new ConsoleInput(chatService);

        this.connectionManager = new ConnectionManager(
                config.nickname(),
                config.port(),
                this
        );

        this.peerServer = new PeerServer(
                config.port(),
                connectionManager::accept,
                executor
        );

        this.peerConnector = new PeerConnector(
                CONNECT_TIMEOUT,
                CONNECT_MAX_RETRIES,
                CONNECT_RETRY_DELAY
        );
    }

    public void start() throws IOException {
        peerServer.start();

        console(
                "Listening on %s:%d as '%s'"
                        .formatted(
                                config.host(),
                                config.port(),
                                config.nickname()
                        )
        );

        PeerAddress self = config.selfAddress();

        for (PeerAddress peer : config.knownPeers()) {
            if (self.compareTo(peer) < 0) {
                executor.submit(() -> connectToPeer(peer));
            }
        }

        consoleInput.run();

        if (running) {
            shutdown();
        }
    }

    // --------------------------------------------------------------- connect

    private void connectToPeer(PeerAddress peer) {
        try {
            Socket socket = peerConnector.connect(peer);

            connectionManager.accept(socket);

        } catch (IOException e) {
            console(
                    "Failed to connect to %s: %s"
                            .formatted(
                                    peer,
                                    e.getMessage()
                            )
            );
        }
    }

    // --------------------------------------------------------- message events

    @Override
    public void onMessage(PeerConnection connection, Message message) {
        switch (message) {
            case Message.JoinMessage join -> {
                boolean isNew = !registry.isKnown(join.nickname());
                connection.setNickname(join.nickname());
                registry.register(join.nickname(), connection);
                if (isNew) {
                    console("* " + join.nickname() + " joined the chat");
                }
            }
            case Message.LeaveMessage leave -> {
                registry.remove(leave.nickname());
                console("* " + leave.nickname() + " left the chat");
                // Clear the nickname first so the close() below (triggered by
                // this voluntary LEAVE) doesn't also fire a redundant
                // "disconnected" announcement from onDisconnect().
                connection.setNickname(null);
                connection.close();
            }
            case Message.ChatMessage chat -> console(chat.sender() + ": " + chat.text());
            case Message.PrivateMessage pm -> console("(private) " + pm.sender() + ": " + pm.text());
            case Message.HeartbeatMessage ignored -> {
                // Keep-alive only; no application-level action needed.
            }
        }
    }

    @Override
    public void onDisconnect(PeerConnection connection) {
        String nickname = connection.nickname();
        if (nickname != null) {
            registry.remove(connection);
            console("* " + nickname + " disconnected");
        }
    }

    // ------------------------------------------------------------- lifecycle

    private void shutdown() {
        running = false;

        for (PeerConnection connection : registry.all()) {
            connection.close();
        }

        peerServer.close();

        executor.shutdownNow();
    }

    private void console(String text) {
        System.out.println(text);
    }
}