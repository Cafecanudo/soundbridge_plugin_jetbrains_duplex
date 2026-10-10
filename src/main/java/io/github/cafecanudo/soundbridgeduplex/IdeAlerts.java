package io.github.cafecanudo.soundbridgeduplex;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SystemNotifications;
import com.intellij.util.concurrency.AppExecutorUtil;

import java.util.concurrent.TimeUnit;

public final class IdeAlerts {

    public static final String NOTIFICATION_GROUP = "SoundBridge";

    private static final long BALLOON_SECONDS = 5;

    private IdeAlerts() {
    }

    public static void notify(Project project, String title, String content, NotificationType type) {
        show(project, createNotification(title, content, type), title, content);
    }

    public static void notify(Project project, String title, String content, NotificationType type,
                              String actionText, Runnable action) {
        Notification notification = createNotification(title, content, type);
        notification.addAction(NotificationAction.createSimpleExpiring(actionText, action));
        show(project, notification, title, content);
    }

    private static Notification createNotification(String title, String content, NotificationType type) {
        return NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP)
                .createNotification(title, content, type);
    }

    private static void show(Project project, Notification notification, String title, String content) {
        notification.notify(project);
        AppExecutorUtil.getAppScheduledExecutorService().schedule(
                () -> ApplicationManager.getApplication().invokeLater(notification::hideBalloon),
                BALLOON_SECONDS, TimeUnit.SECONDS);
        if (!ApplicationManager.getApplication().isActive()) {
            SystemNotifications.getInstance().notify(NOTIFICATION_GROUP, title, content);
        }
    }
}
