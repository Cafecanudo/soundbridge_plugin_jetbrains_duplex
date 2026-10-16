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
import java.util.function.Consumer;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

final class RequestTask extends Task.Backgroundable {

    private static final long POLL_MILLIS = 250;
    private static final long CANCEL_WAIT_MILLIS = 5000;
    private static final Path WORK_DIRECTORY = Path.of(System.getProperty("user.home"));

    private final String launcher;
    private final List<String> arguments;
    private final Consumer<Boolean> onDone;
    private final AtomicReference<CliExitCode> exit = new AtomicReference<>(CliExitCode.UNKNOWN);
    private volatile boolean receiverPaused;

    RequestTask(Project project, String launcher, List<String> arguments, String name, Consumer<Boolean> onDone) {
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
        }
    }

    @Override
    public void onFinished() {
        if (receiverPaused) {
            ReceiveService.getInstance(getProject()).resumeAfterTransmit();
        }
        onDone.accept(exit.get() == CliExitCode.OK);
    }

    private final class Listener implements CliListener {

        @Override
        public void onEvent(CliEvent event) {
        }

        @Override
        public void onText(String line, boolean error) {
        }

        @Override
        public void onExit(CliExitCode code, int rawCode) {
            exit.set(code);
        }
    }
}
