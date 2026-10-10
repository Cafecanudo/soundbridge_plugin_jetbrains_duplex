package io.github.cafecanudo.soundbridgeduplex;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SystemNotifications;

public final class IdeAlerts {

    public static final String NOTIFICATION_GROUP = "SoundBridge";

    private IdeAlerts() {
    }

    public static void notify(Project project, String title, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP)
                .createNotification(title, content, type)
                .notify(project);
        if (!ApplicationManager.getApplication().isActive()) {
            SystemNotifications.getInstance().notify(NOTIFICATION_GROUP, title, content);
        }
    }
}
