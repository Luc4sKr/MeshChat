package com.meshchat.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class MessageCodec {

    private MessageCodec() {
    }

    public static byte[] encode(Message message) {
        var buffer = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(buffer)) {
            switch (message) {
                case Message.JoinMessage m -> {
                    out.writeByte(MessageType.JOIN.code());
                    writeString(out, m.nickname());
                    out.writeInt(m.listenPort());
                }
                case Message.LeaveMessage m -> {
                    out.writeByte(MessageType.LEAVE.code());
                    writeString(out, m.nickname());
                }
                case Message.ChatMessage m -> {
                    out.writeByte(MessageType.CHAT.code());
                    writeString(out, m.sender());
                    writeString(out, m.text());
                    writeString(out, m.messageId());
                }
                case Message.PrivateMessage m -> {
                    out.writeByte(MessageType.PRIVATE.code());
                    writeString(out, m.sender());
                    writeString(out, m.recipient());
                    writeString(out, m.text());
                    writeString(out, m.messageId());
                    out.writeInt(m.route().size());
                    for (String node : m.route()) {
                        writeString(out, node);
                    }
                }
                case Message.ListRequestMessage m -> {
                    out.writeByte(MessageType.LIST_REQUEST.code());
                    writeString(out, m.requester());
                    writeString(out, m.requestId());
                    out.writeInt(m.discoveredNodes().size());
                    for (String discoveredNode : m.discoveredNodes()) {
                        writeString(out, discoveredNode);
                    }
                }
                case Message.HeartbeatMessage m -> out.writeByte(MessageType.HEARTBEAT.code());
            }
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return buffer.toByteArray();
    }

    public static Message decode(byte[] payload) throws IOException {
        try (var in = new DataInputStream(new ByteArrayInputStream(payload))) {
            MessageType type = MessageType.fromCode(in.readByte());
            return switch (type) {
                case JOIN -> new Message.JoinMessage(readString(in), in.readInt());
                case LEAVE -> new Message.LeaveMessage(readString(in));
                case CHAT -> new Message.ChatMessage(readString(in), readString(in), readString(in));
                case PRIVATE -> decodePrivateMessage(in);
                case LIST_REQUEST -> decodeListRequest(in);
                case HEARTBEAT -> new Message.HeartbeatMessage();
            };
        }
    }

    private static Message.PrivateMessage decodePrivateMessage(DataInputStream in) throws IOException {
        String sender = readString(in);
        String recipient = readString(in);
        String text = readString(in);
        String messageId = readString(in);
        int routeSize = in.readInt();
        List<String> route = new ArrayList<>();
        for (int i = 0; i < routeSize; i++) {
            route.add(readString(in));
        }
        return new Message.PrivateMessage(sender, recipient, text, route, messageId);
    }

    private static Message.ListRequestMessage decodeListRequest(DataInputStream in) throws IOException {
        String requester = readString(in);
        String requestId = readString(in);
        int discoveredNodesSize = in.readInt();
        List<String> discoveredNodes = new ArrayList<>();
        for (int i = 0; i < discoveredNodesSize; i++) {
            discoveredNodes.add(readString(in));
        }
        return new Message.ListRequestMessage(requester, requestId, discoveredNodes);
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0) {
            throw new IOException("Invalid string length: " + length);
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}