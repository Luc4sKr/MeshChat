package com.meshchat.server;

import java.net.Socket;

@FunctionalInterface
public interface SocketHandler {

    void handle(Socket socket);
}