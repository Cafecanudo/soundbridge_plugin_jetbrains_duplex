package io.github.cafecanudo.soundbridgeduplex.calibration;

public record SizeResult(long size, int passed, int tested) {

    public String label() {
        return TestSize.labelOf(size) + " " + passed + "/" + tested;
    }
}
