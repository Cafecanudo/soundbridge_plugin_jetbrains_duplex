package io.github.cafecanudo.soundbridgeduplex.settings;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationOptions;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationProfile;
import io.github.cafecanudo.soundbridgeduplex.calibration.UniProfile;
import io.github.cafecanudo.soundbridgeduplex.PluginInfo;
import io.github.cafecanudo.soundbridgeduplex.client.DevicesDocument;
import io.github.cafecanudo.soundbridgeduplex.client.LinkProfile;
import io.github.cafecanudo.soundbridgeduplex.client.SoundBridgeClient;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Optional;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

final class SoundBridgeSettingsPanel {

    private static final String HTML_LINE_BREAK = "<br>";

    private static final int COMMAND_DELAY_STEP_MS = 100;

    private final Project project;
    private final String projectDirectory;
    private final JBTextField launcherField = new JBTextField();
    private final JButton testButton = new JButton(message("settings.launcher.test"));
    private final JBLabel versionLabel = new JBLabel();
    private final ComboBox<ProfileItem> profileCombo = new ComboBox<>();
    private final DeviceComboBox outputCombo = new DeviceComboBox();
    private final DeviceComboBox inputCombo = new DeviceComboBox();
    private final JBLabel inputLabel = new JBLabel(message("settings.input"));
    private final TextFieldWithBrowseButton directoryField = new TextFieldWithBrowseButton();
    private final JButton projectDirectoryButton = new JButton(message("settings.directory.project"));
    private final JButton refreshButton = new JButton(message("settings.devices.refresh"));
    private final JBLabel devicesStatus = new JBLabel();
    private final JBLabel errorLabel = new JBLabel();
    private final JBCheckBox warnUnsentCheck = new JBCheckBox(message("settings.warnUnsent"));
    private final JBCheckBox osNotificationsCheck = new JBCheckBox(message("settings.osNotifications"));
    private final JBCheckBox expandFoldersCheck = new JBCheckBox(message("settings.expandFolders"));
    private final JBIntSpinner commandDelaySpinner = new JBIntSpinner(
            SoundBridgeProjectSettings.DEFAULT_COMMAND_DELAY_MS, 0, SoundBridgeProjectSettings.MAX_COMMAND_DELAY_MS,
            COMMAND_DELAY_STEP_MS);
    private final CalibrationSection calibration;
    private final JPanel root;

    private int generation;
    private List<LinkProfile> profiles = List.of();
    private boolean fillingProfiles;

    SoundBridgeSettingsPanel(Project project, String projectDirectory) {
        this.project = project;
        this.projectDirectory = projectDirectory;
        errorLabel.setForeground(JBColor.RED);
        testButton.addActionListener(e -> testLauncher());
        refreshButton.addActionListener(e -> refreshDevices());
        directoryField.addActionListener(e -> chooseDirectory());
        projectDirectoryButton.addActionListener(e -> directoryField.setText(projectDirectory));
        calibration = new CalibrationSection(project, this::config);
        profileCombo.addActionListener(e -> profileSelected());
        outputCombo.addActionListener(e -> calibration.refreshProfileLabel());
        inputCombo.addActionListener(e -> calibration.refreshProfileLabel());
        calibration.setOnModeChanged(this::updateInputVisibility);
        root = buildLayout();
        updateInputVisibility();
    }

    JComponent component() {
        return root;
    }

    void dispose() {
        calibration.dispose();
    }

    CalibrationOptions calibrationOptions() {
        return calibration.options();
    }

    void loadCalibration(CalibrationOptions options, Optional<CalibrationProfile> profile, UniProfile uni) {
        calibration.load(options, profile, uni);
    }

    UniProfile uniProfile() {
        return calibration.uniProfile();
    }

    Optional<String> uniError() {
        return calibration.uniError();
    }

    ProjectConfig config() {
        return new ProjectConfig(launcherField.getText(), outputCombo.spec(), inputCombo.spec(), directoryField.getText(),
                calibration.bidirectional(), selectedProfile());
    }

    boolean warnUnsent() {
        return warnUnsentCheck.isSelected();
    }

    void loadWarnUnsent(boolean value) {
        warnUnsentCheck.setSelected(value);
    }

    boolean expandFolders() {
        return expandFoldersCheck.isSelected();
    }

    int commandDelayMs() {
        return commandDelaySpinner.getNumber();
    }

    void loadSending(boolean expandFolders, int commandDelayMs) {
        expandFoldersCheck.setSelected(expandFolders);
        commandDelaySpinner.setNumber(commandDelayMs);
    }

    boolean osNotifications() {
        return osNotificationsCheck.isSelected();
    }

    void loadOsNotifications(boolean value) {
        osNotificationsCheck.setSelected(value);
    }

    void load(ProjectConfig config) {
        launcherField.setText(config.launcher());
        showProfiles(profiles, config.profile());
        outputCombo.show(List.of(), config.output());
        inputCombo.show(List.of(), config.input());
        directoryField.setText(config.receiveDirectory());
        calibration.setBidirectional(config.bidirectional());
        checkVersion(false);
        refreshDevices();
    }

    private void updateInputVisibility() {
        boolean visible = calibration.bidirectional();
        inputLabel.setVisible(visible);
        inputCombo.setVisible(visible);
    }

