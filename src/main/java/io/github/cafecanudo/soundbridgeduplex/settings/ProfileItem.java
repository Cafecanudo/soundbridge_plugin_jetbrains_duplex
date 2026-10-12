package io.github.cafecanudo.soundbridgeduplex.settings;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

record ProfileItem(String name) {

    static final ProfileItem NONE = new ProfileItem("");

    @Override
    public String toString() {
        return name.isEmpty() ? message("settings.profile.none") : name;
    }
}
