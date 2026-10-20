package io.github.cafecanudo.soundbridgeduplex.ignore;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewNodeDecorator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

public final class IgnoredNodeDecorator implements ProjectViewNodeDecorator {

    @Override
    public void decorate(ProjectViewNode<?> node, PresentationData data) {
        Project project = node.getProject();
        VirtualFile file = node.getVirtualFile();
        if (project == null || project.isDisposed() || file == null || !file.isInLocalFileSystem()) {
            return;
        }
        if (IgnoreService.getInstance(project).isIgnored(file.toNioPath())) {
            data.setForcedTextForeground(JBColor.GRAY);
            data.setLocationString(message("ignore.decoration"));
        }
    }
}
