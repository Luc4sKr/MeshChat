package com.meshchat.node;

import com.meshchat.config.NodeConfig;
import com.meshchat.config.PeerAddress;
import com.meshchat.console.ConsoleInput;
import com.meshchat.peer.PeerConnection;
import com.meshchat.peer.PeerConnectionListener;
import com.meshchat.peer.PeerRegistry;
import com.meshchat.protocol.Message;
import com.meshchat.service.ChatService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
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

    private final PeerRegistry registry = new PeerRegistry();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private volatile boolean running = true;
    private java.net.ServerSocket serverSocket;

    public ChatNode(NodeConfig config) {
        this.config = config;

        this.chatService = new ChatService(
                config.nickname(),
                registry,
                this::shutdown
        );

        this.consoleInput =
                new ConsoleInput(chatService);
    }

    public void start() throws IOException {
        serverSocket = new java.net.ServerSocket(config.port());
        console("Listening on %s:%d as '%s'".formatted(config.host(), config.port(), config.nickname()));

        executor.submit(this::acceptLoop);

        PeerAddress self = config.selfAddress();
        for (PeerAddress peer : config.knownPeers()) {
            if (self.compareTo(peer) < 0) {
                executor.submit(() -> connectToPeer(peer, CONNECT_MAX_RETRIES));
            }
        }

        consoleInput.run();

        if (running) {
            shutdown();
        }
    }

    // ---------------------------------------------------------------- accept

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                executor.submit(() -> onSocketEstablished(socket));
            } catch (SocketException e) {
                if (running) {
                    console("Accept loop stopped unexpectedly: " + e.getMessage());
                }
                return; // serverSocket was closed as part of shutdown()
            } catch (IOException e) {
                console("Failed to accept connection: " + e.getMessage());
            }
        }
    }

    // --------------------------------------------------------------- connect

    private void connectToPeer(PeerAddress peer, int attemptsLeft) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(peer.host(), peer.port()), (int) CONNECT_TIMEOUT.toMillis());
            onSocketEstablished(socket);
        } catch (IOException e) {
            if (attemptsLeft <= 1 || !running) {
                console("Giving up connecting to %s: %s".formatted(peer, e.getMessage()));
                return;
            }
            sleepQuietly(CONNECT_RETRY_DELAY);
            connectToPeer(peer, attemptsLeft - 1);
        }
    }

    private void onSocketEstablished(Socket socket) {
        try {
            PeerConnection connection = new PeerConnection(socket, this);
            connection.start();
            connection.send(new Message.JoinMessage(config.nickname(), config.port()));
        } catch (IOException e) {
            console("Failed to initialize connection with " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
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
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // Closing on shutdown; nothing to react to.
        }
        executor.shutdownNow();
    }

    private void console(String text) {
        System.out.println(text);
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}