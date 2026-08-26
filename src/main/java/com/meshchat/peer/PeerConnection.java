package com.meshchat.peer;

import com.meshchat.net.FrameCodec;
import com.meshchat.protocol.Message;
import com.meshchat.protocol.MessageCodec;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class PeerConnection {

    private static final int OUTBOUND_QUEUE_CAPACITY = 256;
    private static final Duration ENQUEUE_TIMEOUT = Duration.ofSeconds(2);
    private static final int READ_TIMEOUT_MILLIS = 30_000;
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(10);

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final PeerConnectionListener listener;
    private final BlockingQueue<Message> outboundQueue = new LinkedBlockingQueue<>(OUTBOUND_QUEUE_CAPACITY);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicReference<String> nickname = new AtomicReference<>();

    private Thread readerThread;
    private Thread writerThread;
    private Thread heartbeatThread;

    public PeerConnection(Socket socket, PeerConnectionListener listener) throws IOException {
        this.socket = socket;
        this.socket.setSoTimeout(READ_TIMEOUT_MILLIS);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.listener = listener;
    }

    public void start() {
        readerThread = Thread.ofVirtual().name("peer-reader-" + remoteAddress()).start(this::readLoop);
        writerThread = Thread.ofVirtual().name("peer-writer-" + remoteAddress()).start(this::writeLoop);
        heartbeatThread = Thread.ofVirtual().name("peer-heartbeat-" + remoteAddress()).start(this::heartbeatLoop);
    }

    public void send(Message message) {
        if (closed.get()) {
            return;
        }
        try {
            boolean accepted = outboundQueue.offer(message, ENQUEUE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!accepted) {
                close();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void readLoop() {
        try {
            while (!closed.get()) {
                byte[] payload = FrameCodec.readFrame(in);
                Message message = MessageCodec.decode(payload);
                listener.onMessage(this, message);
            }
        } catch (SocketTimeoutException e) {
            // No traffic (not even a heartbeat) within the deadline: treat as dead.
        } catch (EOFException e) {
            // Peer closed the socket.
        } catch (IOException e) {
            // Any other I/O failure: connection is no longer usable.
        } finally {
            close();
        }
    }

    private void writeLoop() {
        try {
            while (!closed.get()) {
                Message message = outboundQueue.take();
                FrameCodec.writeFrame(out, MessageCodec.encode(message));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // Writing failed: connection is no longer usable.
        } finally {
            close();
        }
    }

    private void heartbeatLoop() {
        try {
            while (!closed.get()) {
                Thread.sleep(HEARTBEAT_INTERVAL.toMillis());
                send(new Message.HeartbeatMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void close() {
        if (closed.compareAndSet(false, true)) {
            drainPendingOutbound();
            readerThread.interrupt();
            writerThread.interrupt();
            heartbeatThread.interrupt();
            try {
                socket.close();
            } catch (IOException ignored) {
                // Nothing meaningful to do if closing the socket fails.
            }
            listener.onDisconnect(this);
        }
    }

    private static final Duration CLOSE_DRAIN_TIMEOUT = Duration.ofMillis(500);

    private void drainPendingOutbound() {
        long deadline = System.currentTimeMillis() + CLOSE_DRAIN_TIMEOUT.toMillis();
        while (!outboundQueue.isEmpty() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public boolean isClosed() {
        return closed.get();
    }

    public String nickname() {
        return nickname.get();
    }

    public void setNickname(String value) {
        nickname.set(value);
    }

    public String remoteAddress() {
        return socket.getRemoteSocketAddress().toString();
    }
}
