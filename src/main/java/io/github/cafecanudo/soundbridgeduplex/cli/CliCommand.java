package io.github.cafecanudo.soundbridgeduplex.cli;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.util.execution.ParametersListUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record CliCommand(List<String> launcher, List<String> arguments) {

    public static final String DEFAULT_LAUNCHER = "python -m soundbridge";
    public static final String JSON_FLAG = "--json";

    private static final Map<String, String> UTF8_ENVIRONMENT = Map.of(
            "PYTHONUTF8", "1",
            "PYTHONIOENCODING", "utf-8");

    public CliCommand {
        if (launcher.isEmpty()) {
            throw new IllegalArgumentException("launcher is empty");
        }
        launcher = List.copyOf(launcher);
        arguments = List.copyOf(arguments);
    }

    public static CliCommand of(String launcher, List<String> arguments) {
        return new CliCommand(ParametersListUtil.parse(launcher.strip()), arguments);
    }

    public CliCommand withJson() {
        if (arguments.contains(JSON_FLAG)) {
            return this;
        }
        List<String> extended = new ArrayList<>(arguments);
        extended.add(JSON_FLAG);
        return new CliCommand(launcher, extended);
    }

    public List<String> fullCommand() {
        List<String> command = new ArrayList<>(launcher);
        command.addAll(arguments);
        return command;
    }

    public GeneralCommandLine toCommandLine(Path workDirectory) {
        return new GeneralCommandLine(fullCommand())
                .withCharset(StandardCharsets.UTF_8)
                .withEnvironment(UTF8_ENVIRONMENT)
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                .withWorkDirectory(workDirectory.toFile());
    }
}
