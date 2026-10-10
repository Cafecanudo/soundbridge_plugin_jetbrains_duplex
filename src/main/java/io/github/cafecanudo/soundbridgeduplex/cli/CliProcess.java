package io.github.cafecanudo.soundbridgeduplex.cli;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.util.Key;

import java.nio.file.Path;

public final class CliProcess {

    private final KillableProcessHandler handler;

    private CliProcess(KillableProcessHandler handler) {
        this.handler = handler;
    }

    public static CliProcess start(CliCommand command, Path workDirectory, CliListener listener)
            throws ExecutionException {
        KillableProcessHandler handler = new KillableProcessHandler(command.withJson().toCommandLine(workDirectory));
        handler.setShouldKillProcessSoftly(true);
        handler.addProcessListener(new OutputBridge(listener));
        handler.startNotify();
        return new CliProcess(handler);
    }

    public void cancel() {
        handler.destroyProcess();
    }

    public void kill() {
        handler.killProcess();
    }

    public boolean isRunning() {
        return !handler.isProcessTerminated();
    }

    public boolean waitFor(long timeoutMillis) {
        return handler.waitFor(timeoutMillis);
    }

    private static final class OutputBridge implements ProcessListener {

        private final CliListener listener;
        private final LineAssembler stdout;
        private final LineAssembler stderr;

        private OutputBridge(CliListener listener) {
            this.listener = listener;
            this.stdout = new LineAssembler(this::onStdoutLine);
            this.stderr = new LineAssembler(line -> listener.onText(line, true));
        }

        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
            if (ProcessOutputTypes.STDOUT.equals(outputType)) {
                stdout.append(event.getText());
            } else if (ProcessOutputTypes.STDERR.equals(outputType)) {
                stderr.append(event.getText());
            }
        }

        @Override
        public void processTerminated(ProcessEvent event) {
            stdout.flush();
            stderr.flush();
            listener.onExit(CliExitCode.of(event.getExitCode()), event.getExitCode());
        }

        private void onStdoutLine(String line) {
            CliEventParser.parse(line).ifPresentOrElse(listener::onEvent, () -> listener.onText(line, false));
        }
    }
}
