package io.github.cafecanudo.soundbridgeduplex.settings;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationMode;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationObserver;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationOptions;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationProfile;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationRequest;
import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationService;
import io.github.cafecanudo.soundbridgeduplex.calibration.TestSize;
import io.github.cafecanudo.soundbridgeduplex.calibration.UniProfile;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Component;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

final class CalibrationSection implements CalibrationObserver {

    private static final int LOG_ROWS = 10;
    private static final int PEAK_COLUMNS = 8;
    private static final double HZ_PER_KHZ = 1000.0;
    private static final int ITEM_GAP = 6;
    private static final int GROUP_GAP = 16;

    private final Project project;
    private final Supplier<ProjectConfig> currentConfig;
    private final JBCheckBox bidirectionalCheck = new JBCheckBox(message("calibration.bidirectional"));
    private final JBCheckBox uniMonoCheck = new JBCheckBox(message("calibration.uni.mono"));
    private final ComboBox<Object> uniBandCombo = new ComboBox<>();
    private final JBTextField uniPeakField = new JBTextField(PEAK_COLUMNS);
    private final ComboBox<Object> uniMcsCombo = new ComboBox<>();
    private final JButton uniCalibrateButton = new JButton(message("calibration.uni.start"));
    private final Map<TestSize, JBCheckBox> sizeChecks = new EnumMap<>(TestSize.class);
    private final JBCheckBox validateCheck = new JBCheckBox(message("calibration.validate"));
    private final JBCheckBox saveCheck = new JBCheckBox(message("calibration.save"));
    private final JBLabel profileLabel = new JBLabel();
    private final JButton calibrateButton = new JButton(message("calibration.start"));
    private final JButton uniCancelButton = new JButton(message("calibration.cancel"));
    private final JButton cancelButton = new JButton(message("calibration.cancel"));
    private final JBTextArea logArea = new JBTextArea(LOG_ROWS, 0);
    private final JPanel twoWayPanel;
    private final JPanel root;

    private Optional<CalibrationProfile> profile = Optional.empty();
    private Runnable onModeChanged = () -> { };

    CalibrationSection(Project project, Supplier<ProjectConfig> currentConfig) {
        this.project = project;
        this.currentConfig = currentConfig;
        for (TestSize size : TestSize.values()) {
            sizeChecks.put(size, new JBCheckBox(size.label()));
        }
        uniBandCombo.setModel(optionalModel(UniProfile.BANDS_HZ.stream().map(CalibrationSection::bandLabel).toArray()));
        uniMcsCombo.setModel(optionalModel(UniProfile.MCS_VALUES.toArray()));
        keepPreferredSize(uniBandCombo, uniPeakField, uniMcsCombo);
        logArea.setEditable(false);
        bidirectionalCheck.addActionListener(e -> updateMode());
        validateCheck.addActionListener(e -> updateSizesEnabled());
        uniCalibrateButton.addActionListener(e -> start(CalibrationMode.ONE_WAY));
        calibrateButton.addActionListener(e -> start(CalibrationMode.TWO_WAY));
        cancelButton.addActionListener(e -> service().cancel());
        uniCancelButton.addActionListener(e -> service().cancel());
        twoWayPanel = buildTwoWayPanel();
        root = buildLayout();
        service().addObserver(this);
        service().log().forEach(this::onLog);
        onRunningChanged(service().isRunning());
    }

    JComponent component() {
        return root;
    }

    void dispose() {
        service().removeObserver(this);
    }

    void setOnModeChanged(Runnable listener) {
        onModeChanged = listener;
    }

    boolean bidirectional() {
        return bidirectionalCheck.isSelected();
    }

    void setBidirectional(boolean value) {
        bidirectionalCheck.setSelected(value);
        updateMode();
    }

    CalibrationOptions options() {
        Set<TestSize> sizes = EnumSet.noneOf(TestSize.class);
        sizeChecks.forEach((size, check) -> {
            if (check.isSelected()) {
                sizes.add(size);
            }
        });
        return new CalibrationOptions(sizes, validateCheck.isSelected(), saveCheck.isSelected());
    }

    UniProfile uniProfile() {
        return new UniProfile(uniMonoCheck.isSelected(), selectedBand(), validPeak().orElse(null), selectedMcs());
    }

    Optional<String> uniError() {
        try {
            UniProfile.parsePeak(uniPeakField.getText());
            return Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.of(message("calibration.uni.invalidPeak", uniPeakField.getText().strip()));
        }
    }

    void load(CalibrationOptions options, Optional<CalibrationProfile> savedProfile, UniProfile uni) {
        sizeChecks.forEach((size, check) -> check.setSelected(options.sizes().contains(size)));
        validateCheck.setSelected(options.validate());
        saveCheck.setSelected(options.save());
        updateSizesEnabled();
        profile = savedProfile;
        refreshProfileLabel();
        uniMonoCheck.setSelected(uni.mono());
        uniBandCombo.setSelectedItem(uni.bandHighHz() == null ? "" : bandLabel(uni.bandHighHz()));
        uniPeakField.setText(uni.peak() == null ? "" : uni.peak().toString());
        uniMcsCombo.setSelectedItem(uni.mcs() == null ? "" : uni.mcs());
    }

