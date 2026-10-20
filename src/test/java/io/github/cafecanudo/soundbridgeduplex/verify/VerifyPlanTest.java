package io.github.cafecanudo.soundbridgeduplex.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerifyPlanTest {

    @TempDir
    Path root;

    @Test
    void foldersAreListedRecursivelyRelativeToTheBase() throws IOException {
        Path base = root.resolve("projeto");
        write(base.resolve("src/main/A.java"));
        write(base.resolve("src/main/dao/B.java"));
        write(base.resolve("README.md"));

        VerifyPlan plan = VerifyPlan.of(base, List.of(base.resolve("src"), base.resolve("README.md")));

        assertEquals(List.of("src/main/A.java", "src/main/dao/B.java", "README.md"), plan.paths());
        assertTrue(plan.outside().isEmpty());
    }

    @Test
    void itemsOutsideTheBaseAndRepeatsAreLeftOut() throws IOException {
        Path base = root.resolve("projeto");
        Path inside = write(base.resolve("a.txt"));
        Path outside = write(root.resolve("fora.txt"));

        VerifyPlan plan = VerifyPlan.of(base, List.of(inside, outside, inside, base));

        assertEquals(List.of("a.txt"), plan.paths());
        assertEquals(List.of(outside), plan.outside());
    }

    @Test
    void batchesNeverPassTheLimit() {
        VerifyPlan plan = new VerifyPlan(List.of("aaaa", "bbbb", "cccc", "dddd", "e"), List.of());

        assertEquals(List.of(List.of("aaaa", "bbbb"), List.of("cccc", "dddd"), List.of("e")), plan.batches(9));
        assertEquals(List.of(plan.paths()), plan.batches(VerifyPlan.DEFAULT_BATCH_CHARS));
    }

    @Test
    void aPathLongerThanTheLimitGoesAlone() {
        VerifyPlan plan = new VerifyPlan(List.of("a", "muito-longo", "b"), List.of());

        assertEquals(List.of(List.of("a"), List.of("muito-longo"), List.of("b")), plan.batches(4));
    }

    private static Path write(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        return Files.writeString(file, "x");
    }
}
