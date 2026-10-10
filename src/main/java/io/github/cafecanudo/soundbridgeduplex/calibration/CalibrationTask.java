package io.github.cafecanudo.soundbridgeduplex.calibration;

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
import java.util.concurrent.atomic.AtomicReference;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

final class CalibrationTask extends Task.Backgroundable {

    private static final long POLL_MILLIS = 250;
    private static final String STAGE_EVENT = "calibrate_stage";
    private static final String STAGE = "stage";
    private static final Path WORK_DIRECTORY = Path.of(System.getProperty("user.home"));

    private final CalibrationRequest request;
    private final CalibrationService service;
    private final AtomicReference<CalibrationResult> result = new AtomicReference<>();
    private final AtomicReference<String> failure = new AtomicReference<>();
    private final AtomicReference<CliExitCode> exitCode = new AtomicReference<>();

    CalibrationTask(Project project, CalibrationRequest request, CalibrationService service) {
        super(project, message("calibration.task.title"), true);
        this.request = request;
        this.service = service;
    }

    @Override
    public void run(ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        service.attach(indicator);
        CliCommand command = CliCommand.of(request.launcher(), request.arguments());
        try {
            CliProcess process = CliProcess.start(command, WORK_DIRECTORY, new Listener(indicator));
            while (!process.waitFor(POLL_MILLIS)) {
                if (indicator.isCanceled()) {
                    process.cancel();
                    failure.set(message("calibration.notify.cancelled"));
                    process.waitFor(POLL_MILLIS * 20);
                    return;
                }
            }
        } catch (ExecutionException e) {
            failure.set(e.getMessage());
            service.appendLog(e.getMessage());
        }
    }

    @Override
    public void onFinished() {
        service.finish(request, result.get(), failure.get(), exitCode.get() == CliExitCode.OK);
    }

    private final class Listener implements CliListener {

        private final ProgressIndicator indicator;

        private Listener(ProgressIndicator indicator) {
            this.indicator = indicator;
        }

        @Override
        public void onEvent(CliEvent event) {
            if (STAGE_EVENT.equals(event.name())) {
                event.string(STAGE).ifPresent(indicator::setText2);
            }
            CalibrationResult.from(event).ifPresent(result::set);
            CalibrationLog.lines(event).forEach(service::appendLog);
        }

        @Override
        public void onText(String line, boolean error) {
            if (!line.isBlank()) {
                service.appendLog(line);
            }
        }

        @Override
        public void onExit(CliExitCode code, int rawCode) {
            exitCode.set(code);
            if (code != CliExitCode.OK && failure.get() == null && result.get() == null) {
                failure.set(message("calibration.notify.exit", rawCode));
            }
        }
    }
}
