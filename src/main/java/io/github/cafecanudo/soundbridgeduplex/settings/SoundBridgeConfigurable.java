package io.github.cafecanudo.soundbridgeduplex.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;

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
                || panel.uniError().isPresent();
    }

    @Override
    public void apply() throws ConfigurationException {
        Optional<String> error = panel.uniError();
        if (error.isPresent()) {
            throw new ConfigurationException(error.get());
        }
        settings().update(panel.config());
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

    private SoundBridgeProjectSettings settings() {
        return SoundBridgeProjectSettings.getInstance(project);
    }
}
