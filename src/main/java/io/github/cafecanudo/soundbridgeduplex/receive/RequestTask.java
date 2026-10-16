package io.github.cafecanudo.soundbridgeduplex.receive;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import io.github.cafecanudo.soundbridgeduplex.cli.CliCommand;
import io.github.cafecanudo.soundbridgeduplex.cli.CliEvent;
import io.github.cafecanudo.soundbridgeduplex.cli.CliExitCode;
import io.github.cafecanudo.soundbridgeduplex.cli.CliListener;
import io.github.cafecanudo.soundbridgeduplex.cli.CliProcess;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

final class RequestTask extends Task.Backgroundable {

    private static final long POLL_MILLIS = 250;
    private static final long CANCEL_WAIT_MILLIS = 5000;
    private static final Path WORK_DIRECTORY = Path.of(System.getProperty("user.home"));

    private final String launcher;
    private final List<String> arguments;
    private final BiConsumer<Boolean, String> onDone;
    private final AtomicReference<CliExitCode> exit = new AtomicReference<>(CliExitCode.UNKNOWN);
    private final AtomicReference<String> lastLine = new AtomicReference<>("");
    private volatile boolean receiverPaused;

    RequestTask(Project project, String launcher, List<String> arguments, String name,
                BiConsumer<Boolean, String> onDone) {
        super(project, message("request.task.title", name), true);
        this.launcher = launcher;
        this.arguments = List.copyOf(arguments);
        this.onDone = onDone;
    }

    @Override
    public void run(ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        receiverPaused = ReceiveService.getInstance(getProject()).pauseForTransmit();
        try {
            CliProcess process = CliProcess.start(CliCommand.of(launcher, arguments), WORK_DIRECTORY, new Listener());
            while (!process.waitFor(POLL_MILLIS)) {
                if (indicator.isCanceled()) {
                    process.cancel();
                    process.waitFor(CANCEL_WAIT_MILLIS);
                    exit.set(CliExitCode.INTERRUPTED);
                    return;
                }
            }
            process.waitFor(CANCEL_WAIT_MILLIS);
        } catch (ExecutionException e) {
            exit.set(CliExitCode.UNKNOWN);
            lastLine.set(e.getMessage());
        }
    }

    @Override
    public void onFinished() {
        if (receiverPaused) {
            ReceiveService.getInstance(getProject()).resumeAfterTransmit();
        }
        onDone.accept(exit.get() == CliExitCode.OK, lastLine.get());
    }

    private final class Listener implements CliListener {

        @Override
        public void onEvent(CliEvent event) {
            event.string("message").filter(text -> !text.isBlank()).ifPresent(lastLine::set);
        }

        @Override
        public void onText(String line, boolean error) {
            if (!line.isBlank()) {
                lastLine.set(line.strip());
            }
        }

        @Override
        public void onExit(CliExitCode code, int rawCode) {
            exit.set(code);
        }
    }
}