    private void refreshDevices() {
        int current = ++generation;
        clearError();
        devicesStatus.setText(message("settings.devices.loading"));
        refreshButton.setEnabled(false);
        SoundBridgeClient client = new SoundBridgeClient(launcherField.getText().strip());
        BackgroundTask.run(client::listDevices,
                document -> {
                    if (current == generation) {
                        showDevices(document);
                    }
                },
                error -> {
                    if (current == generation) {
                        refreshButton.setEnabled(true);
                        devicesStatus.setText("");
                        showError(error);
                    }
                });
    }

    private void testLauncher() {
        clearError();
        checkVersion(true);
    }

    private void checkVersion(boolean reportError) {
        showVersions(message("settings.version.running"));
        testButton.setEnabled(false);
        SoundBridgeClient client = new SoundBridgeClient(launcherField.getText().strip());
        BackgroundTask.run(client::version,
                version -> {
                    testButton.setEnabled(true);
                    showVersions(version);
                },
                error -> {
                    testButton.setEnabled(true);
                    showVersions(message("settings.versions.notFound"));
                    if (reportError) {
                        showError(error);
                    }
                });
    }

    private void showVersions(String script) {
        versionLabel.setText(message("settings.versions.value", PluginInfo.version(), script));
    }

    private void showProfiles(List<LinkProfile> available, String selected) {
        fillingProfiles = true;
        profiles = available;
        DefaultComboBoxModel<ProfileItem> model = new DefaultComboBoxModel<>();
        model.addElement(ProfileItem.NONE);
        available.forEach(profile -> model.addElement(new ProfileItem(profile.name())));
        ProfileItem current = new ProfileItem(selected);
        if (model.getIndexOf(current) < 0) {
            model.addElement(current);
        }
        profileCombo.setModel(model);
        profileCombo.setSelectedItem(current);
        fillingProfiles = false;
    }

    private void profileSelected() {
        if (fillingProfiles) {
            return;
        }
        String name = selectedProfile();
        profiles.stream().filter(profile -> profile.name().equals(name)).findFirst().ifPresent(profile -> {
            if (!profile.input().isEmpty()) {
                inputCombo.select(profile.input());
            }
            if (!profile.output().isEmpty()) {
                outputCombo.select(profile.output());
            }
        });
    }

    private String selectedProfile() {
        return profileCombo.getSelectedItem() instanceof ProfileItem item ? item.name() : "";
    }

    private void showDevices(DevicesDocument document) {
        showProfiles(document.profiles(), selectedProfile());
        outputCombo.show(document.outputs(), outputCombo.spec());
        inputCombo.show(document.inputs(), inputCombo.spec());
        devicesStatus.setText(message("settings.devices.loaded", document.outputs().size(), document.inputs().size()));
        refreshButton.setEnabled(true);
    }

    private void chooseDirectory() {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        descriptor.setTitle(message("settings.directory.choose"));
        VirtualFile initial = LocalFileSystem.getInstance().findFileByPath(directoryField.getText().strip());
        FileChooser.chooseFile(descriptor, project, root, initial,
                file -> directoryField.setText(FileUtil.toSystemDependentName(file.getPath())));
    }

    private void showError(String error) {
        String html = StringUtil.escapeXmlEntities(error).replace("\n", HTML_LINE_BREAK);
        errorLabel.setText("<html>" + html + "</html>");
    }

    private void clearError() {
        errorLabel.setText("");
    }

    private JPanel buildLayout() {
        JPanel form = FormBuilder.createFormBuilder()
                .addLabeledComponent(message("settings.versions"), versionLabel)
                .addSeparator()
                .addLabeledComponent(message("settings.launcher"), row(launcherField, testButton))
                .addComponentToRightColumn(hint(message("settings.launcher.hint")))
                .addSeparator()
                .addLabeledComponent(message("settings.profile"), profileCombo)
                .addComponentToRightColumn(hint(message("settings.profile.hint")))
                .addLabeledComponent(inputLabel, inputCombo)
                .addLabeledComponent(message("settings.output"), outputCombo)
                .addComponentToRightColumn(hint(message("settings.devices.hint")))
                .addComponentToRightColumn(row(devicesStatus, refreshButton))
                .addSeparator()
                .addLabeledComponent(message("settings.directory"), row(directoryField, projectDirectoryButton))
                .addComponentToRightColumn(hint(message("settings.directory.hint")))
                .addComponent(errorLabel)
                .addComponent(warnUnsentCheck)
                .addComponent(hint(message("settings.warnUnsent.hint")))
                .addComponent(osNotificationsCheck)
                .addComponent(hint(message("settings.osNotifications.hint")))
                .addSeparator()
                .addComponent(expandFoldersCheck)
                .addComponent(hint(message("settings.expandFolders.hint")))
                .addLabeledComponent(message("settings.commandDelay"), commandDelaySpinner)
                .addComponentToRightColumn(hint(message("settings.commandDelay.hint")))
                .addSeparator()
                .addComponent(calibration.component())
                .getPanel();

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(form, BorderLayout.NORTH);
        return wrapper;
    }

    private static JBLabel hint(String text) {
        JBLabel label = new JBLabel(text);
        label.setComponentStyle(UIUtil.ComponentStyle.SMALL);
        label.setFontColor(UIUtil.FontColor.BRIGHTER);
        return label;
    }

    private static JPanel row(JComponent main, JComponent trailing) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(main);
        panel.add(trailing);
        return panel;
    }
}
