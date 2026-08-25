package com.meshchat.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public final class MessageCodec {

    private MessageCodec() {
    }

    public static byte[] encode(Message message) {
        var buffer = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(buffer)) {
            switch (message) {
                case Message.JoinMessage m -> {
                    out.writeByte(MessageType.JOIN.code());
                    out.writeUTF(m.nickname());
                    out.writeInt(m.listenPort());
                }
                case Message.LeaveMessage m -> {
                    out.writeByte(MessageType.LEAVE.code());
                    out.writeUTF(m.nickname());
                }
                case Message.ChatMessage m -> {
                    out.writeByte(MessageType.CHAT.code());
                    out.writeUTF(m.sender());
                    out.writeUTF(m.text());
                }
                case Message.PrivateMessage m -> {
                    out.writeByte(MessageType.PRIVATE.code());
                    out.writeUTF(m.sender());
                    out.writeUTF(m.recipient());
                    out.writeUTF(m.text());
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
                case JOIN -> new Message.JoinMessage(in.readUTF(), in.readInt());
                case LEAVE -> new Message.LeaveMessage(in.readUTF());
                case CHAT -> new Message.ChatMessage(in.readUTF(), in.readUTF());
                case PRIVATE -> new Message.PrivateMessage(in.readUTF(), in.readUTF(), in.readUTF());
                case HEARTBEAT -> new Message.HeartbeatMessage();
            };
        }
    }
}