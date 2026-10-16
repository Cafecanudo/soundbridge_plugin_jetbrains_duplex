package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import io.github.cafecanudo.soundbridgeduplex.cli.CliCommand;
import io.github.cafecanudo.soundbridgeduplex.cli.CliEvent;
import io.github.cafecanudo.soundbridgeduplex.cli.CliExitCode;
import io.github.cafecanudo.soundbridgeduplex.cli.CliListener;
import io.github.cafecanudo.soundbridgeduplex.cli.CliProcess;
import io.github.cafecanudo.soundbridgeduplex.receive.ReceiveService;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

final class SendTask extends Task.Backgroundable {

    private static final long POLL_MILLIS = 250;
    private static final long CANCEL_WAIT_MILLIS = 5000;
    private static final double NANOS_PER_SECOND = 1e9;
    private static final Path WORK_DIRECTORY = Path.of(System.getProperty("user.home"));

    private final SendRequest request;
    private final SendService service;
    private final List<SendFileResult> results = new CopyOnWriteArrayList<>();
    private final List<SentFile> sentFiles = new CopyOnWriteArrayList<>();
    private volatile int currentCommand;
    private final AtomicReference<String> failure = new AtomicReference<>();
    private volatile boolean cancelled;
    private long startedAt;
    private volatile boolean receiverPaused;

    SendTask(Project project, SendRequest request, SendService service) {
        super(project, message("send.task.title"), true);
        this.request = request;
        this.service = service;
    }

    @Override
    public void run(ProgressIndicator indicator) {
        service.attach(indicator);
        receiverPaused = ReceiveService.getInstance(getProject()).pauseForTransmit();
        startedAt = System.nanoTime();
        for (int i = 0; i < request.commands().size(); i++) {
            Map<String, String> fingerprint = i < request.jobs().size()
                    ? SentRegistry.fingerprint(request.jobs().get(i)) : Map.of();
            currentCommand = i;
            CliExitCode code = runCommand(request.commands().get(i), indicator);
            if (code == CliExitCode.OK) {
                SoundBridgeProjectSettings.getInstance(getProject()).recordSent(fingerprint);
            }
            if (cancelled || code != CliExitCode.OK) {
                if (!cancelled && failure.get() == null) {
                    failure.set(message("send.notify.exit", code.code()));
                }
                return;
            }
        }
    }

    @Override
    public void onFinished() {
        if (receiverPaused) {
            ReceiveService.getInstance(getProject()).resumeAfterTransmit();
        }
        double seconds = (System.nanoTime() - startedAt) / NANOS_PER_SECOND;
        service.finish(request, new SendOutcome(results, seconds, cancelled, failure.get()), sentFiles);
    }

    private CliExitCode runCommand(List<String> arguments, ProgressIndicator indicator) {
        AtomicReference<CliExitCode> exit = new AtomicReference<>(CliExitCode.UNKNOWN);
        try {
            CliProcess process = CliProcess.start(CliCommand.of(request.launcher(), arguments), WORK_DIRECTORY,
                    new Listener(indicator, exit));
            while (!process.waitFor(POLL_MILLIS)) {
                if (indicator.isCanceled()) {
                    cancelled = true;
                    process.cancel();
                    process.waitFor(CANCEL_WAIT_MILLIS);
                    return CliExitCode.INTERRUPTED;
                }
            }
            process.waitFor(CANCEL_WAIT_MILLIS);
        } catch (ExecutionException e) {
            failure.set(e.getMessage());
            service.appendLog(e.getMessage());
            return CliExitCode.UNKNOWN;
        }
        return exit.get();
    }

    private final class Listener implements CliListener {

        private final ProgressIndicator indicator;
        private final AtomicReference<CliExitCode> exit;
        private final SendEventReader reader = new SendEventReader();
        private final List<String> fileLog = new ArrayList<>();

        private Listener(ProgressIndicator indicator, AtomicReference<CliExitCode> exit) {
            this.indicator = indicator;
            this.exit = exit;
        }

        @Override
        public void onEvent(CliEvent event) {
            SendEventReader.Update update = reader.read(event);
            update.log().forEach(service::appendLog);
            fileLog.addAll(update.log());
            if (update.result() != null) {
                results.add(update.result());
                sentFiles.add(new SentFile(currentCommand, update.result(), fileLog));
                fileLog.clear();
            }
            if (update.progress() != null) {
                showProgress(update.progress());
            }
        }

        @Override
        public void onText(String line, boolean error) {
            if (!line.isBlank()) {
                service.appendLog(line);
                fileLog.add(line);
            }
        }

        @Override
        public void onExit(CliExitCode code, int rawCode) {
            exit.set(code);
        }

        private void showProgress(SendProgress progress) {
            indicator.setText2(progress.counter().isEmpty() ? progress.file() : progress.counter() + " · " + progress.file());
            indicator.setIndeterminate(progress.fraction() == null);
            if (progress.fraction() != null) {
                indicator.setFraction(progress.fraction());
            }
            service.publishProgress(progress);
        }
    }
}
