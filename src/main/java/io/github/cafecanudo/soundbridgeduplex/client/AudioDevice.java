package io.github.cafecanudo.soundbridgeduplex.client;

public record AudioDevice(String id, String name, int channels) {

    public static final String LABEL_SEPARATOR = " · ";

    public String label() {
        return id + LABEL_SEPARATOR + name;
    }

    @Override
    public String toString() {
        return label();
    }
}
