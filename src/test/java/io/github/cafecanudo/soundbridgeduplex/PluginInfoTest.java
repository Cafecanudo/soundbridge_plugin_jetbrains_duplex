package io.github.cafecanudo.soundbridgeduplex;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PluginInfoTest {

    @Test
    void versionComesFromTheBuild() {
        assertNotEquals("?", PluginInfo.version());
        assertFalse(PluginInfo.version().contains("${"));
    }
}
