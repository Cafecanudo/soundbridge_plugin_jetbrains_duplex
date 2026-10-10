package io.github.cafecanudo.soundbridgeduplex.receive;

import com.intellij.openapi.progress.util.ColorProgressBar;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import io.github.cafecanudo.soundbridgeduplex.send.SendEventReader;
import io.github.cafecanudo.soundbridgeduplex.send.SendProgress;
import io.github.cafecanudo.soundbridgeduplex.settings.SoundBridgeProjectSettings;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class ReceivePanel {

    private static final int LOG_ROWS = 5;
    private static final int ITEM_GAP = 6;
    private static final int PROGRESS_SCALE = 1000;
    private static final double PERCENT = 100.0;
    private static final String PART_SEPARATOR = " · ";

    private final Project project;
    private final JButton startButton = new JButton(message("receive.start"));
    private final JButton stopButton = new JButton(message("receive.stop"));
    private final JBLabel stateLabel = new JBLabel();
    private final JBLabel folderLabel = new JBLabel();
    private final JProgressBar progressBar = new JProgressBar(0, PROGRESS_SCALE);
    private final JBLabel progressLabel = new JBLabel(message("send.progress.idle"));
    private final JBCheckBox showLogCheck = new JBCheckBox(message("send.showLog"));
    private final JBTextArea logArea = new JBTextArea(LOG_ROWS, 0);
    private final JBScrollPane logScroll = new JBScrollPane(logArea);
    private final ReceiveObserver observer = new Watcher();
    private final JPanel root;

    public ReceivePanel(Project project) {
        this.project = project;
        logArea.setEditable(false);
        progressBar.setForeground(ColorProgressBar.GREEN);
        startButton.addActionListener(e -> service().start());
        stopButton.addActionListener(e -> service().stop());
        showLogCheck.setSelected(settings().showReceiveLog());
        showLogCheck.addActionListener(e -> {
            settings().setShowReceiveLog(showLogCheck.isSelected());
            updateVisibility();
        });
        service().addObserver(observer);
        service().log().forEach(this::appendLog);
        root = buildLayout();
        showState(service().state());
        updateVisibility();
    }

    public JComponent component() {
        return root;
    }

    public void dispose() {
        service().removeObserver(observer);
    }

    private void showState(ReceiveState state) {
        stateLabel.setText(state.label());
        folderLabel.setText(settings().config().receiveDirectory());
        boolean active = state != ReceiveState.STOPPED;
        startButton.setEnabled(!active);
        stopButton.setEnabled(active);
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

    private void appendLog(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void updateVisibility() {
        logScroll.setVisible(showLogCheck.isSelected());
        root.revalidate();
        root.repaint();
    }

    private JPanel buildLayout() {
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(startButton);
        buttons.add(Box.createHorizontalStrut(ITEM_GAP));
        buttons.add(stopButton);
        buttons.add(Box.createHorizontalStrut(ITEM_GAP));
        buttons.add(showLogCheck);
        buttons.add(Box.createHorizontalGlue());
        JPanel form = FormBuilder.createFormBuilder()
                .addLabeledComponent(message("receive.state"), stateLabel)
                .addLabeledComponent(message("receive.folder"), folderLabel)
                .addComponentToRightColumn(buttons)
                .addLabeledComponent(message("send.progress"), progressBar)
                .addComponentToRightColumn(progressLabel)
                .addComponent(logScroll)
                .getPanel();
        form.setBorder(JBUI.Borders.empty(0, 8, 8, 8));
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(form, BorderLayout.NORTH);
        return wrapper;
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
    }
}
