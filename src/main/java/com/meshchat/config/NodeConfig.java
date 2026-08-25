package com.meshchat.config;

import java.util.List;

public record NodeConfig(String nickname, String host, int port, List<PeerAddress> knownPeers) {

    public PeerAddress selfAddress() {
        return new PeerAddress(host, port);
    }
}
