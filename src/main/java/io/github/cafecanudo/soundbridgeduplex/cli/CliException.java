package io.github.cafecanudo.soundbridgeduplex.cli;

public final class CliException extends Exception {

    public CliException(String message) {
        super(message);
    }

    public CliException(String message, Throwable cause) {
        super(message, cause);
    }
}
