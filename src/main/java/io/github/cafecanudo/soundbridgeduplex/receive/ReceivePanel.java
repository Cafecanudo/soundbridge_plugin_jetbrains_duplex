package io.github.cafecanudo.soundbridgeduplex.receive;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import io.github.cafecanudo.soundbridgeduplex.ProgressColors;
import io.github.cafecanudo.soundbridgeduplex.cli.CliCommand;
import io.github.cafecanudo.soundbridgeduplex.send.CommandPreview;
import io.github.cafecanudo.soundbridgeduplex.send.SendEventReader;
import io.github.cafecanudo.soundbridgeduplex.send.SendProgress;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeConfigurable;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class ReceivePanel {

    private static final int LOG_ROWS = 5;
    private static final int COMMAND_ROWS = 2;
    private static final int ITEM_GAP = 6;
    private static final int PROGRESS_SCALE = 1000;
    private static final double PERCENT = 100.0;
    private static final String PART_SEPARATOR = " · ";
    private static final String LINE_BREAK = "\n";

    private final Project project;
    private final JBRadioButton oneWayRadio = new JBRadioButton(message("send.mode.uni"));
    private final JBRadioButton twoWayRadio = new JBRadioButton(message("send.mode.bi"));
    private final ButtonGroup modeGroup = new ButtonGroup();
    private final JButton configureButton = new JButton(message("send.configure"));
    private final JBLabel devicesLabel = new JBLabel();
    private final JBLabel parametersLabel = new JBLabel();
    private final TextFieldWithBrowseButton directoryField = new TextFieldWithBrowseButton();
    private final JButton startButton = new JButton(message("receive.start"));
    private final JButton stopButton = new JButton(message("receive.stop"));
    private final JButton calibrateButton = new JButton(message("receive.calibrate"));
    private final JBCheckBox serveCheck = new JBCheckBox(message("receive.serve"));
    private final JBLabel calibrationTitle = new JBLabel(message("receive.calibration.title"));
    private final JBLabel calibrationLabel = new JBLabel();
    private final JButton copyButton = new JButton(message("receive.calibration.copy"));
    private final JBLabel stateLabel = new JBLabel();
    private final JProgressBar progressBar = new JProgressBar(0, PROGRESS_SCALE);
    private final JBLabel progressLabel = new JBLabel(message("send.progress.idle"));
    private final JBCheckBox showCommandCheck = new JBCheckBox(message("send.showCommand"));
    private final JBCheckBox showLogCheck = new JBCheckBox(message("send.showLog"));
    private final JBLabel commandLabel = new JBLabel(message("send.command"));
    private final JBTextArea commandArea = new JBTextArea(COMMAND_ROWS, 0);
    private final JBScrollPane commandScroll = new JBScrollPane(commandArea);
    private final JBLabel logLabel = new JBLabel(message("send.log"));
    private final JBTextArea logArea = new JBTextArea(LOG_ROWS, 0);
    private final JBScrollPane logScroll = new JBScrollPane(logArea);
    private final ReceiveObserver observer = new Watcher();
    private final JPanel root;

    public ReceivePanel(Project project) {
        this.project = project;
        commandArea.setEditable(false);
        commandArea.setLineWrap(true);
        logArea.setEditable(false);
        progressBar.setForeground(ProgressColors.PASSED);
        modeGroup.add(oneWayRadio);
        modeGroup.add(twoWayRadio);
        oneWayRadio.addActionListener(e -> modeChanged());
        twoWayRadio.addActionListener(e -> modeChanged());
        configureButton.addActionListener(e -> openSettings());
        directoryField.addActionListener(e -> chooseDirectory());
        directoryField.getTextField().addActionListener(e -> directoryChanged());
        directoryField.getTextField().addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                directoryChanged();
            }
        });
        startButton.addActionListener(e -> service().start(options()));
        serveCheck.setToolTipText(message("receive.serve.tooltip"));
        serveCheck.addActionListener(e -> {
            settings().setServeRequests(serveCheck.isSelected());
            refresh();
        });
        calibrateButton.setToolTipText(message("receive.calibrate.tooltip"));
        calibrateButton.addActionListener(e -> service().startCalibration(options()));
        copyButton.setToolTipText(message("receive.calibration.copy.tooltip"));
        copyButton.addActionListener(e -> copyCalibration());
        stopButton.addActionListener(e -> service().stop());
        showCommandCheck.setSelected(settings().showReceiveCommand());
        showCommandCheck.addActionListener(e -> {
            settings().setShowReceiveCommand(showCommandCheck.isSelected());
            updateVisibility();
        });
        showLogCheck.setSelected(settings().showReceiveLog());
        showLogCheck.addActionListener(e -> {
            settings().setShowReceiveLog(showLogCheck.isSelected());
            updateVisibility();
        });
        service().addObserver(observer);
        service().log().forEach(this::appendLog);
        root = buildLayout();
        root.addAncestorListener(onShown(this::loadSettings));
        loadSettings();
        showState(service().state());
        showCalibration(service().lastCalibration());
        updateVisibility();
    }

    public JComponent component() {
        return root;
    }

    public void dispose() {
        service().removeObserver(observer);
    }

    private void loadSettings() {
        ProjectConfig config = settings().config();
        boolean outputConfigured = !config.output().isEmpty();
        twoWayRadio.setEnabled(outputConfigured);
        twoWayRadio.setToolTipText(outputConfigured ? null : message("receive.mode.noOutput"));
        boolean twoWay = outputConfigured && settings().receiveBidirectional();
        (twoWay ? twoWayRadio : oneWayRadio).setSelected(true);
        serveCheck.setSelected(settings().serveRequests());
        directoryField.setText(config.receiveDirectory());
        refresh();
    }

    private void modeChanged() {
        settings().setReceiveBidirectional(twoWayRadio.isSelected());
        serveCheck.setEnabled(twoWayRadio.isSelected());
        refresh();
    }

    private void directoryChanged() {
        settings().setReceiveDirectory(directoryField.getText());
        refresh();
    }

    private void refresh() {
        ProjectConfig config = settings().config();
        String input = config.input().isEmpty() ? message("receive.device.none") : config.input();
        String output = config.output().isEmpty() ? message("receive.device.none") : config.output();
        devicesLabel.setText(twoWayRadio.isSelected() ? message("receive.devices.bi", input, output)
                : message("receive.devices.uni", input));
        parametersLabel.setText(parameters(config));
        List<String> arguments = ReceiveCommandBuilder.arguments(config, options(), settings().calibrationProfile());
        commandArea.setText(CommandPreview.format(CliCommand.of(config.launcher(), arguments).withJson().fullCommand()));
    }

    private String parameters(ProjectConfig config) {
        if (!twoWayRadio.isSelected()) {
            return message("receive.parameters.uni");
        }
        return ReceiveCommandBuilder.ackPeak(config, settings().calibrationProfile())
                .map(peak -> message("receive.parameters.ackPeak", peak.toString()))
                .orElse(message("receive.parameters.noCalibration"));
    }

    private ReceiveOptions options() {
        return new ReceiveOptions(twoWayRadio.isSelected(), directoryField.getText(),
                twoWayRadio.isSelected() && serveCheck.isSelected());
    }

    private void chooseDirectory() {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        descriptor.setTitle(message("settings.directory.choose"));
        VirtualFile initial = LocalFileSystem.getInstance().findFileByPath(directoryField.getText().strip());
        FileChooser.chooseFile(descriptor, project, root, initial, file -> {
            directoryField.setText(FileUtil.toSystemDependentName(file.getPath()));
            directoryChanged();
        });
    }

    private void openSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, SoundBridgeConfigurable.class);
        loadSettings();
    }

    private void showState(ReceiveState state) {
        boolean waitingCalibration = state == ReceiveState.LISTENING && service().isCalibrateOnly();
        stateLabel.setText(waitingCalibration ? message("receive.state.waitingCalibration") : state.label());
        boolean active = state != ReceiveState.STOPPED;
        startButton.setEnabled(!active);
        calibrateButton.setEnabled(!active);
        stopButton.setEnabled(active);
        oneWayRadio.setEnabled(!active);
        twoWayRadio.setEnabled(!active && !settings().config().output().isEmpty());
        directoryField.setEnabled(!active);
        serveCheck.setEnabled(!active && twoWayRadio.isSelected());
        if (state == ReceiveState.LISTENING || state == ReceiveState.STOPPED) {
            progressBar.setIndeterminate(false);
        }
    }

    private void showProgress(SendProgress progress) {
        progressBar.setIndeterminate(progress.fraction() == null);
        if (progress.fraction() != null) {
            progressBar.setValue((int) Math.round(progress.fraction() * PROGRESS_SCALE));
        }
        List<String> parts = new ArrayList<>();
        parts.add(progress.file());
        if (progress.fraction() != null) {
            parts.add(String.format(Locale.ROOT, "%.0f%%", progress.fraction() * PERCENT));
        }
        if (progress.etaSeconds() != null) {
            parts.add(message("send.progress.eta", SendEventReader.formatTime(progress.etaSeconds())));
        }
        if (!progress.status().isEmpty()) {
            parts.add(progress.status());
        }
        progressLabel.setText(String.join(PART_SEPARATOR, parts));
    }

    private void showCalibrationStage(String stage) {
        progressBar.setIndeterminate(true);
        progressLabel.setText(message("receive.calibration.stage", stage));
    }

    private void showCalibration(ReceivedCalibration calibration) {
        boolean present = calibration != null;
        calibrationTitle.setVisible(present);
        calibrationLabel.setVisible(present);
        copyButton.setVisible(present && calibration.canCopy());
        if (present) {
            progressBar.setIndeterminate(false);
            progressLabel.setText(message("receive.calibration.done"));
            calibrationLabel.setText(ReceiveCalibrationText.html(calibration));
        }
        root.revalidate();
    }

    private void copyCalibration() {
        ReceivedCalibration calibration = service().lastCalibration();
        if (calibration != null && calibration.canCopy()) {
            CopyPasteManager.getInstance().setContents(new StringSelection(calibration.txFlags()));
        }
    }

    private void appendLog(String line) {
        logArea.append(line + LINE_BREAK);
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void updateVisibility() {
        commandLabel.setVisible(showCommandCheck.isSelected());
        commandScroll.setVisible(showCommandCheck.isSelected());
        logLabel.setVisible(showLogCheck.isSelected());
        logScroll.setVisible(showLogCheck.isSelected());
        root.revalidate();
        root.repaint();
    }

    private JPanel buildLayout() {
        JPanel form = FormBuilder.createFormBuilder()
                .addLabeledComponent(message("send.mode"), row(oneWayRadio, twoWayRadio, configureButton))
                .addLabeledComponent(message("receive.devices"), devicesLabel)
                .addLabeledComponent(message("send.parameters"), parametersLabel)
                .addLabeledComponent(message("receive.folder"), directoryField)
                .addComponentToRightColumn(serveCheck)
                .addComponentToRightColumn(row(showCommandCheck, showLogCheck))
                .addLabeledComponent(commandLabel, commandScroll)
                .addComponentToRightColumn(row(startButton, calibrateButton, stopButton))
                .addLabeledComponent(message("receive.state"), stateLabel)
                .addLabeledComponent(message("send.progress"), progressBar)
                .addComponentToRightColumn(progressLabel)
                .addLabeledComponent(calibrationTitle, calibrationLabel)
                .addComponentToRightColumn(row(copyButton))
                .addLabeledComponent(logLabel, logScroll)
                .getPanel();
        form.setBorder(JBUI.Borders.empty(8));
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(form, BorderLayout.NORTH);
        return wrapper;
    }

    private static JPanel row(Component... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                panel.add(Box.createHorizontalStrut(ITEM_GAP));
            }
            if (components[i] instanceof JComponent component && !(component instanceof JBLabel)) {
                component.setMaximumSize(component.getPreferredSize());
            }
            panel.add(components[i]);
        }
        panel.add(Box.createHorizontalGlue());
        return panel;
    }

    private static AncestorListener onShown(Runnable action) {
        return new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                action.run();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }
        };
    }

    private ReceiveService service() {
        return ReceiveService.getInstance(project);
    }

    private SoundBridgeProjectSettings settings() {
        return SoundBridgeProjectSettings.getInstance(project);
    }

    private final class Watcher implements ReceiveObserver {

        @Override
        public void onLog(String line) {
            appendLog(line);
        }

        @Override
        public void onStateChanged(ReceiveState state) {
            showState(state);
        }

        @Override
        public void onProgress(SendProgress progress) {
            showProgress(progress);
        }

        @Override
        public void onCalibrationStage(String stage) {
            showCalibrationStage(stage);
        }

        @Override
        public void onCalibration(ReceivedCalibration calibration) {
            showCalibration(calibration);
        }
    }
}
