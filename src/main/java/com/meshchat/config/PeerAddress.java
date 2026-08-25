package com.meshchat.config;

public record PeerAddress(String host, int port) implements Comparable<PeerAddress> {

    public static PeerAddress parse(String hostPort) {
        String trimmed = hostPort.trim();
        int separator = trimmed.lastIndexOf(':');
        if (separator <= 0 || separator == trimmed.length() - 1) {
            throw new IllegalArgumentException(
                    "Invalid peer address '%s', expected host:port".formatted(hostPort));
        }
        String host = trimmed.substring(0, separator);
        int port = Integer.parseInt(trimmed.substring(separator + 1));
        return new PeerAddress(host, port);
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }

    @Override
    public int compareTo(PeerAddress other) {
        return this.toString().compareTo(other.toString());
    }
}