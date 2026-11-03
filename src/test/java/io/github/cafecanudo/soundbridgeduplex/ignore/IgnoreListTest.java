package io.github.cafecanudo.soundbridgeduplex.ignore;

import io.github.cafecanudo.soundbridgeduplex.send.SendSelection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IgnoreListTest {

    @TempDir
    Path root;

    @Test
    void folderEntryIgnoresEverythingBelowIt() {
        IgnoreList list = new IgnoreList(root, List.of("build", "src/Secret.java"));

        assertTrue(list.isIgnored(root.resolve("build")));
        assertTrue(list.isIgnored(root.resolve("build/libs/a.jar")));
        assertTrue(list.isIgnored(root.resolve("src/Secret.java")));
        assertFalse(list.isIgnored(root.resolve("src/Main.java")));
        assertFalse(list.isIgnored(root.resolve("buildSrc/x.kt")));
        assertTrue(list.hasIgnoredUnder(root.resolve("src")));
        assertFalse(list.hasIgnoredUnder(root.resolve("build")));
    }

    @Test
    void entriesAreRelativeInsideTheProjectAndSkipWhatIsAlreadyIgnored() {
        IgnoreList list = new IgnoreList(root, List.of("build"));

        List<String> entries = list.with(List.of(root.resolve("src/a.txt"), root.resolve("build/x"),
                root.resolve("src/a.txt")));

        assertEquals(List.of("build", "src/a.txt"), entries);
        assertTrue(new IgnoreList(root, entries).isEntry(root.resolve("src/a.txt")));
        assertEquals(List.of("src/a.txt"), new IgnoreList(root, entries).without(List.of("build")));
    }

    @Test
    void pathsOutsideTheProjectAreStoredAbsolute() {
        Path outside = root.getParent().resolve("fora/a.txt");
        IgnoreList list = new IgnoreList(root.resolve("projeto"), List.of());

        String key = list.key(outside);

        assertTrue(Path.of(key).isAbsolute());
        assertTrue(new IgnoreList(root.resolve("projeto"), List.of(key)).isIgnored(outside));
    }

    @Test
    void filterSplitsSendsAndDeletionsAndOpensFoldersWithIgnoredItems() throws IOException {
        Path src = Files.createDirectories(root.resolve("src"));
        Path main = Files.writeString(src.resolve("Main.java"), "m");
        Files.writeString(src.resolve("Secret.java"), "s");
        Path build = Files.createDirectories(root.resolve("build"));
        IgnoreList list = new IgnoreList(root, List.of("build", "src/Secret.java"));
        SendSelection selection = new SendSelection(List.of(src, build), List.of(root.resolve("build/old.jar"),
                root.resolve("velho.txt")));

        IgnoreFilter.Result result = IgnoreFilter.apply(selection, list, Files::isDirectory);

        assertEquals(List.of(main), result.kept().sends());
        assertEquals(List.of(root.resolve("velho.txt")), result.kept().deletions());
        assertEquals(List.of(src.resolve("Secret.java"), build), result.ignoredSends());
        assertEquals(List.of(root.resolve("build/old.jar")), result.ignoredDeletions());
        assertEquals(3, result.ignoredCount());
    }

    @Test
    void nameGlobMatchesAtAnyDepth() {
        IgnoreList list = new IgnoreList(root, List.of("*.iml"));

        assertTrue(list.isIgnored(root.resolve("app.iml")));
        assertTrue(list.isIgnored(root.resolve("modules/core/core.iml")));
        assertFalse(list.isIgnored(root.resolve("src/iml.txt")));
    }

    @Test
    void globWithSlashMatchesAtAnyLevelAndIgnoresMatchedFolderContents() {
        IgnoreList list = new IgnoreList(root, List.of(".git/*", "src/**/gen"));

        assertTrue(list.isIgnored(root.resolve(".git/HEAD")));
        assertTrue(list.isIgnored(root.resolve(".git/objects/ab/cdef")));
        assertFalse(list.isIgnored(root.resolve(".git")));
        assertTrue(list.isIgnored(root.resolve("sub/.git/HEAD")));
        assertTrue(list.isIgnored(root.resolve("modules/core/.git/objects/x")));
        assertTrue(list.isIgnored(root.resolve("src/gen/A.java")));
        assertTrue(list.isIgnored(root.resolve("src/main/java/gen/B.java")));
        assertTrue(list.isIgnored(root.resolve("app/src/gen/D.java")));
        assertFalse(list.isIgnored(root.resolve("lib/gen/C.java")));
    }

    @Test
    void globsApplyInsideTheProjectOrTheReceiveFolder() {
        Path project = root.resolve("trabalho/projeto");
        Path receive = root.resolve("trabalho");

        assertFalse(new IgnoreList(project, List.of("*.iml")).isIgnored(receive.resolve("outro/app.iml")));
        IgnoreList list = new IgnoreList(project, List.of("*.iml", ".git/*", "/raiz*"), receive);
        assertTrue(list.isIgnored(receive.resolve("outro/app.iml")));
        assertTrue(list.isIgnored(receive.resolve("outro/.git/HEAD")));
        assertTrue(list.isIgnored(receive.resolve("raiz.txt")));
        assertTrue(list.isIgnored(project.resolve("raiz.txt")));
        assertFalse(list.isIgnored(root.resolve("fora/app.iml")));
        assertEquals(List.of(project.toAbsolutePath().normalize()), IgnoreList.patternBases(project, project));
    }

    @Test
    void folderIsOpenedOnlyWhenAGlobMatchesInside() throws IOException {
        Path modules = Files.createDirectories(root.resolve("modules/core"));
        Files.writeString(modules.resolve("core.iml"), "x");
        Path main = Files.writeString(modules.resolve("Main.java"), "m");
        Path clean = Files.createDirectories(root.resolve("clean"));
        Files.writeString(clean.resolve("A.java"), "a");
        IgnoreList list = new IgnoreList(root, List.of("*.iml"));

        assertTrue(list.hasIgnoredUnder(root.resolve("modules")));
        assertFalse(list.hasIgnoredUnder(clean));

        IgnoreFilter.Result result = IgnoreFilter.apply(SendSelection.of(List.of(root.resolve("modules"), clean)),
                list, Files::isDirectory);

        assertEquals(List.of(main, clean), result.kept().sends());
        assertEquals(List.of(modules.resolve("core.iml")), result.ignoredSends());
    }

    @Test
    void manualInputIsSplitNormalizedAndValidated() {
        assertEquals(List.of("*.iml", ".git/*", "build"),
                IgnorePattern.split(" *.iml \n.git\\*, /build/;*.iml"));
        assertEquals(List.of("/out*"), IgnorePattern.split("/out*/"));
        assertTrue(IgnorePattern.problemKey("../x").isPresent());
        assertTrue(IgnorePattern.problemKey("/").isPresent());
        assertFalse(IgnorePattern.problemKey("src/**/gen").isPresent());
    }

    @Test
    void defaultsCoverGitIdeaAndVisualStudioFilesAndKeepExistingEntries() {
        List<String> entries = IgnoreList.withDefaults(List.of("build", "*.iml"));
        IgnoreList list = new IgnoreList(root, entries);

        assertEquals(IgnoreList.DEFAULT_ENTRIES.size() + 1, entries.size());
        assertTrue(entries.contains("build"));
        assertTrue(list.isIgnored(root.resolve(".git/HEAD")));
        assertTrue(list.isIgnored(root.resolve("sub/.git/config")));
        assertTrue(list.isIgnored(root.resolve(".idea/workspace.xml")));
        assertTrue(list.isIgnored(root.resolve("app.iml")));
        assertTrue(list.isIgnored(root.resolve(".vs/App/v17/.suo")));
        assertTrue(list.isIgnored(root.resolve("App.csproj.user")));
        assertFalse(list.isIgnored(root.resolve("src/Main.java")));
        assertFalse(list.isIgnored(root.resolve(".gitignore")));
    }

    @Test
    void leadingSlashAnchorsANameGlobToTheRoot() {
        IgnoreList list = new IgnoreList(root, List.of("/out*"));

        assertTrue(list.isIgnored(root.resolve("out/a.class")));
        assertFalse(list.isIgnored(root.resolve("sub/out/a.class")));
    }
}
