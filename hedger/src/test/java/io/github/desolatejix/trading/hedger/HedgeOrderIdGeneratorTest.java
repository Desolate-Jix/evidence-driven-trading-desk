package io.github.desolatejix.trading.hedger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HedgeOrderIdGeneratorTest {
    @Test
    void producesExactlyEightMonotonicDecimalDigits() {
        var ids = new HedgeOrderIdGenerator();
        assertEquals("00000001", ids.next());
        assertEquals("00000002", ids.next());
    }

    @Test
    void allowsLastIdThenFailsRatherThanWrapping() {
        var ids = new HedgeOrderIdGenerator(99_999_999);
        assertEquals("99999999", ids.next());
        assertThrows(IllegalStateException.class, ids::next);
    }

    @Test
    void rejectsOutOfRangeStartingPoint() {
        assertThrows(IllegalArgumentException.class, () -> new HedgeOrderIdGenerator(0));
        assertThrows(IllegalArgumentException.class, () -> new HedgeOrderIdGenerator(100_000_000));
    }
}
