package io.github.desolatejix.trading.hedger;

import io.github.desolatejix.trading.model.Execution;
import io.github.desolatejix.trading.model.PositionSnapshot;
import io.github.desolatejix.trading.model.Side;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DeskPositionBookTest {
    @Test
    void keepsIndependentOwnerAndFeedLedgersAndAggregatesExactly() {
        var book = new DeskPositionBook(Set.of("TAKER", "QUOTER", "HEDGER"));
        assertTrue(book.apply(execution("ALPHA", "TAKER", "i-1", "r-1", 2, 600, 1, Side.BUY, "TAKER")));
        assertTrue(book.apply(execution("ALPHA", "QUOTER", "i-2", "r-2", 1, 605, 2, Side.SELL, "MAKER")));
        assertTrue(book.apply(execution("BETA", "TAKER", "i-3", "r-3", 3, 700, 3, Side.SELL, "TAKER")));
        assertEquals(new PositionSnapshot(2, -1200), book.snapshot("TAKER", "ALPHA"));
        assertEquals(new PositionSnapshot(-1, 605), book.snapshot("QUOTER", "ALPHA"));
        assertEquals(new PositionSnapshot(-3, 2100), book.snapshot("TAKER", "BETA"));
        assertEquals(new PositionSnapshot(1, -595), book.aggregate("ALPHA"));
        assertEquals(new PositionSnapshot(-3, 2100), book.aggregate("BETA"));
    }

    @Test
    void ignoresOwnersOutsideTheConfiguredDesk() {
        var book = new DeskPositionBook(Set.of("TAKER"));
        assertFalse(book.apply(execution("ALPHA", "OUTSIDE", "i", "r", 1, 600, 1, Side.BUY, "TAKER")));
        assertEquals(new PositionSnapshot(0, 0), book.aggregate("ALPHA"));
    }

    @Test
    void ignoresTrueRedeliveryButRetainsBothDeskOwnedLegsOfOneMatch() {
        var book = new DeskPositionBook(Set.of("TAKER", "QUOTER"));
        var takerLeg = execution("ALPHA", "TAKER", "incoming", "resting", 2, 603, 42, Side.BUY, "TAKER");
        var makerLeg = execution("ALPHA", "QUOTER", "incoming", "resting", 2, 603, 42, Side.SELL, "MAKER");
        assertTrue(book.apply(takerLeg));
        assertFalse(book.apply(takerLeg));
        assertTrue(book.apply(makerLeg));
        assertEquals(new PositionSnapshot(2, -1206), book.snapshot("TAKER", "ALPHA"));
        assertEquals(new PositionSnapshot(-2, 1206), book.snapshot("QUOTER", "ALPHA"));
        assertEquals(new PositionSnapshot(0, 0), book.aggregate("ALPHA"));
    }

    @Test
    void validatesConfiguredOwnersAndLookupKeys() {
        assertThrows(IllegalArgumentException.class, () -> new DeskPositionBook(Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new DeskPositionBook(Set.of(" ")));
        var book = new DeskPositionBook(Set.of("TAKER"));
        assertThrows(IllegalArgumentException.class, () -> book.snapshot("", "ALPHA"));
        assertThrows(IllegalArgumentException.class, () -> book.aggregate(" "));
    }

    private static Execution execution(String feed, String owner, String incoming, String resting,
                                       long volume, long price, long matchId, Side side, String role) {
        return new Execution(feed, owner, incoming, resting, volume, price, matchId, side, role);
    }
}
