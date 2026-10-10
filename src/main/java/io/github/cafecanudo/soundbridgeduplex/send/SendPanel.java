package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationObserver;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationProfile;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationService;
import io.github.cafecanudo.soundbridgeduplex.calibration.UniProfile;
import io.github.cafecanudo.soundbridgeduplex.cli.CliCommand;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeConfigurable;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

final class SendPanel {

    private static final int ITEM_ROWS = 4;
    private static final int COMMAND_ROWS = 3;
    private static final int LOG_ROWS = 6;
    private static final int ITEM_GAP = 6;
    private static final int PROGRESS_SCALE = 1000;
    private static final double PERCENT = 100.0;
    private static final double HZ_PER_KHZ = 1000.0;
    private static final String LINE_BREAK = "\n";
    private static final String PART_SEPARATOR = " · ";

    private final Project project;
    private final DefaultListModel<Path> itemsModel = new DefaultListModel<>();
    private final JBList<Path> itemsList = new JBList<>(itemsModel);
    private final JButton clearButton = new JButton(message("send.items.clear"));
    private final JBRadioButton oneWayRadio = new JBRadioButton(message("send.mode.uni"));
    private final JBRadioButton twoWayRadio = new JBRadioButton(message("send.mode.bi"));
    private final ButtonGroup modeGroup = new ButtonGroup();
    private final JBLabel modeHint = new JBLabel();
    private final JBLabel parametersLabel = new JBLabel();
    private final JButton configureButton = new JButton(message("send.configure"));
    private final JBTextField destinationField = new JBTextField();
    private final ComboBox<Object> zipCombo = new ComboBox<>();
    private final ComboBox<McsSelection> mcsCombo = new ComboBox<>();
    private final JBTextArea commandArea = new JBTextArea(COMMAND_ROWS, 0);
    private final JButton sendButton = new JButton(message("send.start"));
    private final JButton cancelButton = new JButton(message("send.cancel"));
    private final JProgressBar progressBar = new JProgressBar(0, PROGRESS_SCALE);
    private final JBLabel progressLabel = new JBLabel(message("send.progress.idle"));
    private final JBTextArea logArea = new JBTextArea(LOG_ROWS, 0);
    private final Consumer<List<Path>> draftListener = this::showItems;
    private final SendObserver sendObserver = new SendWatcher();
    private final CalibrationObserver calibrationObserver = new CalibrationWatcher();
    private final JPanel root;

    private boolean perItemDestination;

    SendPanel(Project project) {
        this.project = project;
        zipCombo.setModel(zipModel());
        mcsCombo.setModel(mcsModel());
        commandArea.setEditable(false);
        commandArea.setLineWrap(true);
        logArea.setEditable(false);
        modeGroup.add(oneWayRadio);
        modeGroup.add(twoWayRadio);
        clearButton.addActionListener(e -> draft().setItems(List.of()));
        configureButton.addActionListener(e -> openSettings());
        oneWayRadio.addActionListener(e -> applyModeMcs());
        twoWayRadio.addActionListener(e -> applyModeMcs());
        zipCombo.addActionListener(e -> refresh());
        mcsCombo.addActionListener(e -> refresh());
        destinationField.getDocument().addDocumentListener(onChange(this::refresh));
        sendButton.addActionListener(e -> start());
        cancelButton.addActionListener(e -> sendService().cancel());
        draft().addListener(draftListener);
        sendService().addObserver(sendObserver);
        calibrationService().addObserver(calibrationObserver);
        root = buildLayout();
        root.addAncestorListener(onShown(this::applySettings));
        sendService().log().forEach(this::appendLog);
        showItems(draft().items());
    }

    JComponent component() {
        return root;
    }

    void dispose() {
        draft().removeListener(draftListener);
        sendService().removeObserver(sendObserver);
        calibrationService().removeObserver(calibrationObserver);
    }

    private void showItems(List<Path> items) {
        itemsModel.clear();
        items.forEach(itemsModel::addElement);
        perItemDestination = SendPlan.perItem(SendPlan.jobs(projectBase(), items, Files::isDirectory, ""));
        destinationField.setEnabled(!perItemDestination);
        destinationField.setText(perItemDestination ? message("send.destination.perItem")
                : DestinationResolver.defaultDestination(projectBase(), items, Files::isDirectory));
        applySettings();
    }

