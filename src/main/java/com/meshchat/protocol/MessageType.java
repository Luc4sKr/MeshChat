package com.meshchat.protocol;

public enum MessageType {

    JOIN((byte) 1),
    LEAVE((byte) 2),
    CHAT((byte) 3),
    PRIVATE((byte) 4),
    HEARTBEAT((byte) 5),
    LIST_REQUEST((byte) 6);

    private final byte code;

    MessageType(byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static MessageType fromCode(byte code) {
        for (MessageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message type code: " + code);
    }
}
