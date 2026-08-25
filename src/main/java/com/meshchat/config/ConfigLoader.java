package com.meshchat.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ConfigLoader {

    private ConfigLoader() {
    }

    public static NodeConfig load(String[] args) throws IOException {
        Map<String, String> values = (args.length == 1 && args[0].endsWith(".properties"))
                ? loadFromFile(Path.of(args[0]))
                : loadFromArgs(args);

        String nickname = require(values, "nickname");
        String host = values.getOrDefault("host", "localhost");
        int port = Integer.parseInt(require(values, "port"));
        List<PeerAddress> peers = parsePeers(values.getOrDefault("peers", ""));

        return new NodeConfig(nickname, host, port, peers);
    }

    private static Map<String, String> loadFromArgs(String[] args) {
        Map<String, String> values = new HashMap<>();
        for (String arg : args) {
            if (!arg.startsWith("--") || !arg.contains("=")) {
                throw new IllegalArgumentException(
                        "Invalid argument '%s', expected --key=value".formatted(arg));
            }
            String[] parts = arg.substring(2).split("=", 2);
            values.put(parts[0].trim(), parts[1].trim());
        }
        return values;
    }

    private static Map<String, String> loadFromFile(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        Map<String, String> values = new HashMap<>();
        for (String name : properties.stringPropertyNames()) {
            values.put(name, properties.getProperty(name));
        }
        return values;
    }

    private static List<PeerAddress> parsePeers(String csv) {
        List<PeerAddress> peers = new ArrayList<>();
        if (csv.isBlank()) {
            return peers;
        }
        for (String entry : csv.split(",")) {
            if (!entry.isBlank()) {
                peers.add(PeerAddress.parse(entry));
            }
        }
        return peers;
    }

    private static String require(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required config key: " + key);
        }
        return value;
    }
}

