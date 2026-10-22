package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import io.github.cafecanudo.soundbridgeduplex.ProgressColors;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationObserver;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationProfile;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationService;
import io.github.cafecanudo.soundbridgeduplex.calibration.UniProfile;
import io.github.cafecanudo.soundbridgeduplex.ignore.IgnoreFilter;
import io.github.cafecanudo.soundbridgeduplex.ignore.IgnoreList;
import io.github.cafecanudo.soundbridgeduplex.ignore.IgnoreService;
import io.github.cafecanudo.soundbridgeduplex.memory.ClipboardMemory;
import io.github.cafecanudo.soundbridgeduplex.memory.MemoryFiles;
import io.github.cafecanudo.soundbridgeduplex.cli.CliCommand;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeConfigurable;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifyOutcome;

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
    private static final int EMPTY_ITEM_ROWS = 1;
    private static final int MEMORY_POLL_MILLIS = 1000;
    private static final int COMMAND_ROWS = 3;
    private static final int LOG_ROWS = 6;
    private static final int ITEM_GAP = 6;
    private static final int PROGRESS_SCALE = 1000;
    private static final double PERCENT = 100.0;
    private static final double HZ_PER_KHZ = 1000.0;
    private static final String LINE_BREAK = "\n";
    private static final String PART_SEPARATOR = " · ";

    private final Project project;
    private final DefaultListModel<SendItem> itemsModel = new DefaultListModel<>();
    private final JBList<SendItem> itemsList = new JBList<>(itemsModel);
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
    private final JButton memoryButton = new JButton();
    private final javax.swing.Timer memoryTimer = new javax.swing.Timer(MEMORY_POLL_MILLIS, e -> pollMemory());
    private ClipboardMemory.Probe memoryProbe;
    private final JButton verifyButton = new JButton(message("send.verify"));
    private final JButton cancelButton = new JButton(message("send.cancel"));
    private final JBCheckBox autoSendCheck = new JBCheckBox(message("send.autoSend"));
    private final JBCheckBox replaceCheck = new JBCheckBox(message("send.replace"));
    private final JBCheckBox showWindowCheck = new JBCheckBox(message("send.showWindow"));
    private final JBCheckBox showCommandCheck = new JBCheckBox(message("send.showCommand"));
    private final JBCheckBox showLogCheck = new JBCheckBox(message("send.showLog"));
    private final JBLabel commandLabel = new JBLabel(message("send.command"));
    private final JBLabel logLabel = new JBLabel(message("send.log"));
    private final JProgressBar progressBar = new JProgressBar(0, PROGRESS_SCALE);
    private final JBLabel progressLabel = new JBLabel(message("send.progress.idle"));
    private final JProgressBar totalBar = new JProgressBar(0, PROGRESS_SCALE);
    private final JBLabel totalLabel = new JBLabel(message("send.total.idle"));
    private final JBTextArea logArea = new JBTextArea(LOG_ROWS, 0);
    private final JBScrollPane commandScroll = new JBScrollPane(commandArea);
    private final JBScrollPane logScroll = new JBScrollPane(logArea);
    private final Consumer<SendSelection> draftListener = this::showItems;
    private final Consumer<ResendBatch> resendListener = batch -> {
        draft().consumeResend();
        resend(batch);
    };
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
        progressBar.setForeground(ProgressColors.PASSED);
        totalBar.setForeground(ProgressColors.PASSED);
        modeGroup.add(oneWayRadio);
        modeGroup.add(twoWayRadio);
        configureButton.addActionListener(e -> openSettings());
        oneWayRadio.addActionListener(e -> modeChosen());
        twoWayRadio.addActionListener(e -> modeChosen());
        zipCombo.addActionListener(e -> {
            settings().setZipLevel(options().zipLevel());
            refresh();
        });
        mcsCombo.addActionListener(e -> refresh());
        destinationField.getDocument().addDocumentListener(onChange(this::refresh));
        destinationField.getEmptyText().setText(message("send.destination.root"));
        sendButton.addActionListener(e -> start());
        verifyButton.setToolTipText(message("send.verify.tooltip"));
        verifyButton.addActionListener(e -> verify());
        cancelButton.addActionListener(e -> sendService().cancel());
        autoSendCheck.setSelected(settings().autoSend());
        autoSendCheck.setToolTipText(message("send.autoSend.tooltip"));
        autoSendCheck.addActionListener(e -> settings().setAutoSend(autoSendCheck.isSelected()));
        showCommandCheck.setSelected(settings().showCommand());
        showLogCheck.setSelected(settings().showLog());
        showCommandCheck.addActionListener(e -> {
            settings().setShowCommand(showCommandCheck.isSelected());
            updateVisibility();
        });
        showLogCheck.addActionListener(e -> {
            settings().setShowLog(showLogCheck.isSelected());
            updateVisibility();
        });
        showWindowCheck.setSelected(settings().showWindowOnSend());
        showWindowCheck.setToolTipText(message("send.showWindow.tooltip"));
        showWindowCheck.addActionListener(e -> settings().setShowWindowOnSend(showWindowCheck.isSelected()));
        zipCombo.setSelectedItem(settings().zipLevel() == null ? message("send.zip.none") : settings().zipLevel());
        replaceCheck.setSelected(settings().replaceExisting());
        replaceCheck.setToolTipText(message("send.replace.tooltip"));
        replaceCheck.addActionListener(e -> {
            settings().setReplaceExisting(replaceCheck.isSelected());
            refresh();
        });
        draft().addListener(draftListener);
        draft().addResendListener(resendListener);
        sendService().addObserver(sendObserver);
        calibrationService().addObserver(calibrationObserver);
        root = buildLayout();
        updateVisibility();
        root.addAncestorListener(onShown(this::applySettings));
        root.addAncestorListener(onVisibility(memoryTimer::start, memoryTimer::stop));
        memoryButton.setVisible(false);
        memoryButton.setToolTipText(message("memory.button.tooltip"));
        memoryButton.addActionListener(e -> useMemory());
        itemsList.getEmptyText().setText(message("send.items.emptyHint"));
        sendService().log().forEach(this::appendLog);
        showItems(draft().selection());
        ResendBatch pending = draft().consumeResend();
        if (pending != null) {
            resend(pending);
        }
    }

    JComponent component() {
        return root;
    }

    void dispose() {
        memoryTimer.stop();
        draft().removeListener(draftListener);
        draft().removeResendListener(resendListener);
        sendService().removeObserver(sendObserver);
        calibrationService().removeObserver(calibrationObserver);
    }

    private void showItems(SendSelection chosen) {
        SendSelection selection = SendFiles.prepare(chosen, settings().expandFolders());
        perItemDestination = SendPlan.perItemDestination(destinationBase(), selection, Files::isDirectory);
        itemsModel.clear();
        IgnoreList ignore = ignoreService().list();
        selection.deletions().forEach(path -> itemsModel.addElement(new SendItem(path, true, itemDestination(path, true),
                ignore.isIgnored(path))));
        selection.sends().forEach(path -> itemsModel.addElement(new SendItem(path, false, itemDestination(path, false),
                ignore.isIgnored(path))));
        itemsList.setVisibleRowCount(itemsModel.isEmpty() ? EMPTY_ITEM_ROWS : ITEM_ROWS);
        root.revalidate();
        destinationField.setEnabled(!perItemDestination);
        destinationField.setText(perItemDestination ? message("send.destination.perItem")
                : DestinationResolver.defaultDestination(destinationBase(), selection.sends(), Files::isDirectory));
        applySettings();
        if (draft().consumeVerifyRequest()) {
            verify();
            return;
        }
        if (draft().consumeAutoSendRequest() && settings().autoSend()) {
            autoStart();
        }
    }

    private String itemDestination(Path path, boolean delete) {
        if (!perItemDestination) {
            return null;
        }
        return DestinationResolver.defaultDestination(destinationBase(), List.of(path), delete ? item -> false : Files::isDirectory);
    }

    private void pollMemory() {
        ClipboardMemory.Probe probe = ClipboardMemory.probe(systemClipboard()).orElse(null);
        if (java.util.Objects.equals(probe, memoryProbe)) {
            return;
        }
        memoryProbe = probe;
        memoryButton.setVisible(probe != null);
        if (probe != null) {
            memoryButton.setText(probe.label());
            memoryButton.setMaximumSize(memoryButton.getPreferredSize());
        }
        root.revalidate();
    }

    private void useMemory() {
        try {
            List<Path> items = ClipboardMemory.materialize(systemClipboard(), MemoryFiles.defaultRoot());
            draft().setSelection(SendSelection.of(items));
        } catch (java.io.IOException e) {
            sendService().notifyUser(e.getMessage(), NotificationType.WARNING);
        }
    }

    private static java.awt.datatransfer.Transferable systemClipboard() {
        try {
            return java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private void autoStart() {
        if (request().isEmpty()) {
            sendService().notifyUser(message("send.autoSend.notStarted", message("send.mode.none")), NotificationType.WARNING);
            return;
        }
        start();
    }

    private void applySettings() {
        SoundBridgeProjectSettings settings = settings();
        ProjectConfig config = settings.config();
        Set<SendMode> calibrated = SendMode.calibrated(config, settings.uniProfile(), settings.calibrationProfile());
        enableMode(oneWayRadio, calibrated.contains(SendMode.ONE_WAY));
        enableMode(twoWayRadio, calibrated.contains(SendMode.TWO_WAY));
        modeHint.setText(calibrated.isEmpty() ? message("send.mode.none") : "");
        if (selectedMode().filter(calibrated::contains).isPresent()) {
            refresh();
            return;
        }
        modeGroup.clearSelection();
        SendMode.preferred(config, calibrated, settings.sendMode()).ifPresent(this::selectMode);
        applyModeMcs();
    }

    private void modeChosen() {
        selectedMode().ifPresent(settings()::setSendMode);
        applyModeMcs();
    }

    private void selectMode(SendMode mode) {
        (mode == SendMode.ONE_WAY ? oneWayRadio : twoWayRadio).setSelected(true);
    }

    private void applyModeMcs() {
        selectedMode().ifPresent(mode -> mcsCombo.setSelectedItem(McsSelection.of(parameters(mode).mcs())));
        refresh();
    }

    private void refresh() {
        parametersLabel.setText(selectedMode().map(mode -> describe(parameters(mode)))
                .orElse(message("send.parameters.none")));
        commandArea.setText(request().map(SendPanel::preview)
                .orElse(message(currentSelection().isEmpty() ? "send.items.empty" : "send.mode.none")));
        updateButtons();
    }

    private Optional<SendRequest> request() {
        Optional<SendMode> mode = selectedMode();
        SendSelection selection = currentSelection();
        if (selection.isEmpty() || mode.isEmpty()) {
            return Optional.empty();
        }
        SoundBridgeProjectSettings settings = settings();
        ProjectConfig config = mode.get().applyTo(settings.config());
        String typed = perItemDestination ? "" : destinationField.getText();
        IgnoreFilter.Result filtered = IgnoreFilter.apply(selection, ignoreService().list(), Files::isDirectory);
        SendSelection kept = filtered.kept();
        List<SendJob> jobs = kept.isEmpty() ? List.of()
                : SendPlan.jobs(destinationBase(), kept, Files::isDirectory, typed);
        List<List<String>> commands = SendCommandBuilder.commands(config, settings.uniProfile(),
                settings.calibrationProfile(), options(), jobs);
        SendSnapshot snapshot = SendSnapshot.of(mode.get(), typed, options());
        return Optional.of(new SendRequest(config.launcher(), commands, jobs, QuickSend.labels(kept.sends()),
                QuickSend.labels(kept.deletions()), snapshot, settings.commandDelayMs(),
                QuickSend.labels(filtered.ignoredSends()), QuickSend.labels(filtered.ignoredDeletions())));
    }

    private void start() {
        request().ifPresent(request -> {
            selectedMode().ifPresent(settings()::setSendMode);
            logArea.setText("");
            progressBar.setForeground(ProgressColors.PASSED);
            progressBar.setValue(0);
            progressLabel.setText(message("send.progress.idle"));
            resetTotal();
            sendService().start(request);
        });
    }

    private void verify() {
        Optional<SendMode> mode = selectedMode();
        if (mode.isEmpty() || currentSelection().sends().isEmpty()) {
            sendService().notifyUser(message("verify.notify.notStarted"), NotificationType.WARNING);
            return;
        }
        mode.ifPresent(settings()::setSendMode);
        VerifyRequest request = VerifyRequest.of(settings(), ignoreService().list(), mode.get(), options(),
                currentSelection().sends());
        logArea.setText("");
        progressBar.setForeground(ProgressColors.PASSED);
        progressBar.setValue(0);
        progressLabel.setText(message("send.progress.idle"));
        resetTotal();
        sendService().startVerify(request);
    }

    private void resetTotal() {
        totalBar.setForeground(ProgressColors.PASSED);
        totalBar.setValue(0);
        totalLabel.setText(message("send.total.idle"));
    }

    private void updateVisibility() {
        commandLabel.setVisible(showCommandCheck.isSelected());
        commandScroll.setVisible(showCommandCheck.isSelected());
        logLabel.setVisible(showLogCheck.isSelected());
        logScroll.setVisible(showLogCheck.isSelected());
        root.revalidate();
        root.repaint();
    }

    void resend(ResendBatch batch) {
        List<Path> sends = batch.selection().sends();
        List<Path> existing = sends.stream().filter(Files::exists).toList();
        int missing = sends.size() - existing.size();
        if (missing > 0) {
            sendService().notifyUser(message("history.resend.missing", missing), NotificationType.WARNING);
        }
        draft().setSelection(new SendSelection(existing, batch.selection().deletions()));
        if (batch.snapshot() != null) {
            applySnapshot(batch.snapshot());
        }
        if (settings().autoSend()) {
            autoStart();
        }
    }

    private void applySnapshot(SendSnapshot snapshot) {
        JBRadioButton radio = snapshot.mode() == SendMode.ONE_WAY ? oneWayRadio : twoWayRadio;
        if (radio.isEnabled()) {
            radio.setSelected(true);
            applyModeMcs();
        }
        if (!perItemDestination && !snapshot.destination().isEmpty()) {
            destinationField.setText(snapshot.destination());
        }
        zipCombo.setSelectedItem(snapshot.zipLevel() == null ? message("send.zip.none") : snapshot.zipLevel());
        mcsCombo.setSelectedItem(McsSelection.of(snapshot.mcs()));
        replaceCheck.setSelected(snapshot.replaceExisting());
        settings().setReplaceExisting(snapshot.replaceExisting());
        refresh();
    }

    private void updateButtons() {
        boolean sending = sendService().isRunning();
        boolean calibrating = calibrationService().isRunning();
        sendButton.setEnabled(!sending && !calibrating && selectedMode().isPresent() && !currentSelection().isEmpty());
        verifyButton.setEnabled(!sending && !calibrating && selectedMode().isPresent()
                && !currentSelection().sends().isEmpty());
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
        return new SendOptions(destinationField.getText(), level, (McsSelection) mcsCombo.getSelectedItem(),
                replaceCheck.isSelected());
    }

    private SendSelection currentSelection() {
        List<Path> sends = new ArrayList<>();
        List<Path> deletions = new ArrayList<>();
        for (int i = 0; i < itemsModel.size(); i++) {
            SendItem item = itemsModel.get(i);
            (item.delete() ? deletions : sends).add(item.path());
        }
        return new SendSelection(sends, deletions);
    }

    private Path destinationBase() {
        return settings().destinationBase();
    }

    private void openSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, SoundBridgeConfigurable.class);
        applySettings();
    }

    private static String preview(SendRequest request) {
        if (request.commands().isEmpty()) {
            return message("send.items.allIgnored", request.ignoredCount());
        }
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
                .addComponent(showWindowCheck)
                .addComponentToRightColumn(replaceCheck)
                .addLabeledComponent(message("send.items"), itemsPanel)
                .addComponentToRightColumn(row(memoryButton))
                .addLabeledComponent(message("send.mode"), row(oneWayRadio, twoWayRadio, configureButton))
                .addComponentToRightColumn(modeHint)
                .addLabeledComponent(message("send.parameters"), parametersLabel)
                .addLabeledComponent(message("send.destination"), destinationField)
                .addLabeledComponent(message("send.zip"), row(zipCombo))
                .addLabeledComponent(message("send.mcs"), row(mcsCombo))
                .addComponentToRightColumn(row(showCommandCheck, showLogCheck))
                .addLabeledComponent(commandLabel, commandScroll)
                .addComponentToRightColumn(row(sendButton, verifyButton, cancelButton, autoSendCheck))
                .addLabeledComponent(message("send.progress"), progressBar)
                .addComponentToRightColumn(progressLabel)
                .addLabeledComponent(message("send.total"), totalBar)
                .addComponentToRightColumn(totalLabel)
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

    private static AncestorListener onVisibility(Runnable shown, Runnable hidden) {
        return new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                shown.run();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                hidden.run();
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
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

    private IgnoreService ignoreService() {
        return IgnoreService.getInstance(project);
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
            if (running) {
                logArea.setText("");
                resetTotal();
            } else {
                progressBar.setIndeterminate(false);
            }
            updateButtons();
        }

        @Override
        public void onProgress(SendProgress progress) {
            showProgress(progress);
        }

        @Override
        public void onTotal(SendTotals.Total total) {
            totalBar.setValue((int) Math.round(total.fraction() * PROGRESS_SCALE));
            totalLabel.setText(total.failed() == 0 ? message("send.total.label", total.done(), total.total())
                    : message("send.total.failed", total.done(), total.total(), total.failed()));
        }

        @Override
        public void onVerified(VerifyOutcome outcome) {
            progressBar.setIndeterminate(false);
            if (outcome.succeeded()) {
                progressBar.setValue(PROGRESS_SCALE);
                progressLabel.setText(outcome.summary());
            } else {
                progressBar.setForeground(ProgressColors.FAILED);
            }
        }

        @Override
        public void onFinished(SendOutcome outcome) {
            progressBar.setIndeterminate(false);
            if (outcome.succeeded()) {
                progressBar.setValue(PROGRESS_SCALE);
                totalBar.setValue(PROGRESS_SCALE);
            } else {
                progressBar.setForeground(ProgressColors.FAILED);
                totalBar.setForeground(ProgressColors.FAILED);
            }
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