    private void applySettings() {
        SoundBridgeProjectSettings settings = settings();
        ProjectConfig config = settings.config();
        Set<SendMode> calibrated = SendMode.calibrated(config, settings.uniProfile(), settings.calibrationProfile());
        enableMode(oneWayRadio, calibrated.contains(SendMode.ONE_WAY));
        enableMode(twoWayRadio, calibrated.contains(SendMode.TWO_WAY));
        modeGroup.clearSelection();
        SendMode.preferred(config, calibrated)
                .ifPresent(mode -> (mode == SendMode.ONE_WAY ? oneWayRadio : twoWayRadio).setSelected(true));
        modeHint.setText(calibrated.isEmpty() ? message("send.mode.none") : "");
        applyModeMcs();
    }

    private void applyModeMcs() {
        selectedMode().ifPresent(mode -> mcsCombo.setSelectedItem(McsSelection.of(parameters(mode).mcs())));
        refresh();
    }

    private void refresh() {
        parametersLabel.setText(selectedMode().map(mode -> describe(parameters(mode)))
                .orElse(message("send.parameters.none")));
        commandArea.setText(request().map(SendPanel::preview)
                .orElse(message(currentItems().isEmpty() ? "send.items.empty" : "send.mode.none")));
        updateButtons();
    }

    private Optional<SendRequest> request() {
        Optional<SendMode> mode = selectedMode();
        List<Path> items = currentItems();
        if (items.isEmpty() || mode.isEmpty()) {
            return Optional.empty();
        }
        SoundBridgeProjectSettings settings = settings();
        ProjectConfig config = mode.get().applyTo(settings.config());
        String typed = perItemDestination ? "" : destinationField.getText();
        List<SendJob> jobs = SendPlan.jobs(projectBase(), items, Files::isDirectory, typed);
        List<List<String>> commands = SendCommandBuilder.commands(config, settings.uniProfile(),
                settings.calibrationProfile(), options(), jobs);
        return Optional.of(new SendRequest(config.launcher(), commands, items.stream().map(Path::toString).toList()));
    }

    private void start() {
        request().ifPresent(request -> {
            logArea.setText("");
            progressBar.setValue(0);
            progressLabel.setText(message("send.progress.idle"));
            sendService().start(request);
        });
    }

    private void updateButtons() {
        boolean sending = sendService().isRunning();
        boolean calibrating = calibrationService().isRunning();
        sendButton.setEnabled(!sending && !calibrating && selectedMode().isPresent() && !currentItems().isEmpty());
        cancelButton.setEnabled(sending);
    }

    private void showProgress(SendProgress progress) {
        progressBar.setIndeterminate(progress.fraction() == null);
        if (progress.fraction() != null) {
            progressBar.setValue((int) Math.round(progress.fraction() * PROGRESS_SCALE));
        }
        List<String> parts = new ArrayList<>();
        parts.add(progress.counter().isEmpty() ? progress.file()
                : message("send.progress.counter", progress.counter(), progress.file()));
        if (progress.fraction() != null) {
            parts.add(String.format(Locale.ROOT, "%.0f%%", progress.fraction() * PERCENT));
        }
        if (progress.kibPerSecond() != null) {
            parts.add(String.format(Locale.ROOT, "%.1f KB/s", progress.kibPerSecond()));
        }
        if (progress.etaSeconds() != null) {
            parts.add(message("send.progress.eta", SendEventReader.formatTime(progress.etaSeconds())));
        }
        if (!progress.status().isEmpty()) {
            parts.add(progress.status());
        }
        progressLabel.setText(String.join(PART_SEPARATOR, parts));
    }

