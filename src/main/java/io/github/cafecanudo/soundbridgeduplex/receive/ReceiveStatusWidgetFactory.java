package io.github.cafecanudo.soundbridgeduplex.receive;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class ReceiveStatusWidgetFactory implements StatusBarWidgetFactory {

    @Override
    public String getId() {
        return ReceiveStatusWidget.ID;
    }

    @Override
    public String getDisplayName() {
        return message("receive.widget.name");
    }

    @Override
    public boolean isAvailable(Project project) {
        return true;
    }

    @Override
    public StatusBarWidget createWidget(Project project) {
        return new ReceiveStatusWidget(project);
    }

    @Override
    public void disposeWidget(StatusBarWidget widget) {
        widget.dispose();
    }

    @Override
    public boolean canBeEnabledOn(StatusBar statusBar) {
        return true;
    }
}
