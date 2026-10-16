package io.github.cafecanudo.soundbridgeduplex.send;

import java.nio.file.Path;

public record PathChange(Path before, Path after) {
}
