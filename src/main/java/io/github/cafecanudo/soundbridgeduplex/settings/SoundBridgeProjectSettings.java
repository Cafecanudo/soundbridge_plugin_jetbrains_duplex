package io.github.cafecanudo.soundbridgeduplex.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import io.github.cafecanudo.soundbridgeduplex.cli.CliCommand;

import java.util.Objects;

@Service(Service.Level.PROJECT)
@State(name = "SoundBridgeDuplex", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class SoundBridgeProjectSettings implements PersistentStateComponent<SoundBridgeProjectSettings.SettingsState> {

    public static final class SettingsState {
        public String launcher = "";
        public String output = "";
        public String input = "";
        public String receiveDirectory = "";
    }

    private final Project project;
    private SettingsState state = new SettingsState();

    public SoundBridgeProjectSettings(Project project) {
        this.project = project;
    }

    public static SoundBridgeProjectSettings getInstance(Project project) {
        return project.getService(SoundBridgeProjectSettings.class);
    }

    @Override
    public SettingsState getState() {
        return state;
    }

    @Override
    public void loadState(SettingsState loaded) {
        state = loaded;
    }

    public ProjectConfig config() {
        return new ProjectConfig(
                orDefault(state.launcher, CliCommand.DEFAULT_LAUNCHER),
                state.output,
                state.input,
                orDefault(state.receiveDirectory, projectDirectory()));
    }

    public void update(ProjectConfig config) {
        state.launcher = config.launcher().equals(CliCommand.DEFAULT_LAUNCHER) ? "" : config.launcher();
        state.output = config.output();
        state.input = config.input();
        state.receiveDirectory = isProjectDirectory(config.receiveDirectory()) ? "" : config.receiveDirectory();
    }

    public String projectDirectory() {
        String basePath = project.getBasePath();
        return basePath == null ? "" : FileUtil.toSystemDependentName(basePath);
    }

    private boolean isProjectDirectory(String directory) {
        return FileUtil.pathsEqual(directory, projectDirectory());
    }

    private static String orDefault(String value, String fallback) {
        String text = Objects.requireNonNullElse(value, "").strip();
        return text.isEmpty() ? fallback : text;
    }
}
