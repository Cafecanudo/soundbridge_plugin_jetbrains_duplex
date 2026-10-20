package io.github.cafecanudo.soundbridgeduplex;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import javax.swing.JComponent;

public final class ToolWindows {

    public static final String SEND = "SoundBridge";
    public static final String RECEIVE = "SoundBridge.Receive";
    public static final String HISTORY = "SoundBridge.History";

    private ToolWindows() {
    }

    public static void show(Project project, String id) {
        ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(id);
        if (window != null) {
            window.activate(null);
        }
    }

    public static void install(ToolWindow toolWindow, JComponent component, Disposable disposable) {
        install(toolWindow, component, disposable, "");
    }

    public static void install(ToolWindow toolWindow, JComponent component, Disposable disposable, String title) {
        Content content = ContentFactory.getInstance().createContent(component, title, false);
        Disposer.register(content, disposable);
        toolWindow.getContentManager().addContent(content);
    }
}
