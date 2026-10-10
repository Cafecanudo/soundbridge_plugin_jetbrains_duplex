package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import io.github.cafecanudo.soundbridgeduplex.history.HistoryPanel;
import io.github.cafecanudo.soundbridgeduplex.receive.ReceivePanel;

import javax.swing.JComponent;
import java.util.List;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class SoundBridgeToolWindowFactory implements ToolWindowFactory, DumbAware {

    private static final float SEND_PROPORTION = 0.6f;
    private static final float RECEIVE_PROPORTION = 0.4f;

    public static final String TOOL_WINDOW_ID = "SoundBridge";

    public static void show(Project project) {
        ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (window != null) {
            window.activate(null);
        }
    }

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        SendPanel sendPanel = new SendPanel(project);
        ReceivePanel receivePanel = new ReceivePanel(project);
        HistoryPanel historyPanel = new HistoryPanel(project, sendPanel::resend);
        JComponent sections = SectionStack.build(List.of(
                new SectionStack.Section("send", message("send.section.send"), new JBScrollPane(sendPanel.component()),
                        SEND_PROPORTION),
                new SectionStack.Section("receive", message("receive.section"), new JBScrollPane(receivePanel.component()),
                        RECEIVE_PROPORTION),
                new SectionStack.Section("history", message("history.section"), historyPanel.component(), 1.0f)));
        Content content = ContentFactory.getInstance().createContent(sections, "", false);
        Disposer.register(content, sendPanel::dispose);
        Disposer.register(content, receivePanel::dispose);
        Disposer.register(content, historyPanel::dispose);
        toolWindow.getContentManager().addContent(content);
    }
}
