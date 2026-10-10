package io.github.cafecanudo.soundbridgeduplex.send;

public record SendFileResult(String file, boolean ok, double seconds, long size, Integer finalMcs) {
}
