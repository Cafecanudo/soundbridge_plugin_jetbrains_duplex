package io.github.cafecanudo.soundbridgeduplex.send;

import io.github.cafecanudo.soundbridgeduplex.calibration.UniProfile;
import io.github.cafecanudo.soundbridgeduplex.ignore.IgnoreList;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;
import io.github.cafecanudo.soundbridgeduplex.verify.VerifyPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VerifyRequestTest {

    private static final ProjectConfig CONFIG = new ProjectConfig("sb", "out", "in", "", false, "");
    private static final SendOptions OPTIONS = new SendOptions("", null, McsSelection.of(null), false);

    @TempDir
    Path root;

    @Test
    void globIgnoredFilesStayOutOfTheVerification() throws IOException {
        Files.createDirectories(root.resolve(".git/objects/ab"));
        Path head = Files.writeString(root.resolve(".git/HEAD"), "h");
        Files.writeString(root.resolve(".git/objects/ab/cd"), "o");
        Files.createDirectories(root.resolve("src"));
        Files.writeString(root.resolve("src/A.java"), "a");
        IgnoreList ignore = new IgnoreList(root, List.of(".git/*"));

        VerifyRequest request = VerifyRequest.of(CONFIG, UniProfile.EMPTY, Optional.empty(), root, ignore,
                SendMode.ONE_WAY, OPTIONS, List.of(root), 0, VerifyPlan.DEFAULT_BATCH_CHARS);

        assertEquals(List.of("src/A.java"), request.paths());
        assertEquals(2, request.ignored().size());
        assertEquals(head, request.ignored().get(0));
    }
}
