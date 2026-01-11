package com.syncstudy.WS;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/config.properties")) {
            if (in == null) {
                System.err.println("config.properties not found, using defaults");
            } else {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("Failed to load config.properties: " + e.getMessage());
        }
    }

    public static String getChatHost() {
        return props.getProperty("chat.server.host", "localhost");
    }

    public static int getChatPort() {
        return Integer.parseInt(props.getProperty("chat.server.port", "9000"));
    }

}
