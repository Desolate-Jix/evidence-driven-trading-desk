package io.github.desolatejix.trading.quoter;

import io.github.desolatejix.trading.model.Bbo;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public final class QuoteEngine {
    public QuoteDecision decide(Bbo bbo, long position, long tickSize, long inventoryLimit) {
        return decide(Optional.ofNullable(bbo), true, position, tickSize, inventoryLimit);
    }

    public QuoteDecision decide(
            Optional<Bbo> bbo,
            boolean marketFresh,
            long position,
            long tickSize,
            long inventoryLimit
    ) {
        Objects.requireNonNull(bbo, "bbo");
        validateConfiguration(tickSize, inventoryLimit);
        if (bbo.isEmpty() || !marketFresh) {
            return QuoteDecision.suppressed();
        }

        Bbo market = bbo.orElseThrow();
        long twiceTick = Math.multiplyExact(tickSize, 2);
        long midpointTimesTwo = market.midpointTimesTwo();

        OptionalLong bid = desiredBid(midpointTimesTwo, twiceTick, tickSize, market.askPrice());
        OptionalLong ask = desiredAsk(midpointTimesTwo, twiceTick, tickSize, market.bidPrice());

        if (position >= inventoryLimit) {
            bid = OptionalLong.empty();
        }
        if (position <= -inventoryLimit) {
            ask = OptionalLong.empty();
        }

        assertPassive(bid, ask, market, tickSize);
        return new QuoteDecision(bid, ask, 1);
    }

    private static OptionalLong desiredBid(
            long midpointTimesTwo,
            long twiceTick,
            long tickSize,
            long marketAsk
    ) {
        long numerator = Math.subtractExact(midpointTimesTwo, twiceTick);
        if (numerator <= 0) {
            return OptionalLong.empty();
        }
        long midpointUnits = Math.floorDiv(numerator, twiceTick);
        long midpointPrice = Math.multiplyExact(midpointUnits, tickSize);

        long passiveUnits = Math.floorDiv(Math.subtractExact(marketAsk, 1), tickSize);
        long passiveCeiling = Math.multiplyExact(passiveUnits, tickSize);
        long price = Math.min(midpointPrice, passiveCeiling);
        return price > 0 ? OptionalLong.of(price) : OptionalLong.empty();
    }

    private static OptionalLong desiredAsk(
            long midpointTimesTwo,
            long twiceTick,
            long tickSize,
            long marketBid
    ) {
        long numerator = Math.addExact(midpointTimesTwo, twiceTick);
        long midpointUnits = ceilDivPositive(numerator, twiceTick);
        long midpointPrice = Math.multiplyExact(midpointUnits, tickSize);

        long passiveUnits = Math.addExact(Math.floorDiv(marketBid, tickSize), 1);
        long passiveFloor = Math.multiplyExact(passiveUnits, tickSize);
        return OptionalLong.of(Math.max(midpointPrice, passiveFloor));
    }

    private static long ceilDivPositive(long numerator, long denominator) {
        long quotient = numerator / denominator;
        return numerator % denominator == 0 ? quotient : Math.addExact(quotient, 1);
    }

    private static void validateConfiguration(long tickSize, long inventoryLimit) {
        if (tickSize <= 0) {
            throw new IllegalArgumentException("tickSize must be positive");
        }
        if (inventoryLimit <= 0) {
            throw new IllegalArgumentException("inventoryLimit must be positive");
        }
    }

    private static void assertPassive(
            OptionalLong bid,
            OptionalLong ask,
            Bbo market,
            long tickSize
    ) {
        if (bid.isPresent()) {
            long price = bid.getAsLong();
            if (price % tickSize != 0 || price >= market.askPrice()) {
                throw new IllegalStateException("bid is not tick-aligned and passive");
            }
        }
        if (ask.isPresent()) {
            long price = ask.getAsLong();
            if (price % tickSize != 0 || price <= market.bidPrice()) {
                throw new IllegalStateException("ask is not tick-aligned and passive");
            }
        }
        if (bid.isPresent() && ask.isPresent() && bid.getAsLong() >= ask.getAsLong()) {
            throw new IllegalStateException("desired quotes cross each other");
        }
    }
}