    private void appendLog(String line) {
        logArea.append(line + LINE_BREAK);
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private Optional<SendMode> selectedMode() {
        if (oneWayRadio.isSelected()) {
            return Optional.of(SendMode.ONE_WAY);
        }
        return twoWayRadio.isSelected() ? Optional.of(SendMode.TWO_WAY) : Optional.empty();
    }

    private UniProfile parameters(SendMode mode) {
        SoundBridgeProjectSettings settings = settings();
        return SendCommandBuilder.txParameters(mode.applyTo(settings.config()), settings.uniProfile(),
                settings.calibrationProfile());
    }

    private SendOptions options() {
        Object zip = zipCombo.getSelectedItem();
        Integer level = zip instanceof Integer value ? value : null;
        return new SendOptions(destinationField.getText(), level, (McsSelection) mcsCombo.getSelectedItem());
    }

    private List<Path> currentItems() {
        List<Path> items = new ArrayList<>();
        for (int i = 0; i < itemsModel.size(); i++) {
            items.add(itemsModel.get(i));
        }
        return items;
    }

    private Path projectBase() {
        String basePath = project.getBasePath();
        return basePath == null ? null : Path.of(basePath);
    }

    private void openSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, SoundBridgeConfigurable.class);
        applySettings();
    }

    private static String preview(SendRequest request) {
        return request.commands().stream()
                .map(arguments -> CommandPreview.format(CliCommand.of(request.launcher(), arguments).withJson().fullCommand()))
                .collect(Collectors.joining(LINE_BREAK));
    }

    private static void enableMode(JBRadioButton radio, boolean calibrated) {
        radio.setEnabled(calibrated);
        radio.setToolTipText(calibrated ? null : message("send.mode.notCalibrated"));
    }

    private static String describe(UniProfile parameters) {
        if (parameters.bandHighHz() == null && parameters.peak() == null && parameters.mcs() == null) {
            return message("send.parameters.none");
        }
        List<String> parts = new ArrayList<>();
        parts.add(message(parameters.mono() ? "calibration.choice.mono" : "calibration.choice.stereo"));
        if (parameters.bandHighHz() != null) {
            parts.add(String.format(Locale.ROOT, "%.0f kHz", parameters.bandHighHz() / HZ_PER_KHZ));
        }
        if (parameters.peak() != null) {
            parts.add(message("calibration.choice.peak", parameters.peak().toString()));
        }
        if (parameters.mcs() != null) {
            parts.add("MCS " + parameters.mcs());
        }
        return String.join(PART_SEPARATOR, parts);
    }

    private static DefaultComboBoxModel<Object> zipModel() {
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        IntStream.iterate(SendOptions.DEFAULT_ZIP_LEVEL, level -> level >= SendOptions.MIN_ZIP_LEVEL, level -> level - 1)
                .forEach(model::addElement);
        model.addElement(message("send.zip.none"));
        return model;
    }

    private static DefaultComboBoxModel<McsSelection> mcsModel() {
        DefaultComboBoxModel<McsSelection> model = new DefaultComboBoxModel<>();
        model.addElement(McsSelection.AUTO);
        UniProfile.MCS_VALUES.forEach(value -> model.addElement(McsSelection.fixed(value)));
        return model;
    }

    private JPanel buildLayout() {
        JPanel itemsPanel = new JPanel(new BorderLayout());
        itemsList.setVisibleRowCount(ITEM_ROWS);
        itemsPanel.add(new JBScrollPane(itemsList), BorderLayout.CENTER);
        JPanel form = FormBuilder.createFormBuilder()
                .addComponent(new TitledSeparator(message("send.section.send")))
                .addLabeledComponent(message("send.items"), itemsPanel)
                .addComponentToRightColumn(row(clearButton))
                .addLabeledComponent(message("send.mode"), row(oneWayRadio, twoWayRadio, configureButton))
                .addComponentToRightColumn(modeHint)
                .addLabeledComponent(message("send.parameters"), parametersLabel)
                .addLabeledComponent(message("send.destination"), destinationField)
                .addLabeledComponent(message("send.zip"), row(zipCombo))
                .addLabeledComponent(message("send.mcs"), row(mcsCombo))
                .addLabeledComponent(message("send.command"), new JBScrollPane(commandArea))
                .addComponentToRightColumn(row(sendButton, cancelButton))
                .addLabeledComponent(message("send.progress"), progressBar)
                .addComponentToRightColumn(progressLabel)
                .addLabeledComponent(message("send.log"), new JBScrollPane(logArea))
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

    private static DocumentListener onChange(Runnable action) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        };
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

    private SoundBridgeProjectSettings settings() {
        return SoundBridgeProjectSettings.getInstance(project);
    }

    private SendService sendService() {
        return SendService.getInstance(project);
    }

    private CalibrationService calibrationService() {
        return CalibrationService.getInstance(project);
    }

    private SendDraft draft() {
        return SendDraft.getInstance(project);
    }

    private final class SendWatcher implements SendObserver {

        @Override
        public void onLog(String line) {
            appendLog(line);
        }

        @Override
        public void onRunningChanged(boolean running) {
            if (!running) {
                progressBar.setIndeterminate(false);
            }
            updateButtons();
        }

        @Override
        public void onProgress(SendProgress progress) {
            showProgress(progress);
        }
    }

    private final class CalibrationWatcher implements CalibrationObserver {

        @Override
        public void onLog(String line) {
        }

        @Override
        public void onRunningChanged(boolean running) {
            updateButtons();
        }

        @Override
        public void onProfileChanged(CalibrationProfile profile) {
            applySettings();
        }
    }
}
