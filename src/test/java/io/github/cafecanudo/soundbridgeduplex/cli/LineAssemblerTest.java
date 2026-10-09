package io.github.cafecanudo.soundbridgeduplex.cli;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineAssemblerTest {

    @Test
    void joinsChunksSplitMidLine() {
        List<String> lines = new ArrayList<>();
        LineAssembler assembler = new LineAssembler(lines::add);

        assembler.append("{\"sche");
        assembler.append("ma\": 1}\r\nsecond");
        assembler.append(" line\n");

        assertEquals(List.of("{\"schema\": 1}", "second line"), lines);
    }

    @Test
    void flushEmitsTrailingLineWithoutNewline() {
        List<String> lines = new ArrayList<>();
        LineAssembler assembler = new LineAssembler(lines::add);

        assembler.append("last");
        assembler.flush();
        assembler.flush();

        assertEquals(List.of("last"), lines);
    }

    @Test
    void keepsEmptyLines() {
        List<String> lines = new ArrayList<>();
        LineAssembler assembler = new LineAssembler(lines::add);

        assembler.append("a\n\nb\n");

        assertEquals(List.of("a", "", "b"), lines);
    }
}
