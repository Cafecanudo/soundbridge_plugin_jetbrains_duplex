package io.github.cafecanudo.soundbridgeduplex.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliEventParserTest {

    @Test
    void parsesProgressEvent() {
        CliEvent event = CliEventParser.parse(
                "{\"schema\": 1, \"event\": \"progress\", \"name\": \"a.bin\", \"fraction\": 0.5, \"eta_s\": null}")
                .orElseThrow();

        assertEquals(1, event.schema());
        assertEquals("progress", event.name());
        assertEquals("a.bin", event.string("name").orElseThrow());
        assertEquals(0.5, event.number("fraction").orElseThrow());
        assertTrue(event.number("eta_s").isEmpty());
        assertFalse(event.data().has("schema"));
        assertFalse(event.data().has("event"));
    }

    @Test
    void readsBooleanField() {
        CliEvent event = CliEventParser.parse("{\"schema\": 1, \"event\": \"result\", \"ok\": true}").orElseThrow();

        assertEquals(Boolean.TRUE, event.bool("ok").orElseThrow());
    }

    @Test
    void rejectsPlainText() {
        assertTrue(CliEventParser.parse("aguardando transmissão").isEmpty());
    }

    @Test
    void rejectsMalformedJson() {
        assertTrue(CliEventParser.parse("{\"schema\": 1, \"event\": ").isEmpty());
    }

    @Test
    void rejectsObjectWithoutSchema() {
        assertTrue(CliEventParser.parse("{\"event\": \"info\"}").isEmpty());
    }

    @Test
    void flagsNewerSchema() {
        CliEvent event = CliEventParser.parse("{\"schema\": 2, \"event\": \"info\"}").orElseThrow();

        assertFalse(event.isSupportedSchema());
    }
}
