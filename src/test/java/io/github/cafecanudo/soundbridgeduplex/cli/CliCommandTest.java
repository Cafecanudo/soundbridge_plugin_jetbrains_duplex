package io.github.cafecanudo.soundbridgeduplex.cli;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CliCommandTest {

    @Test
    void splitsLauncherRespectingQuotes() {
        CliCommand command = CliCommand.of("\"C:\\Program Files\\Python\\python.exe\" -m soundbridge", List.of("--tx", "a.bin"));

        assertEquals(List.of("C:\\Program Files\\Python\\python.exe", "-m", "soundbridge", "--tx", "a.bin"),
                command.fullCommand());
    }

    @Test
    void addsJsonFlagOnce() {
        CliCommand command = CliCommand.of(CliCommand.DEFAULT_LAUNCHER, List.of("--list-devices"));

        assertEquals(List.of("--list-devices", "--json"), command.withJson().withJson().arguments());
    }

    @Test
    void rejectsEmptyLauncher() {
        assertThrows(IllegalArgumentException.class, () -> CliCommand.of("  ", List.of()));
    }
}
