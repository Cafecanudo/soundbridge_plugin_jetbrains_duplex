package io.github.cafecanudo.soundbridgeduplex.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationOptions;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationProfile;
import io.github.cafecanudo.soundbridgeduplex.calibration.UniProfile;
import io.github.cafecanudo.soundbridgeduplex.cli.CliCommand;
import io.github.cafecanudo.soundbridgeduplex.send.SendOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service(Service.Level.PROJECT)
@State(name = "SoundBridgeDuplex", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class SoundBridgeProjectSettings implements PersistentStateComponent<SoundBridgeProjectSettings.SettingsState> {

    public static final class SettingsState {
        public String launcher = "";
        public String output = "";
        public String input = "";
        public String receiveDirectory = "";
        public boolean bidirectional = false;
        public boolean uniMono = false;
        public String uniBandHz = "";
        public String uniPeak = "";
        public String uniMcs = "";
        public String calibrationSizes = CalibrationOptions.encodeSizes(CalibrationOptions.DEFAULT.sizes());
        public boolean calibrationValidate = CalibrationOptions.DEFAULT.validate();
        public boolean calibrationSave = CalibrationOptions.DEFAULT.save();
        public String calibrationProfile = "";
        public boolean autoSend = false;
        public boolean replaceExisting = false;
        public boolean showCommand = false;
        public boolean showLog = false;
        public boolean showWindowOnSend = true;
        public int zipLevel = SendOptions.DEFAULT_ZIP_LEVEL;
        public boolean showHistory = true;
        public List<String> history = new ArrayList<>();
    }

    private static final int NO_ZIP = -1;

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
                orDefault(state.receiveDirectory, projectDirectory()),
                state.bidirectional);
    }

    public void update(ProjectConfig config) {
        state.launcher = config.launcher().equals(CliCommand.DEFAULT_LAUNCHER) ? "" : config.launcher();
        state.output = config.output();
        state.input = config.input();
        state.receiveDirectory = isProjectDirectory(config.receiveDirectory()) ? "" : config.receiveDirectory();
        state.bidirectional = config.bidirectional();
    }

    public UniProfile uniProfile() {
        return new UniProfile(state.uniMono, UniProfile.parseDouble(state.uniBandHz),
                UniProfile.parseDouble(state.uniPeak), UniProfile.parseInteger(state.uniMcs));
    }

    public void updateUniProfile(UniProfile profile) {
        state.uniMono = profile.mono();
        state.uniBandHz = text(profile.bandHighHz());
        state.uniPeak = text(profile.peak());
        state.uniMcs = text(profile.mcs());
    }

    public CalibrationOptions calibrationOptions() {
        return new CalibrationOptions(
                CalibrationOptions.decodeSizes(state.calibrationSizes), state.calibrationValidate, state.calibrationSave);
    }

    public void updateCalibrationOptions(CalibrationOptions options) {
        state.calibrationSizes = CalibrationOptions.encodeSizes(options.sizes());
        state.calibrationValidate = options.validate();
        state.calibrationSave = options.save();
    }

    public Optional<CalibrationProfile> calibrationProfile() {
        return CalibrationProfile.fromJson(state.calibrationProfile);
    }

    public void updateCalibrationProfile(CalibrationProfile profile) {
        state.calibrationProfile = profile.toJson();
    }

    public boolean autoSend() {
        return state.autoSend;
    }

    public void setAutoSend(boolean value) {
        state.autoSend = value;
    }

    public boolean replaceExisting() {
        return state.replaceExisting;
    }

    public void setReplaceExisting(boolean value) {
        state.replaceExisting = value;
    }

    public boolean showCommand() {
        return state.showCommand;
    }

    public void setShowCommand(boolean value) {
        state.showCommand = value;
    }

    public boolean showLog() {
        return state.showLog;
    }

    public void setShowLog(boolean value) {
        state.showLog = value;
    }

    public boolean showWindowOnSend() {
        return state.showWindowOnSend;
    }

    public void setShowWindowOnSend(boolean value) {
        state.showWindowOnSend = value;
    }

    public Integer zipLevel() {
        return state.zipLevel < SendOptions.MIN_ZIP_LEVEL ? null : state.zipLevel;
    }

    public void setZipLevel(Integer level) {
        state.zipLevel = level == null ? NO_ZIP : level;
    }

    public boolean showHistory() {
        return state.showHistory;
    }

    public void setShowHistory(boolean value) {
        state.showHistory = value;
    }

    public List<String> history() {
        return state.history == null ? List.of() : List.copyOf(state.history);
    }

    public void setHistory(List<String> entries) {
        state.history = new ArrayList<>(entries);
    }

    public String projectDirectory() {
        String basePath = project.getBasePath();
        return basePath == null ? "" : FileUtil.toSystemDependentName(basePath);
    }

    private boolean isProjectDirectory(String directory) {
        return FileUtil.pathsEqual(directory, projectDirectory());
    }

    private static String text(Number value) {
        return value == null ? "" : value.toString();
    }

    private static String orDefault(String value, String fallback) {
        String text = Objects.requireNonNullElse(value, "").strip();
        return text.isEmpty() ? fallback : text;
    }
}
