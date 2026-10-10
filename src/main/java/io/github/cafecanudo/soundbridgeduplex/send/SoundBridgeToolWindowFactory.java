package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import io.github.cafecanudo.soundbridgeduplex.history.HistoryPanel;

public final class SoundBridgeToolWindowFactory implements ToolWindowFactory, DumbAware {

    private static final String SPLITTER_KEY = "io.github.cafecanudo.soundbridgeduplex.sendHistorySplitter";
    private static final float SEND_PROPORTION = 0.7f;

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        SendPanel sendPanel = new SendPanel(project);
        HistoryPanel historyPanel = new HistoryPanel(project, sendPanel::resend);
        JBSplitter splitter = new JBSplitter(true, SPLITTER_KEY, SEND_PROPORTION);
        splitter.setFirstComponent(new JBScrollPane(sendPanel.component()));
        splitter.setSecondComponent(historyPanel.component());
        Content content = ContentFactory.getInstance().createContent(splitter, "", false);
        Disposer.register(content, sendPanel::dispose);
        Disposer.register(content, historyPanel::dispose);
        toolWindow.getContentManager().addContent(content);
    }
}
