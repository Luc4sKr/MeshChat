package com.meshchat;

import com.meshchat.config.ConfigLoader;
import com.meshchat.config.NodeConfig;
import com.meshchat.node.ChatNode;

public class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            NodeConfig config = ConfigLoader.load(args);
            new ChatNode(config).start();
        } catch (IllegalArgumentException e) {
            System.err.println("Configuration error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            System.exit(1);
        }
    }
}
