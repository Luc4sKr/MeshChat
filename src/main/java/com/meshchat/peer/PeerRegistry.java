package com.meshchat.peer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class PeerRegistry {

    private final ConcurrentHashMap<String, PeerConnection> connections = new ConcurrentHashMap<>();

    public void register(String nickname, PeerConnection connection) {
        connections.put(nickname, connection);
    }

    public void remove(String nickname) {
        if (nickname != null) {
            connections.remove(nickname);
        }
    }

    public void remove(PeerConnection connection) {
        connections.values().remove(connection);
    }

    public Optional<PeerConnection> find(String nickname) {
        return Optional.ofNullable(connections.get(nickname));
    }

    public Collection<PeerConnection> all() {
        return List.copyOf(connections.values());
    }

    public List<String> nicknames() {
        return connections.keySet().stream().sorted().toList();
    }

    public boolean isKnown(String nickname) {
        return connections.containsKey(nickname);
    }
}