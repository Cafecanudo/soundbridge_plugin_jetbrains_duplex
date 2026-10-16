package io.github.cafecanudo.soundbridgeduplex.send;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SendPlanTest {

    private static final Path BASE = Path.of("C:/IdeaProjects/untitled").toAbsolutePath().normalize();
    private static final Path FILE_A = BASE.resolve("src/sss/a.java");
    private static final Path FILE_B = BASE.resolve("src/sss/b.java");
    private static final Path FOLDER = BASE.resolve("src/outro");
    private static final Set<Path> DIRECTORIES = Set.of(FOLDER);

    @Test
    void sameFolderIsOneCommandPerFileWithTypedDestination() {
        List<SendJob> jobs = SendPlan.jobs(BASE, List.of(FILE_A, FILE_B), DIRECTORIES::contains, "custom/dest");

        assertEquals(List.of(new SendJob(List.of(FILE_A), "custom/dest"), new SendJob(List.of(FILE_B), "custom/dest")),
                jobs);
        assertFalse(SendPlan.perItemDestination(BASE, SendSelection.of(List.of(FILE_A, FILE_B)), DIRECTORIES::contains));
    }

    @Test
    void differentFoldersAreOneCommandPerFileWithItsDestination() {
        List<SendJob> jobs = SendPlan.jobs(BASE, List.of(FILE_A, FOLDER, FILE_B), DIRECTORIES::contains, "ignored");

        assertEquals(List.of(new SendJob(List.of(FILE_A), "src/sss"), new SendJob(List.of(FILE_B), "src/sss"),
                new SendJob(List.of(FOLDER), "src/outro")), jobs);
        assertTrue(SendPlan.perItemDestination(BASE, SendSelection.of(List.of(FILE_A, FOLDER)), DIRECTORIES::contains));
    }

    @Test
    void noItemsNoJobs() {
        assertEquals(List.of(), SendPlan.jobs(BASE, List.of(), DIRECTORIES::contains, ""));
    }

    @Test
    void deletionsComeFirstAndForcePerItemDestinations() {
        Path gone = BASE.resolve("lib/old.java");
        SendSelection selection = new SendSelection(List.of(FILE_A), List.of(gone, FILE_B));

        List<SendJob> jobs = SendPlan.jobs(BASE, selection, DIRECTORIES::contains, "ignored");

        assertEquals(List.of(new SendJob(List.of(gone), "lib", true), new SendJob(List.of(FILE_B), "src/sss", true),
                new SendJob(List.of(FILE_A), "src/sss")), jobs);
        assertTrue(SendPlan.perItemDestination(BASE, selection, DIRECTORIES::contains));
        assertFalse(SendPlan.perItemDestination(BASE, SendSelection.of(List.of(FILE_A)), DIRECTORIES::contains));
    }
}
