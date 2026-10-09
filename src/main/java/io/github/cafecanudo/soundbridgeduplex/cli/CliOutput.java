package io.github.cafecanudo.soundbridgeduplex.cli;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record CliOutput(int rawExitCode, String stdout, String stderr) {

    private static final String INFO_EVENT = "info";
    private static final String MESSAGE_KEY = "message";
    private static final String LINE_SEPARATOR = "\n";

    public CliExitCode exitCode() {
        return CliExitCode.of(rawExitCode);
    }

    public boolean isSuccess() {
        return exitCode() == CliExitCode.OK;
    }

    public List<CliEvent> events() {
        return stdout.lines().map(CliEventParser::parse).flatMap(Optional::stream).toList();
    }

    public List<String> messages() {
        return events().stream()
                .filter(event -> INFO_EVENT.equals(event.name()))
                .map(event -> event.string(MESSAGE_KEY))
                .flatMap(Optional::stream)
                .toList();
    }

    public String describeFailure() {
        String text = Stream.concat(messages().stream(), stderr.lines())
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining(LINE_SEPARATOR));
        return text.isEmpty() ? stdout.strip() : text;
    }
}
