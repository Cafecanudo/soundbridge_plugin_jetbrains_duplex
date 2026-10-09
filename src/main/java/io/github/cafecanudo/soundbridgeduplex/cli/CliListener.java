package io.github.cafecanudo.soundbridgeduplex.cli;

public interface CliListener {

    void onEvent(CliEvent event);

    void onText(String line, boolean error);

    void onExit(CliExitCode exitCode, int rawCode);
}
