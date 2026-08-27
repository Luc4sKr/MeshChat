package com.meshchat.peer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class PeerRegistry {

    private final ConcurrentHashMap<String, PeerConnection> connections = new ConcurrentHashMap<>();
    private final Set<String> knownPeers = ConcurrentHashMap.newKeySet();

    public void register(String nickname, PeerConnection connection) {
        connections.put(nickname, connection);
        knownPeers.add(nickname);
    }

    public void rememberKnown(String nickname) {
        if (nickname != null && !nickname.isBlank()) {
            knownPeers.add(nickname);
        }
    }

    public void remove(String nickname) {
        if (nickname != null) {
            connections.remove(nickname);
            knownPeers.remove(nickname);
        }
    }

    public void remove(PeerConnection connection) {
        connections.entrySet().removeIf(entry -> entry.getValue() == connection);
    }

    public Optional<PeerConnection> find(String nickname) {
        return Optional.ofNullable(connections.get(nickname));
    }

    public Collection<PeerConnection> all() {
        return List.copyOf(connections.values());
    }

    public Collection<PeerConnection> allExcluding(PeerConnection excluded) {
        return connections.values().stream()
                .filter(connection -> connection != excluded)
                .filter(connection -> !connection.samePeerAs(excluded))
                .toList();
    }

    public List<String> nicknames() {
        return Stream.concat(
                        connections.keySet().stream(),
                        knownPeers.stream()
                )
                .distinct()
                .sorted()
                .toList();
    }

    public boolean isKnown(String nickname) {
        return connections.containsKey(nickname) || knownPeers.contains(nickname);
    }
}