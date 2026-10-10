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
import java.util.stream.IntStream;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

final class SendPanel {

    private static final int ITEM_ROWS = 4;
    private static final int COMMAND_ROWS = 3;
    private static final int ITEM_GAP = 6;
    private static final double HZ_PER_KHZ = 1000.0;

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
    private final Consumer<List<Path>> draftListener = items -> showItems(items);
    private final JPanel root;

    SendPanel(Project project) {
        this.project = project;
        zipCombo.setModel(zipModel());
        mcsCombo.setModel(mcsModel());
        commandArea.setEditable(false);
        commandArea.setLineWrap(true);
        sendButton.setEnabled(false);
        sendButton.setToolTipText(message("send.start.pending"));
        clearButton.addActionListener(e -> draft().setItems(List.of()));
        configureButton.addActionListener(e -> openSettings());
        modeGroup.add(oneWayRadio);
        modeGroup.add(twoWayRadio);
        oneWayRadio.addActionListener(e -> applyModeMcs());
        twoWayRadio.addActionListener(e -> applyModeMcs());
        zipCombo.addActionListener(e -> refresh());
        mcsCombo.addActionListener(e -> refresh());
        destinationField.getDocument().addDocumentListener(onChange(this::refresh));
        draft().addListener(draftListener);
        root = buildLayout();
        root.addAncestorListener(onShown(this::applySettings));
        showItems(draft().items());
    }

    JComponent component() {
        return root;
    }

    void dispose() {
        draft().removeListener(draftListener);
    }

    private void showItems(List<Path> items) {
        itemsModel.clear();
        items.forEach(itemsModel::addElement);
        destinationField.setText(DestinationResolver.defaultDestination(projectBase(), items, Files::isDirectory));
        applySettings();
    }

    private Path projectBase() {
        String basePath = project.getBasePath();
        return basePath == null ? null : Path.of(basePath);
    }

    private void applySettings() {
        SoundBridgeProjectSettings settings = SoundBridgeProjectSettings.getInstance(project);
        ProjectConfig config = settings.config();
        Set<SendMode> calibrated = SendMode.calibrated(config, settings.uniProfile(), settings.calibrationProfile());
        enableMode(oneWayRadio, calibrated.contains(SendMode.ONE_WAY));
        enableMode(twoWayRadio, calibrated.contains(SendMode.TWO_WAY));
        Optional<SendMode> preferred = SendMode.preferred(config, calibrated);
        modeGroup.clearSelection();
        preferred.ifPresent(mode -> (mode == SendMode.ONE_WAY ? oneWayRadio : twoWayRadio).setSelected(true));
        modeHint.setText(calibrated.isEmpty() ? message("send.mode.none") : "");
        applyModeMcs();
    }

    private void applyModeMcs() {
        selectedMode().ifPresent(mode -> mcsCombo.setSelectedItem(McsSelection.of(parameters(mode).mcs())));
        refresh();
    }

    private void refresh() {
        Optional<SendMode> mode = selectedMode();
        parametersLabel.setText(mode.map(m -> describe(parameters(m))).orElse(message("send.parameters.none")));
        List<Path> items = currentItems();
        if (items.isEmpty() || mode.isEmpty()) {
            commandArea.setText(message(items.isEmpty() ? "send.items.empty" : "send.mode.none"));
            return;
        }
        SoundBridgeProjectSettings settings = SoundBridgeProjectSettings.getInstance(project);
        ProjectConfig config = mode.get().applyTo(settings.config());
        List<String> arguments = SendCommandBuilder.arguments(config, settings.uniProfile(), settings.calibrationProfile(),
                options(), items);
        commandArea.setText(CommandPreview.format(CliCommand.of(config.launcher(), arguments).withJson().fullCommand()));
    }

    private Optional<SendMode> selectedMode() {
        if (oneWayRadio.isSelected()) {
            return Optional.of(SendMode.ONE_WAY);
        }
        return twoWayRadio.isSelected() ? Optional.of(SendMode.TWO_WAY) : Optional.empty();
    }

    private UniProfile parameters(SendMode mode) {
        SoundBridgeProjectSettings settings = SoundBridgeProjectSettings.getInstance(project);
        return SendCommandBuilder.txParameters(mode.applyTo(settings.config()), settings.uniProfile(),
                settings.calibrationProfile());
    }

    private static void enableMode(JBRadioButton radio, boolean calibrated) {
        radio.setEnabled(calibrated);
        radio.setToolTipText(calibrated ? null : message("send.mode.notCalibrated"));
    }

    private SendOptions options() {
        Object zip = zipCombo.getSelectedItem();
        Integer level = zip instanceof Integer value ? value : null;
        McsSelection mcs = (McsSelection) mcsCombo.getSelectedItem();
        return new SendOptions(destinationField.getText(), level, mcs);
    }

    private List<Path> currentItems() {
        List<Path> items = new ArrayList<>();
        for (int i = 0; i < itemsModel.size(); i++) {
            items.add(itemsModel.get(i));
        }
        return items;
    }

    private void openSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, SoundBridgeConfigurable.class);
        applySettings();
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
        return String.join(" · ", parts);
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
                .addComponentToRightColumn(row(sendButton))
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

    private SendDraft draft() {
        return SendDraft.getInstance(project);
    }
}
