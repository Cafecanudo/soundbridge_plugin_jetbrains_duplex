package io.github.cafecanudo.soundbridgeduplex.cli;

import java.util.Arrays;

public enum CliExitCode {
    OK(0),
    FAILED(1),
    USAGE(2),
    DEVICE(3),
    INTERRUPTED(130),
    UNKNOWN(Integer.MIN_VALUE);

    private final int code;

    CliExitCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static CliExitCode of(int code) {
        return Arrays.stream(values())
                .filter(value -> value.code == code)
                .findFirst()
                .orElse(UNKNOWN);
    }
}
