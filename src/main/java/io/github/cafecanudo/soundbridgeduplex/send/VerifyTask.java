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
import io.github.cafecanudo.soundbridgeduplex.verify.VerifyEventReader;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifyOutcome;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

final class VerifyTask extends Task.Backgroundable {

    private static final long POLL_MILLIS = 250;
    private static final long CANCEL_WAIT_MILLIS = 5000;
    private static final double NANOS_PER_SECOND = 1e9;

    private final VerifyRequest request;
    private final SendService service;
    private final List<VerifyEventReader.Batch> batches = new CopyOnWriteArrayList<>();
    private final AtomicReference<String> failure = new AtomicReference<>();
    private volatile boolean cancelled;
    private volatile boolean receiverPaused;
    private long startedAt;

    VerifyTask(Project project, VerifyRequest request, SendService service) {
        super(project, message("verify.task.title"), true);
        this.request = request;
        this.service = service;
    }

    @Override
    public void run(ProgressIndicator indicator) {
        service.attach(indicator);
        receiverPaused = ReceiveService.getInstance(getProject()).pauseForTransmit();
        startedAt = System.nanoTime();
        for (int i = 0; i < request.commands().size(); i++) {
            if (i > 0 && !pause(request.commandDelayMs(), indicator)) {
                cancelled = true;
                return;
            }
            CliExitCode code = runCommand(request.commands().get(i), indicator);
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
        service.finishVerify(request, VerifyOutcome.of(batches, request.commands().size(), request.paths().size(),
                seconds, cancelled, failure.get()));
    }

    private static boolean pause(int millis, ProgressIndicator indicator) {
        long deadline = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < deadline) {
            if (indicator.isCanceled()) {
                return false;
            }
            try {
                Thread.sleep(Math.min(POLL_MILLIS, Math.max(1, deadline - System.currentTimeMillis())));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return !indicator.isCanceled();
    }

    private CliExitCode runCommand(List<String> arguments, ProgressIndicator indicator) {
        AtomicReference<CliExitCode> exit = new AtomicReference<>(CliExitCode.UNKNOWN);
        try {
            CliProcess process = CliProcess.start(CliCommand.of(request.launcher(), arguments), request.workDirectory(),
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
        private final VerifyEventReader reader = new VerifyEventReader();

        private Listener(ProgressIndicator indicator, AtomicReference<CliExitCode> exit) {
            this.indicator = indicator;
            this.exit = exit;
        }

        @Override
        public void onEvent(CliEvent event) {
            VerifyEventReader.Update update = reader.read(event);
            update.log().forEach(service::appendLog);
            if (update.batch() != null) {
                batches.add(update.batch());
            }
            if (update.progress() != null) {
                indicator.setIndeterminate(update.progress().fraction() == null);
                if (update.progress().fraction() != null) {
                    indicator.setFraction(update.progress().fraction());
                }
                service.publishProgress(update.progress());
            }
        }

        @Override
        public void onText(String line, boolean error) {
            if (!line.isBlank()) {
                service.appendLog(line);
            }
        }

        @Override
        public void onExit(CliExitCode code, int rawCode) {
            exit.set(code);
        }
    }
}
