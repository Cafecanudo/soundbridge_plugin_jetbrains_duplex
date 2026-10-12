package io.github.cafecanudo.soundbridgeduplex;

import com.intellij.ui.JBColor;

import java.awt.Color;

public final class ProgressColors {

    public static final Color PASSED = JBColor.namedColor("ProgressBar.passedColor", new JBColor(0x6CAD74, 0x4A8C53));
    public static final Color FAILED = JBColor.namedColor("ProgressBar.failedColor", new JBColor(0xD67B76, 0xE55757));

    private ProgressColors() {
    }
}