    void refreshProfileLabel() {
        ProjectConfig config = currentConfig.get();
        profileLabel.setText(profile
                .map(p -> describe(p, config))
                .orElse(message("calibration.profile.none")));
    }

    @Override
    public void onLog(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    @Override
    public void onRunningChanged(boolean running) {
        calibrateButton.setEnabled(!running);
        uniCalibrateButton.setEnabled(!running);
        cancelButton.setEnabled(running);
        uniCancelButton.setEnabled(running);
    }

    @Override
    public void onProfileChanged(CalibrationProfile changed) {
        profile = Optional.of(changed);
        refreshProfileLabel();
    }

    private void start(CalibrationMode mode) {
        ProjectConfig config = currentConfig.get();
        logArea.setText("");
        service().start(new CalibrationRequest(config.launcher(), config.output(), config.input(), mode, options()));
    }

    private void updateMode() {
        twoWayPanel.setVisible(bidirectionalCheck.isSelected());
        onModeChanged.run();
    }

    private void updateSizesEnabled() {
        sizeChecks.values().forEach(check -> check.setEnabled(validateCheck.isSelected()));
    }

    private Optional<Double> validPeak() {
        try {
            return UniProfile.parsePeak(uniPeakField.getText());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Double selectedBand() {
        int index = uniBandCombo.getSelectedIndex();
        return index <= 0 ? null : UniProfile.BANDS_HZ.get(index - 1);
    }

    private Integer selectedMcs() {
        Object item = uniMcsCombo.getSelectedItem();
        return item instanceof Integer value ? value : null;
    }

    private static DefaultComboBoxModel<Object> optionalModel(Object[] values) {
        DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
        model.addElement("");
        for (Object value : values) {
            model.addElement(value);
        }
        return model;
    }

    private static String bandLabel(double bandHz) {
        return String.format(Locale.ROOT, "%.0f kHz", bandHz / HZ_PER_KHZ);
    }

    private static String describe(CalibrationProfile profile, ProjectConfig config) {
        String text = message("calibration.profile.current", profile.choice().summary(), profile.calibratedAt());
        if (!profile.matches(config.output(), config.input())) {
            text += " " + message("calibration.profile.otherDevices", profile.output(), profile.input());
        }
        return text;
    }

    private JPanel buildTwoWayPanel() {
        JPanel sizes = row(0, sizeChecks.values().toArray(new Component[0]));
        return FormBuilder.createFormBuilder()
                .addComponent(new TitledSeparator(message("calibration.section.twoWay")))
                .addLabeledComponent(message("calibration.profile"), profileLabel)
                .addComponentToRightColumn(validateCheck)
                .addComponentToRightColumn(sizes)
                .addComponentToRightColumn(saveCheck)
                .addComponentToRightColumn(hint(message("calibration.hint")))
                .addComponentToRightColumn(row(ITEM_GAP, calibrateButton, cancelButton))
                .getPanel();
    }

    private JPanel buildLayout() {
        JPanel uniValues = row(ITEM_GAP, uniMonoCheck, Box.createHorizontalStrut(GROUP_GAP - ITEM_GAP),
                new JBLabel(message("calibration.uni.band")), uniBandCombo, Box.createHorizontalStrut(GROUP_GAP - ITEM_GAP),
                new JBLabel(message("calibration.uni.peak")), uniPeakField, Box.createHorizontalStrut(GROUP_GAP - ITEM_GAP),
                new JBLabel(message("calibration.uni.mcs")), uniMcsCombo);
        return FormBuilder.createFormBuilder()
                .addComponent(bidirectionalCheck)
                .addComponent(new TitledSeparator(message("calibration.section.oneWay")))
                .addLabeledComponent(message("calibration.uni.values"), uniValues)
                .addComponentToRightColumn(hint(message("calibration.uni.hint")))
                .addComponentToRightColumn(row(ITEM_GAP, uniCalibrateButton, uniCancelButton))
                .addComponent(twoWayPanel)
                .addComponent(new JBScrollPane(logArea))
                .getPanel();
    }

    private static JPanel row(int gap, Component... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                panel.add(Box.createHorizontalStrut(gap));
            }
            panel.add(components[i]);
        }
        panel.add(Box.createHorizontalGlue());
        return panel;
    }

    private static void keepPreferredSize(JComponent... components) {
        for (JComponent component : components) {
            component.setMaximumSize(component.getPreferredSize());
        }
    }

    private static JBLabel hint(String text) {
        JBLabel label = new JBLabel(text);
        label.setComponentStyle(UIUtil.ComponentStyle.SMALL);
        label.setFontColor(UIUtil.FontColor.BRIGHTER);
        return label;
    }

    private CalibrationService service() {
        return CalibrationService.getInstance(project);
    }
}
