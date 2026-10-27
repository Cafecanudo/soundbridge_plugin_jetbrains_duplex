package io.github.cafecanudo.soundbridgeduplex.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationProfile;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationService;
import io.github.cafecanudo.soundbridgeduplex.calibration.UniProfile;
import io.github.cafecanudo.soundbridgeduplex.receive.ReceiveService;

import javax.swing.JComponent;
import java.util.Optional;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class SoundBridgeConfigurable implements Configurable {

    private final Project project;
    private SoundBridgeSettingsPanel panel;

    public SoundBridgeConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public String getDisplayName() {
        return message("settings.displayName");
    }

    @Override
    public JComponent createComponent() {
        panel = new SoundBridgeSettingsPanel(project, settings().projectDirectory());
        return panel.component();
    }

    @Override
    public boolean isModified() {
        return !panel.config().equals(settings().config())
                || !panel.calibrationOptions().equals(settings().calibrationOptions())
                || !panel.uniProfile().equals(settings().uniProfile())
                || panel.warnUnsent() != settings().warnUnsentOnCommit()
                || panel.osNotifications() != settings().osNotifications()
                || panel.expandFolders() != settings().expandFolders()
                || panel.commandDelayMs() != settings().commandDelayMs()
                || panel.uniError().isPresent()
                || panel.biError().isPresent()
                || biAdjustment().isPresent();
    }

    @Override
    public void apply() throws ConfigurationException {
        Optional<String> error = panel.uniError().or(panel::biError);
        if (error.isPresent()) {
            throw new ConfigurationException(error.get());
        }
        Optional<UniProfile> adjustment = biAdjustment();
        String previousDirectory = settings().config().receiveDirectory();
        settings().update(panel.config());
        String directory = settings().config().receiveDirectory();
        if (!previousDirectory.equals(directory)) {
            ReceiveService.getInstance(project).directoryChanged(directory);
        }
        adjustment.ifPresent(values -> CalibrationService.getInstance(project)
                .adjust(values, settings().config().output(), settings().config().input()));
        settings().updateUniProfile(panel.uniProfile());
        settings().updateCalibrationOptions(panel.calibrationOptions());
        settings().setWarnUnsentOnCommit(panel.warnUnsent());
        settings().setOsNotifications(panel.osNotifications());
        settings().setExpandFolders(panel.expandFolders());
        settings().setCommandDelayMs(panel.commandDelayMs());
    }

    @Override
    public void reset() {
        panel.load(settings().config());
        panel.loadWarnUnsent(settings().warnUnsentOnCommit());
        panel.loadOsNotifications(settings().osNotifications());
        panel.loadSending(settings().expandFolders(), settings().commandDelayMs());
        panel.loadCalibration(settings().calibrationOptions(), settings().calibrationProfile(), settings().uniProfile());
    }

    @Override
    public void disposeUIResources() {
        panel.dispose();
        panel = null;
    }

    private Optional<UniProfile> biAdjustment() {
        UniProfile values = panel.biValues();
        boolean changed = !values.equals(CalibrationProfile.values(settings().calibrationProfile()));
        return panel.config().bidirectional() && changed && values.isComplete() ? Optional.of(values) : Optional.empty();
    }

    private SoundBridgeProjectSettings settings() {
        return SoundBridgeProjectSettings.getInstance(project);
    }
}
