package io.github.cafecanudo.soundbridgeduplex.send;

import io.github.cafecanudo.soundbridgeduplex.calibration.CalibrationProfile;
import io.github.cafecanudo.soundbridgeduplex.calibration.LinkChoice;
import io.github.cafecanudo.soundbridgeduplex.calibration.UniProfile;
import io.github.cafecanudo.soundbridgeduplex.settings.ProjectConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SendCommandBuilderTest {

    private static final List<Path> ITEMS = List.of(Path.of("C:/p/a.txt"), Path.of("C:/p/docs"));
    private static final String JOINED = Path.of("C:/p/a.txt") + ";" + Path.of("C:/p/docs");
    private static final ProjectConfig UNI = new ProjectConfig("python -m soundbridge", "dq8f", "v2yh", "C:/p", false, "");
    private static final ProjectConfig BI = new ProjectConfig("python -m soundbridge", "dq8f", "v2yh", "C:/p", true, "");
    private static final UniProfile UNI_PROFILE = new UniProfile(true, 7000.0, 0.003, 1);
    private static final LinkChoice BI_CHOICE = new LinkChoice(0.2, false, 22000.0, 9, 41.0, List.of(), true, List.of());

    @Test
    void uniUsesTypedProfileAndOmitsInput() {
        assertEquals(List.of("--tx", JOINED, "--out", "dq8f", "--mono", "--band-high", "7000", "--peak", "0.003", "--mcs", "1"),
                SendCommandBuilder.arguments(UNI, UNI_PROFILE, Optional.empty(), withMcs(1), ITEMS));
    }

    @Test
    void sendsSelectedProfileName() {
        ProjectConfig withProfile = new ProjectConfig("python -m soundbridge", "dq8f", "v2yh", "C:/p", false, "bancada");

        assertEquals(List.of("--tx", JOINED, "--out", "dq8f", "--profile", "bancada"),
                SendCommandBuilder.arguments(withProfile, UniProfile.EMPTY, Optional.empty(), SendOptions.DEFAULT, ITEMS));
    }

    @Test
    void biUsesCalibrationOfSameDevices() {
        Optional<CalibrationProfile> profile = Optional.of(new CalibrationProfile("dq8f", "v2yh", "2026-10-09 20:00", BI_CHOICE));

        assertEquals(List.of("--tx", JOINED, "--out", "dq8f", "--bi", "--in", "v2yh",
                        "--band-high", "22000", "--peak", "0.2", "--mcs", "9"),
                SendCommandBuilder.arguments(BI, UNI_PROFILE, profile, withMcs(9), ITEMS));
    }

    @Test
    void biIgnoresCalibrationOfOtherDevices() {
        Optional<CalibrationProfile> profile = Optional.of(new CalibrationProfile("ggxj", "v2yh", "2026-10-09 20:00", BI_CHOICE));

        assertEquals(List.of("--tx", JOINED, "--out", "dq8f", "--bi", "--in", "v2yh"),
                SendCommandBuilder.arguments(BI, UNI_PROFILE, profile, SendOptions.DEFAULT, ITEMS));
    }

    @Test
    void formOptionsOverrideMcsAndCompression() {
        SendOptions auto = new SendOptions("fotos/2026", null, McsSelection.AUTO, true);
        SendOptions fixed = new SendOptions("", 6, McsSelection.fixed(3), false);

        assertEquals(List.of("--tx", JOINED, "--out", "dq8f", "--mono", "--band-high", "7000", "--peak", "0.003",
                        "--auto", "--dest", "fotos/2026", "--no-zip", "--force-repl"),
                SendCommandBuilder.arguments(UNI, UNI_PROFILE, Optional.empty(), auto, ITEMS));
        assertEquals(List.of("--tx", JOINED, "--out", "dq8f", "--mono", "--band-high", "7000", "--peak", "0.003",
                        "--mcs", "3", "--zip-level", "6"),
                SendCommandBuilder.arguments(UNI, UNI_PROFILE, Optional.empty(), fixed, ITEMS));
    }

    @Test
    void queueMarksAllButLastCommandWithMore() {
        List<SendJob> jobs = List.of(new SendJob(List.of(Path.of("C:/p/src/a.txt")), "src"),
                new SendJob(List.of(Path.of("C:/p/lib")), "lib"));

        List<List<String>> commands = SendCommandBuilder.commands(UNI, UniProfile.EMPTY, Optional.empty(),
                SendOptions.DEFAULT, jobs);

        assertEquals(List.of("--tx", Path.of("C:/p/src/a.txt").toString(), "--out", "dq8f", "--dest", "src", "--more"),
                commands.get(0));
        assertEquals(List.of("--tx", Path.of("C:/p/lib").toString(), "--out", "dq8f", "--dest", "lib"), commands.get(1));
    }

    private static SendOptions withMcs(int value) {
        return new SendOptions("", SendOptions.DEFAULT_ZIP_LEVEL, McsSelection.fixed(value), false);
    }
}
