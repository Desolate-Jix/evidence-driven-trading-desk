package io.github.desolatejix.trading.hedger;

import io.github.desolatejix.trading.model.Side;

import java.util.Objects;

public record HedgeDecision(Side side, long volume, long price) {
    public HedgeDecision {
        Objects.requireNonNull(side, "side");
        if (volume <= 0) {
            throw new IllegalArgumentException("volume must be positive");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }
    }
}
