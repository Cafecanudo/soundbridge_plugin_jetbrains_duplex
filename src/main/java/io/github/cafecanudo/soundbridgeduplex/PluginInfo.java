package io.github.cafecanudo.soundbridgeduplex;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class PluginInfo {

    private static final String RESOURCE = "plugin-info.properties";
    private static final String VERSION = "version";
    private static final String UNKNOWN = "?";
    private static final String VERSION_VALUE = load();

    private PluginInfo() {
    }

    public static String version() {
        return VERSION_VALUE;
    }

    private static String load() {
        try (InputStream input = PluginInfo.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                return UNKNOWN;
            }
            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty(VERSION, UNKNOWN);
        } catch (IOException e) {
            return UNKNOWN;
        }
    }
}
