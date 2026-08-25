package com.meshchat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public final class PeerServer implements AutoCloseable {

    private final int port;
    private final SocketHandler socketHandler;
    private final ExecutorService executor;

    private volatile boolean running;
    private ServerSocket serverSocket;

    public PeerServer(
            int port,
            SocketHandler socketHandler,
            ExecutorService executor
    ) {
        this.port = port;
        this.socketHandler = Objects.requireNonNull(socketHandler);
        this.executor = Objects.requireNonNull(executor);
    }

    public void start() throws IOException {
        if (running) {
            return;
        }

        serverSocket = new ServerSocket(port);
        running = true;

        executor.submit(this::acceptLoop);
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();

                executor.submit(
                        () -> socketHandler.handle(socket)
                );

            } catch (SocketException e) {
                if (running) {
                    System.err.println(
                            "Accept loop stopped unexpectedly: "
                                    + e.getMessage()
                    );
                }

                return;

            } catch (IOException e) {
                if (running) {
                    System.err.println(
                            "Failed to accept connection: "
                                    + e.getMessage()
                    );
                }
            }
        }
    }

    @Override
    public void close() {
        running = false;

        if (serverSocket == null) {
            return;
        }

        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // Nothing useful to do during shutdown.
        }
    }
}