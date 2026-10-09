package io.github.cafecanudo.soundbridgeduplex;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public final class SoundBridgeBundle {

    private static final String BUNDLE = "messages.SoundBridgeBundle";

    private SoundBridgeBundle() {
    }

    public static String message(String key, Object... params) {
        String pattern = ResourceBundle.getBundle(BUNDLE).getString(key);
        return params.length == 0 ? pattern : MessageFormat.format(pattern, params);
    }
}
