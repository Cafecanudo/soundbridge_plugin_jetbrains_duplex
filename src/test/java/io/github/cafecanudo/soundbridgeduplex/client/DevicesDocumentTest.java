package io.github.cafecanudo.soundbridgeduplex.client;

import io.github.cafecanudo.soundbridgeduplex.cli.CliException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DevicesDocumentTest {

    private static final String DOCUMENT = """
            {
              "schema": 1,
              "outputs": [
                {"id": "dq8f", "name": "Alto-falantes (Onboard)", "channels": 2, "samplerate": 48000, "index": 13}
              ],
              "inputs": [
                {"id": "v2yh", "name": "Microfone (Onboard)", "channels": 2, "samplerate": 48000, "index": 14},
                {"id": "9buz", "name": "Microfone (Headset)", "channels": 1, "samplerate": 48000, "index": 15}
              ],
              "aliases": {"dados": "dq8f"},
              "defaults": {"out": "dados", "dir": "D:\\\\recebidos"},
              "config": "C:\\\\Users\\\\x\\\\soundbridge\\\\config.json"
            }
            """;

    @Test
    void parsesDevicesAliasesAndDefaults() throws CliException {
        DevicesDocument document = DevicesDocument.parse(DOCUMENT);

        assertEquals(List.of(new AudioDevice("dq8f", "Alto-falantes (Onboard)", 2)), document.outputs());
        assertEquals(2, document.inputs().size());
        assertEquals(Map.of("dados", "dq8f"), document.aliases());
        assertEquals(new DeviceDefaults("dados", "", "D:\\recebidos"), document.defaults());
        assertEquals("C:\\Users\\x\\soundbridge\\config.json", document.configPath());
    }

    @Test
    void rejectsTextOutput() {
        assertThrows(CliException.class, () -> DevicesDocument.parse("SAÍDAS (--out):"));
    }

    @Test
    void rejectsMissingSection() {
        assertThrows(CliException.class, () -> DevicesDocument.parse("{\"schema\": 1, \"inputs\": []}"));
    }

    @Test
    void labelsDeviceWithIdAndName() {
        assertEquals("dq8f · Alto-falantes", new AudioDevice("dq8f", "Alto-falantes", 2).label());
    }
}
