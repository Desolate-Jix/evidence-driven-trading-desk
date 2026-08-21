package io.github.desolatejix.trading.hedger;

import io.github.desolatejix.trading.model.Execution;
import io.github.desolatejix.trading.model.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionDeduplicatorTest {
    @Test
    void exactNotificationIdentityIncludesEveryFinancialAndOwnershipField() {
        var deduplicator = new ExecutionDeduplicator();
        var original = execution("ALPHA", "TAKER", "i", "r", 2, 603, 42, Side.BUY, "TAKER");
        assertTrue(deduplicator.firstDelivery(original));
        assertFalse(deduplicator.firstDelivery(original));
        assertTrue(deduplicator.firstDelivery(execution("ALPHA", "QUOTER", "i", "r", 2, 603, 42, Side.SELL, "MAKER")));
        assertTrue(deduplicator.firstDelivery(execution("ALPHA", "TAKER", "i", "r", 1, 603, 42, Side.BUY, "TAKER")));
        assertEquals(3, deduplicator.uniqueCount());
    }

    @Test
    void executionKeyRoundTripsAllFields() {
        var execution = execution("ALPHA", "DESK_A", "i", "r", 2, 603, 42, Side.BUY, "TAKER");
        assertEquals(new ExecutionKey("ALPHA", "TAKER", "DESK_A", "i", "r", 2, 603, 42, Side.BUY),
                ExecutionKey.from(execution));
    }

    private static Execution execution(String feed, String owner, String incoming, String resting,
                                       long volume, long price, long matchId, Side side, String role) {
        return new Execution(feed, owner, incoming, resting, volume, price, matchId, side, role);
    }
}
