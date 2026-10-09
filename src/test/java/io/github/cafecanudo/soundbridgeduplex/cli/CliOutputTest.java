package io.github.cafecanudo.soundbridgeduplex.cli;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CliOutputTest {

    @Test
    void describesFailureFromInfoMessagesAndStderr() {
        CliOutput output = new CliOutput(3,
                "{\"schema\": 1, \"event\": \"info\", \"message\": \"erro em --out: device não encontrado\"}\n",
                "Traceback\n");

        assertEquals(CliExitCode.DEVICE, output.exitCode());
        assertFalse(output.isSuccess());
        assertEquals(List.of("erro em --out: device não encontrado"), output.messages());
        assertEquals("erro em --out: device não encontrado\nTraceback", output.describeFailure());
    }

    @Test
    void fallsBackToPlainStdout() {
        CliOutput output = new CliOutput(2, "usage: soundbridge\n", "");

        assertEquals("usage: soundbridge", output.describeFailure());
    }
}
