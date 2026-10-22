package io.github.cafecanudo.soundbridgeduplex.receive;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.ui.JBColor;
import com.intellij.util.Consumer;
import com.intellij.util.ui.JBUI;
import io.github.cafecanudo.soundbridgeduplex.ToolWindows;
import io.github.cafecanudo.soundbridgeduplex.send.SendProgress;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class ReceiveStatusWidget implements StatusBarWidget, StatusBarWidget.IconPresentation, ReceiveObserver {

    public static final String ID = "SoundBridge.ReceiveStatus";

    private static final int ICON_SIZE = 16;
    private static final int DOT_SIZE = 9;
    private static final Icon STOPPED = new DotIcon(JBColor.GRAY);
    private static final Icon LISTENING = new DotIcon(new JBColor(new Color(0x3FA34D), new Color(0x5FB865)));
    private static final Icon RECEIVING = new DotIcon(new JBColor(new Color(0x2F7BD8), new Color(0x4A90E2)));
    private static final Icon PAUSED = new DotIcon(new JBColor(new Color(0xD9A100), new Color(0xE5B83C)));
    private static final Icon CALIBRATING = new DotIcon(new JBColor(new Color(0x8E5BD0), new Color(0xA67FDB)));

    private final Project project;
    private StatusBar statusBar;

    public ReceiveStatusWidget(Project project) {
        this.project = project;
    }

    @Override
    public String ID() {
        return ID;
    }

    @Override
    public WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public void install(StatusBar bar) {
        statusBar = bar;
        service().addObserver(this);
    }

    @Override
    public void dispose() {
        service().removeObserver(this);
        statusBar = null;
    }

    @Override
    public Icon getIcon() {
        return switch (service().state()) {
            case STOPPED -> STOPPED;
            case LISTENING -> service().isCalibrateOnly() ? CALIBRATING : LISTENING;
            case RECEIVING, SERVING -> RECEIVING;
            case PAUSED -> PAUSED;
            case CALIBRATING -> CALIBRATING;
        };
    }

    @Override
    public String getTooltipText() {
        ReceiveService service = service();
        ReceiveState state = service.state();
        boolean waiting = state == ReceiveState.LISTENING && service.isCalibrateOnly();
        String label = waiting ? message("receive.state.waitingCalibration") : state.label();
        ReceiveOptions options = service.options();
        if (state == ReceiveState.STOPPED || options == null) {
            return message("receive.widget.stopped", label);
        }
        String mode = message(options.bidirectional() ? "history.mode.bi" : "history.mode.uni");
        return message("receive.widget.active", label, mode, options.directory());
    }

    @Override
    public Consumer<MouseEvent> getClickConsumer() {
        return event -> ToolWindows.show(project, ToolWindows.RECEIVE);
    }

    @Override
    public void onLog(String line) {
    }

    @Override
    public void onStateChanged(ReceiveState state) {
        refresh();
    }

    @Override
    public void onProgress(SendProgress progress) {
    }

    @Override
    public void onCalibrationStage(String stage) {
        refresh();
    }

    private void refresh() {
        if (statusBar != null) {
            statusBar.updateWidget(ID);
        }
    }

    private ReceiveService service() {
        return ReceiveService.getInstance(project);
    }

    private static final class DotIcon implements Icon {

        private final Color color;

        private DotIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(color);
                int dot = JBUI.scale(DOT_SIZE);
                int offset = (getIconWidth() - dot) / 2;
                g.fillOval(x + offset, y + offset, dot, dot);
            } finally {
                g.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return JBUI.scale(ICON_SIZE);
        }

        @Override
        public int getIconHeight() {
            return JBUI.scale(ICON_SIZE);
        }
    }
}
