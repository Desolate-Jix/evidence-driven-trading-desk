package io.github.desolatejix.trading.accounting;

import io.github.desolatejix.trading.model.Bbo;
import io.github.desolatejix.trading.model.Execution;
import io.github.desolatejix.trading.model.PositionSnapshot;
import io.github.desolatejix.trading.model.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PositionBookTest {
    @Test
    void buyAddsPositionAndSpendsCash() {
        var book = new PositionBook();
        book.apply(execution("ALPHA", "QUOTER", 2, 600, Side.BUY, 1));

        assertEquals(new PositionSnapshot(2, -1200), book.snapshot("ALPHA"));
    }

    @Test
    void sellReducesPositionAndReceivesCash() {
        var book = new PositionBook();
        book.apply(execution("ALPHA", "QUOTER", 2, 605, Side.SELL, 2));

        assertEquals(new PositionSnapshot(-2, 1210), book.snapshot("ALPHA"));
    }

    @Test
    void differentPriceRoundTripRealizesCash() {
        var book = new PositionBook();
        book.apply(execution("ALPHA", "QUOTER", 2, 600, Side.BUY, 3));
        book.apply(execution("ALPHA", "QUOTER", 2, 605, Side.SELL, 4));

        assertEquals(new PositionSnapshot(0, 10), book.snapshot("ALPHA"));
    }

    @Test
    void feedsRemainIndependent() {
        var book = new PositionBook();
        book.apply(execution("ALPHA", "QUOTER", 2, 600, Side.BUY, 5));
        book.apply(execution("BETA", "QUOTER", 1, 800, Side.SELL, 6));

        assertEquals(new PositionSnapshot(2, -1200), book.snapshot("ALPHA"));
        assertEquals(new PositionSnapshot(-1, 800), book.snapshot("BETA"));
    }

    @Test
    void markToMidWorksForLongAndShortPositions() {
        assertEquals(10, new PositionSnapshot(2, -1190).markToMid(600));
        assertEquals(10, new PositionSnapshot(-2, 1210).markToMid(600));
    }

    @Test
    void exactArithmeticRejectsOverflow() {
        var book = new PositionBook();

        assertThrows(ArithmeticException.class,
                () -> book.apply(execution("ALPHA", "QUOTER", Long.MAX_VALUE, 2, Side.BUY, 7)));
    }

    @Test
    void immutableModelsRejectInvalidFinancialInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> new Bbo(600, 1, 600, 1));
        assertThrows(IllegalArgumentException.class,
                () -> execution("ALPHA", "QUOTER", 0, 600, Side.BUY, 8));
        assertThrows(IllegalArgumentException.class,
                () -> execution("ALPHA", "QUOTER", 1, 0, Side.BUY, 9));
        assertThrows(IllegalArgumentException.class,
                () -> execution("ALPHA", "QUOTER", 1, 600, Side.BUY, 0));
    }

    private static Execution execution(
            String feed,
            String owner,
            long volume,
            long price,
            Side side,
            long matchId
    ) {
        return new Execution(
                feed,
                owner,
                "IN" + matchId,
                "REST" + matchId,
                volume,
                price,
                matchId,
                side,
                "TAKER"
        );
    }
}
