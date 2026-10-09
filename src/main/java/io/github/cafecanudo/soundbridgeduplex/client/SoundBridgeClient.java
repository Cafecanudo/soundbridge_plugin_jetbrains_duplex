package io.github.cafecanudo.soundbridgeduplex.client;

import io.github.cafecanudo.soundbridgeduplex.cli.CliCommand;
import io.github.cafecanudo.soundbridgeduplex.cli.CliException;
import io.github.cafecanudo.soundbridgeduplex.cli.CliOutput;
import io.github.cafecanudo.soundbridgeduplex.cli.CliRunner;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public final class SoundBridgeClient {

    private static final Duration VERSION_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration DEVICES_TIMEOUT = Duration.ofSeconds(60);
    private static final Path WORK_DIRECTORY = Path.of(System.getProperty("user.home"));

    private final String launcher;

    public SoundBridgeClient(String launcher) {
        this.launcher = launcher;
    }

    public String version() throws CliException {
        CliOutput output = run(List.of("--version"), VERSION_TIMEOUT);
        return output.stdout().strip();
    }

    public DevicesDocument listDevices() throws CliException {
        CliOutput output = run(List.of("--list-devices", CliCommand.JSON_FLAG), DEVICES_TIMEOUT);
        return DevicesDocument.parse(output.stdout());
    }

    private CliOutput run(List<String> arguments, Duration timeout) throws CliException {
        CliOutput output = CliRunner.run(CliCommand.of(launcher, arguments), WORK_DIRECTORY, timeout);
        if (!output.isSuccess()) {
            throw new CliException(output.describeFailure());
        }
        return output;
    }
}
