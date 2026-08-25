package com.meshchat.net;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * Length-prefixed message framing over a raw TCP stream.
 *
 * <p>Every frame on the wire is: {@code [4-byte big-endian length][payload]}.
 * This is the same delimiting strategy used by the SocketChat base
 * application, reused here so that a long message or a burst of short
 * messages can never be truncated or glued together on the reading side.</p>
 */
public final class FrameCodec {

    /** Hard cap to protect against a malformed/malicious length prefix. */
    private static final int MAX_FRAME_SIZE = 16 * 1024 * 1024; // 16 MB

    private FrameCodec() {
    }

    public static void writeFrame(DataOutputStream out, byte[] payload) throws IOException {
        out.writeInt(payload.length);
        out.write(payload);
        out.flush();
    }

    /**
     * Blocks until a full frame is available, the configured
     * {@code SO_TIMEOUT} elapses ({@link java.net.SocketTimeoutException}),
     * or the stream ends ({@link EOFException}).
     */
    public static byte[] readFrame(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > MAX_FRAME_SIZE) {
            throw new IOException("Invalid frame length: " + length);
        }
        byte[] payload = new byte[length];
        in.readFully(payload);
        return payload;
    }
}