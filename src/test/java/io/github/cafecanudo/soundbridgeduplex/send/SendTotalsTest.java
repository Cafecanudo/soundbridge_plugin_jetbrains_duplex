package io.github.cafecanudo.soundbridgeduplex.send;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SendTotalsTest {

    private static final double DELTA = 1e-9;

    @Test
    void fractionWeighsEachItemByItsSize() {
        SendTotals totals = new SendTotals(new long[]{100, 300});

        assertEquals(0.0, totals.at(0, null, 0).fraction(), DELTA);
        assertEquals(0.125, totals.at(0, 0.5, 0).fraction(), DELTA);
        assertEquals(0.25, totals.at(1, null, 0).fraction(), DELTA);
        assertEquals(0.625, totals.at(1, 0.5, 0).fraction(), DELTA);
        assertEquals(1.0, totals.at(2, null, 0).fraction(), DELTA);
    }

    @Test
    void withoutSizesTheCountIsUsedAndDoneIsCapped() {
        SendTotals totals = new SendTotals(new long[]{0, 0, 0, 0});

        SendTotals.Total total = totals.at(1, 0.5, 1);

        assertEquals(0.375, total.fraction(), DELTA);
        assertEquals(1, total.done());
        assertEquals(4, total.total());
        assertEquals(1, total.failed());
        assertEquals(4, totals.at(9, null, 0).done());
    }
}
