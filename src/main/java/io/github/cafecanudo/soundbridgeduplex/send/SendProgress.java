package io.github.cafecanudo.soundbridgeduplex.send;

public record SendProgress(String file, String counter, Double fraction, Double kibPerSecond, Double etaSeconds,
                           String status) {
}
