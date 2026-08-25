package com.meshchat.peer;


import com.meshchat.protocol.Message;

public interface PeerConnectionListener {

    void onMessage(PeerConnection connection, Message message);

    void onDisconnect(PeerConnection connection);
}
