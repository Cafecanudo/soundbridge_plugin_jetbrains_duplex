package io.github.cafecanudo.soundbridgeduplex.cli;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle;

import java.nio.file.Path;
import java.time.Duration;

public final class CliRunner {

    private CliRunner() {
    }

    public static CliOutput run(CliCommand command, Path workDirectory, Duration timeout) throws CliException {
        ProcessOutput output = execute(command, workDirectory, timeout);
        if (output.isTimeout()) {
            String commandLine = String.join(" ", command.fullCommand());
            throw new CliException(SoundBridgeBundle.message("cli.timeout", timeout.toSeconds(), commandLine));
        }
        return new CliOutput(output.getExitCode(), output.getStdout(), output.getStderr());
    }

    private static ProcessOutput execute(CliCommand command, Path workDirectory, Duration timeout) throws CliException {
        try {
            CapturingProcessHandler handler = new CapturingProcessHandler(command.toCommandLine(workDirectory));
            return handler.runProcess((int) timeout.toMillis(), true);
        } catch (ExecutionException e) {
            throw new CliException(e.getMessage(), e);
        }
    }
}
