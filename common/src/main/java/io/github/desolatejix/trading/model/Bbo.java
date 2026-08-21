package io.github.desolatejix.trading.model;

public record Bbo(long bidPrice, long bidVolume, long askPrice, long askVolume) {
    public Bbo {
        if (bidPrice <= 0 || askPrice <= 0) {
            throw new IllegalArgumentException("prices must be positive");
        }
        if (bidVolume <= 0 || askVolume <= 0) {
            throw new IllegalArgumentException("volumes must be positive");
        }
        if (bidPrice >= askPrice) {
            throw new IllegalArgumentException("BBO must be unlocked and uncrossed");
        }
    }

    public long midpointTimesTwo() {
        return Math.addExact(bidPrice, askPrice);
    }
}
