package com.meshchat.connection;

import com.meshchat.config.PeerAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Objects;

public final class PeerConnector {

    private final Duration connectTimeout;
    private final int maxRetries;
    private final Duration retryDelay;

    public PeerConnector(
            Duration connectTimeout,
            int maxRetries,
            Duration retryDelay
    ) {
        if (connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException(
                    "Connect timeout must be greater than zero"
            );
        }

        if (maxRetries < 1) {
            throw new IllegalArgumentException(
                    "Max retries must be at least one"
            );
        }

        if (retryDelay.isNegative()) {
            throw new IllegalArgumentException(
                    "Retry delay cannot be negative"
            );
        }

        this.connectTimeout = connectTimeout;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
    }

    public Socket connect(PeerAddress peer) throws IOException {
        Objects.requireNonNull(peer);

        IOException lastFailure = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Socket socket = new Socket();

                socket.connect(
                        new InetSocketAddress(
                                peer.host(),
                                peer.port()
                        ),
                        (int) connectTimeout.toMillis()
                );

                return socket;

            } catch (IOException e) {
                lastFailure = e;

                if (attempt < maxRetries) {
                    waitBeforeRetry();
                }
            }
        }

        throw new IOException(
                "Failed to connect to %s after %d attempts"
                        .formatted(peer, maxRetries),
                lastFailure
        );
    }

    private void waitBeforeRetry() throws IOException {
        try {
            Thread.sleep(retryDelay.toMillis());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IOException(
                    "Connection attempt interrupted",
                    e
            );
        }
    }
}