package io.github.desolatejix.trading.quoter;

import io.github.desolatejix.trading.model.Bbo;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QuoteEngineTest {
    private final QuoteEngine engine = new QuoteEngine();

    @Test
    void quotesOneTickAroundMidpointWithoutCrossing() {
        var decision = engine.decide(new Bbo(600, 5, 610, 5), 0, 1, 2);

        assertEquals(OptionalLong.of(604), decision.bidPrice());
        assertEquals(OptionalLong.of(606), decision.askPrice());
        assertEquals(1, decision.size());
        assertTrue(decision.bidPrice().orElseThrow() < 610);
        assertTrue(decision.askPrice().orElseThrow() > 600);
        assertTrue(decision.bidPrice().orElseThrow() < decision.askPrice().orElseThrow());
    }

    @Test
    void roundsOutwardToTheConfiguredTick() {
        var decision = engine.decide(new Bbo(600, 5, 611, 5), 0, 2, 3);

        assertEquals(OptionalLong.of(602), decision.bidPrice());
        assertEquals(OptionalLong.of(608), decision.askPrice());
        assertEquals(0, decision.bidPrice().orElseThrow() % 2);
        assertEquals(0, decision.askPrice().orElseThrow() % 2);
    }

    @Test
    void clampsQuotesToRemainPassiveInANarrowMarket() {
        var decision = engine.decide(new Bbo(600, 5, 601, 5), 0, 1, 2);

        assertEquals(OptionalLong.of(599), decision.bidPrice());
        assertEquals(OptionalLong.of(602), decision.askPrice());
        assertTrue(decision.bidPrice().orElseThrow() < 601);
        assertTrue(decision.askPrice().orElseThrow() > 600);
    }

    @Test
    void suppressesTheSideThatWouldWorsenInventoryAtTheHardLimit() {
        var market = new Bbo(600, 5, 610, 5);

        var longLimit = engine.decide(market, 2, 1, 2);
        assertTrue(longLimit.bidPrice().isEmpty());
        assertEquals(OptionalLong.of(606), longLimit.askPrice());

        var beyondLongLimit = engine.decide(market, 3, 1, 2);
        assertTrue(beyondLongLimit.bidPrice().isEmpty());
        assertEquals(OptionalLong.of(606), beyondLongLimit.askPrice());

        var shortLimit = engine.decide(market, -2, 1, 2);
        assertEquals(OptionalLong.of(604), shortLimit.bidPrice());
        assertTrue(shortLimit.askPrice().isEmpty());
    }

    @Test
    void suppressesBothSidesWhenMarketIsMissingOrStale() {
        assertTrue(engine.decide(Optional.empty(), true, 0, 1, 2).isSuppressed());
        assertTrue(engine.decide(
                Optional.of(new Bbo(600, 5, 610, 5)), false, 0, 1, 2).isSuppressed());
    }

    @Test
    void repeatedMarketAndPositionProduceAnEqualNoChurnDecision() {
        var market = new Bbo(600, 5, 610, 5);

        assertEquals(
                engine.decide(market, 0, 1, 2),
                engine.decide(market, 0, 1, 2));
    }

    @Test
    void rejectsInvalidRiskAndTickConfiguration() {
        var market = new Bbo(600, 5, 610, 5);

        assertThrows(IllegalArgumentException.class, () -> engine.decide(market, 0, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> engine.decide(market, 0, 1, 0));
    }

    @Test
    void quoteDecisionRejectsCrossedOrNonUnitQuotes() {
        assertThrows(IllegalArgumentException.class,
                () -> new QuoteDecision(OptionalLong.of(610), OptionalLong.of(610), 1));
        assertThrows(IllegalArgumentException.class,
                () -> new QuoteDecision(OptionalLong.of(600), OptionalLong.of(610), 2));
        assertFalse(new QuoteDecision(OptionalLong.of(600), OptionalLong.empty(), 1).isSuppressed());
    }
}
