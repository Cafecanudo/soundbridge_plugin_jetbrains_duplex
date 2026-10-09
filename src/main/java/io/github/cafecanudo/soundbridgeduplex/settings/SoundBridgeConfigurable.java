package io.github.cafecanudo.soundbridgeduplex.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;

import javax.swing.JComponent;

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
        return !panel.config().equals(settings().config());
    }

    @Override
    public void apply() {
        settings().update(panel.config());
    }

    @Override
    public void reset() {
        panel.load(settings().config());
    }

    @Override
    public void disposeUIResources() {
        panel = null;
    }

    private SoundBridgeProjectSettings settings() {
        return SoundBridgeProjectSettings.getInstance(project);
    }
}
