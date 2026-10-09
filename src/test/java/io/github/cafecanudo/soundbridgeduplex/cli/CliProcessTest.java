package io.github.cafecanudo.soundbridgeduplex.cli;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliProcessTest {

    private static final long TIMEOUT_SECONDS = 60;

    @Test
    void routesEventsTextAndExitCode() throws Exception {
        RecordingListener listener = new RecordingListener();

        CliProcess process = CliProcess.start(fakeCli(), Path.of("."), listener);

        assertTrue(listener.exited.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue(process.waitFor(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)));
        assertFalse(process.isRunning());
        assertEquals(List.of("info", "progress", "result"), listener.events.stream().map(CliEvent::name).toList());
        assertEquals("ação · ok", listener.events.get(0).string("message").orElseThrow());
        assertEquals(List.of("texto livre", "--json", "sem quebra"), listener.stdout);
        assertEquals(List.of("aviso no stderr"), listener.stderr);
        assertEquals(CliExitCode.DEVICE, listener.exitCode);
    }

    private static CliCommand fakeCli() throws URISyntaxException {
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        Path source = Path.of(CliProcessTest.class.getResource("/FakeCli.java").toURI());
        return new CliCommand(List.of(java.toString(), source.toString()), List.of());
    }

    private static final class RecordingListener implements CliListener {

        private final List<CliEvent> events = new CopyOnWriteArrayList<>();
        private final List<String> stdout = new CopyOnWriteArrayList<>();
        private final List<String> stderr = new CopyOnWriteArrayList<>();
        private final CountDownLatch exited = new CountDownLatch(1);
        private volatile CliExitCode exitCode;

        @Override
        public void onEvent(CliEvent event) {
            events.add(event);
        }

        @Override
        public void onText(String line, boolean error) {
            (error ? stderr : stdout).add(line);
        }

        @Override
        public void onExit(CliExitCode code, int rawCode) {
            exitCode = code;
            exited.countDown();
        }
    }
}
