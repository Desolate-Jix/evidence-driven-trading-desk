package io.github.desolatejix.trading.quoter;

import java.util.Objects;
import java.util.OptionalLong;

public record QuoteDecision(OptionalLong bidPrice, OptionalLong askPrice, int size) {
    public QuoteDecision {
        Objects.requireNonNull(bidPrice, "bidPrice");
        Objects.requireNonNull(askPrice, "askPrice");
        if (size != 1) {
            throw new IllegalArgumentException("the public MVP quotes size one");
        }
        bidPrice.ifPresent(price -> requirePositive(price, "bidPrice"));
        askPrice.ifPresent(price -> requirePositive(price, "askPrice"));
        if (bidPrice.isPresent() && askPrice.isPresent()
                && bidPrice.getAsLong() >= askPrice.getAsLong()) {
            throw new IllegalArgumentException("desired quotes must be uncrossed");
        }
    }

    public boolean isSuppressed() {
        return bidPrice.isEmpty() && askPrice.isEmpty();
    }

    public static QuoteDecision suppressed() {
        return new QuoteDecision(OptionalLong.empty(), OptionalLong.empty(), 1);
    }

    private static void requirePositive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive when present");
        }
    }
}
