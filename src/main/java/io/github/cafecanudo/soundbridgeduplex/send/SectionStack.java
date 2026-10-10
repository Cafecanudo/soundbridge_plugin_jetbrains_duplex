package io.github.cafecanudo.soundbridgeduplex.send;

import com.intellij.ui.JBSplitter;
import com.intellij.ui.TitledSeparator;
import com.intellij.util.ui.JBUI;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

final class SectionStack {

    record Section(String key, String title, JComponent content, float proportion) {
    }

    private static final String KEY_PREFIX = "io.github.cafecanudo.soundbridgeduplex.section.";
    private static final int SIDE_PADDING = 8;

    private SectionStack() {
    }

    static JComponent build(List<Section> sections) {
        Section first = sections.get(0);
        JComponent head = titled(first);
        if (sections.size() == 1) {
            return head;
        }
        JBSplitter splitter = new JBSplitter(true, KEY_PREFIX + first.key(), first.proportion());
        splitter.setFirstComponent(head);
        splitter.setSecondComponent(build(sections.subList(1, sections.size())));
        return splitter;
    }

    private static JComponent titled(Section section) {
        JPanel panel = new JPanel(new BorderLayout());
        TitledSeparator title = new TitledSeparator(section.title());
        title.setBorder(JBUI.Borders.empty(0, SIDE_PADDING));
        panel.add(title, BorderLayout.NORTH);
        panel.add(section.content(), BorderLayout.CENTER);
        return panel;
    }
}
