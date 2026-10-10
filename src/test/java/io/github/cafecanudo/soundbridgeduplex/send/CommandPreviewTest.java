package io.github.cafecanudo.soundbridgeduplex.send;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandPreviewTest {

    @Test
    void alwaysQuotesTxValue() {
        List<String> command = List.of("python", "-m", "soundbridge", "--tx", "C:\\p\\YTr.java", "--out", "dq8f", "--json");

        assertEquals("python -m soundbridge --tx \"C:\\p\\YTr.java\" --out dq8f --json", CommandPreview.format(command));
    }

    @Test
    void alwaysQuotesDestValue() {
        List<String> command = List.of("python", "-m", "soundbridge", "--tx", "a.txt", "--dest", "src/sss", "--json");

        assertEquals("python -m soundbridge --tx \"a.txt\" --dest \"src/sss\" --json", CommandPreview.format(command));
    }

    @Test
    void quotesOtherArgumentsOnlyWithSpaces() {
        List<String> command = List.of("C:\\Program Files\\Python\\python.exe", "-m", "soundbridge", "--tx", "a.txt;b.txt",
                "--dest", "fotos 2026");

        assertEquals("\"C:\\Program Files\\Python\\python.exe\" -m soundbridge --tx \"a.txt;b.txt\" --dest \"fotos 2026\"",
                CommandPreview.format(command));
    }
}
