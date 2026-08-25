package com.meshchat.protocol;

public sealed interface Message {
    record JoinMessage(String nickname, int listenPort) implements Message {
    }

    record LeaveMessage(String nickname) implements Message {
    }

    record ChatMessage(String sender, String text) implements Message {
    }

    record PrivateMessage(String sender, String recipient, String text) implements Message {
    }

    record HeartbeatMessage() implements Message {
    }
}