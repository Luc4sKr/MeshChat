package com.meshchat.protocol;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public sealed interface Message {
    record JoinMessage(String nickname, int listenPort) implements Message {
    }

    record LeaveMessage(String nickname) implements Message {
    }

    record ChatMessage(String sender, String text, String messageId) implements Message {
        public ChatMessage(String sender, String text) {
            this(sender, text, UUID.randomUUID().toString());
        }
    }

    record PrivateMessage(String sender, String recipient, String text, List<String> route, String messageId) implements Message {
        public PrivateMessage(String sender, String recipient, String text) {
            this(sender, recipient, text, List.of(sender), UUID.randomUUID().toString());
        }

        public PrivateMessage(String sender, String recipient, String text, Collection<String> route) {
            this(sender, recipient, text, route, UUID.randomUUID().toString());
        }

        public PrivateMessage(String sender, String recipient, String text, Collection<String> route, String messageId) {
            this(sender, recipient, text, normalizeRoute(route), messageId == null || messageId.isBlank() ? UUID.randomUUID().toString() : messageId);
        }

        public PrivateMessage {
            route = normalizeRoute(route);
            if (messageId == null || messageId.isBlank()) {
                messageId = UUID.randomUUID().toString();
            }
        }

        private static List<String> normalizeRoute(Collection<String> route) {
            if (route == null || route.isEmpty()) {
                return List.of();
            }

            return route.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();
        }
    }

    record ListRequestMessage(String requester, String requestId, List<String> discoveredNodes) implements Message {
        public ListRequestMessage(String requester, String requestId) {
            this(requester, requestId, List.of());
        }

        public ListRequestMessage(String requester, String requestId, Collection<String> discoveredNodes) {
            this(requester, requestId, normalizeDiscoveredNodes(discoveredNodes));
        }

        public ListRequestMessage {
            discoveredNodes = normalizeDiscoveredNodes(discoveredNodes);
        }

        private static List<String> normalizeDiscoveredNodes(Collection<String> discoveredNodes) {
            if (discoveredNodes == null || discoveredNodes.isEmpty()) {
                return List.of();
            }

            return discoveredNodes.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(name -> !name.isBlank())
                    .distinct()
                    .sorted()
                    .toList();
        }
    }

    record HeartbeatMessage() implements Message {
    }
}