package com.meshchat.peer;

import com.meshchat.config.PeerAddress;
import com.meshchat.protocol.Message;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PeerEventHandler implements PeerConnectionListener {

    private final PeerRegistry peerRegistry;
    private final Consumer<PeerAddress> discoveredPeerHandler;
    private final String localNickname;
    private final Set<String> seenChatMessages = ConcurrentHashMap.newKeySet();
    private final Set<String> seenJoinMessages = ConcurrentHashMap.newKeySet();
    private final Set<String> seenListRequestMessages = ConcurrentHashMap.newKeySet();
    private final Set<String> seenPrivateMessages = ConcurrentHashMap.newKeySet();

    public PeerEventHandler(PeerRegistry peerRegistry, Consumer<PeerAddress> discoveredPeerHandler, String localNickname) {
        this.peerRegistry = Objects.requireNonNull(peerRegistry);
        this.discoveredPeerHandler = Objects.requireNonNull(discoveredPeerHandler);
        this.localNickname = Objects.requireNonNull(localNickname);
    }

    public void rememberSeenChatMessage(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return;
        }
        seenChatMessages.add(messageId);
    }

    @Override
    public void onMessage(
            PeerConnection connection,
            Message message
    ) {
        switch (message) {
            case Message.JoinMessage join -> handleJoin(connection, join);

            case Message.LeaveMessage leave -> handleLeave(connection, leave);

            case Message.ChatMessage chat -> handleChat(connection, chat);

            case Message.PrivateMessage privateMessage -> handlePrivateMessage(connection, privateMessage);

            case Message.ListRequestMessage listRequest -> handleListRequest(connection, listRequest);

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
            PeerConnection source,
            Message.JoinMessage message
    ) {
        String fingerprint = message.nickname() + "\u0000" + message.listenPort();
        if (!seenJoinMessages.add(fingerprint)) {
            return;
        }

        source.rememberRemotePeer(source.remoteHost(), message.listenPort());

        boolean directJoin =
                source.nickname() == null || source.nickname().equals(message.nickname());

        if (directJoin) {
            boolean isNew = !peerRegistry.isKnown(message.nickname());

            if (source.nickname() == null) {
                source.setNickname(message.nickname());
            }

            peerRegistry.register(message.nickname(), source);

            if (isNew) {
                console("* " + message.nickname() + " joined the chat");
            }
        } else {
            boolean isNew = !peerRegistry.isKnown(message.nickname());
            peerRegistry.rememberKnown(message.nickname());

            if (isNew) {
                console("* " + message.nickname() + " joined the chat");
                discoveredPeerHandler.accept(
                        new PeerAddress(source.remoteHost(), message.listenPort())
                );
            }
        }

        for (PeerConnection connection : peerRegistry.allExcluding(source)) {
            connection.send(message);
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

    private void handleChat(
            PeerConnection source,
            Message.ChatMessage message
    ) {
        String fingerprint = message.messageId();
        if (fingerprint == null || fingerprint.isBlank()) {
            fingerprint = message.sender() + "\u0000" + message.text();
        }

        if (!seenChatMessages.add(fingerprint)) {
            return;
        }

        console(
                message.sender()
                        + ": "
                        + message.text()
        );

        for (PeerConnection connection : peerRegistry.allExcluding(source)) {
            connection.send(message);
        }
    }

    public static boolean shouldDisplayListRequestResult(
            String localNickname,
            Message.ListRequestMessage message
    ) {
        return localNickname != null
                && !localNickname.isBlank()
                && localNickname.equals(message.requester());
    }

    private void handleListRequest(
            PeerConnection source,
            Message.ListRequestMessage message
    ) {
        if (!seenListRequestMessages.add(message.requestId())) {
            return;
        }

        var discoveredNodes = new LinkedHashSet<>(message.discoveredNodes());
        discoveredNodes.addAll(peerRegistry.nicknames());

        if (shouldDisplayListRequestResult(localNickname, message)) {
            console(
                    "Known participants: "
                            + String.join(", ", discoveredNodes.stream().sorted().toList())
            );
        }

        var forwardedMessage = new Message.ListRequestMessage(
                message.requester(),
                message.requestId(),
                discoveredNodes
        );

        for (PeerConnection connection : peerRegistry.allExcluding(source)) {
            String remoteNickname = connection.nickname();
            if (remoteNickname != null && discoveredNodes.contains(remoteNickname)) {
                continue;
            }
            connection.send(forwardedMessage);
        }
    }

    private void handlePrivateMessage(
            PeerConnection source,
            Message.PrivateMessage message
    ) {
        String fingerprint = message.messageId();
        if (!seenPrivateMessages.add(fingerprint)) {
            return;
        }

        if (localNickname.equals(message.recipient())) {
            console(
                    "(private) "
                            + message.sender()
                            + ": "
                            + message.text()
            );
            return;
        }

        var updatedRoute = new ArrayList<>(message.route());
        if (!updatedRoute.contains(localNickname)) {
            updatedRoute.add(localNickname);
        }

        Message.PrivateMessage forwarded = new Message.PrivateMessage(
                message.sender(),
                message.recipient(),
                message.text(),
                updatedRoute,
                message.messageId()
        );

        var directRecipient = peerRegistry.find(message.recipient());
        if (directRecipient.isPresent() && directRecipient.get() != source) {
            directRecipient.get().send(forwarded);
            return;
        }

        for (PeerConnection connection : peerRegistry.allExcluding(source)) {
            String remoteNickname = connection.nickname();
            if (remoteNickname != null && updatedRoute.contains(remoteNickname)) {
                continue;
            }
            connection.send(forwarded);
        }
    }

    private void handleHeartbeat() {
        // Keep-alive only.
    }

    private void console(String message) {
        System.out.println(message);
    }
}