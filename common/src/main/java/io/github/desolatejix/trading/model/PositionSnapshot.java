package io.github.desolatejix.trading.model;

public record PositionSnapshot(long position, long cash) {
    public long markToMid(long midpoint) {
        if (midpoint <= 0) {
            throw new IllegalArgumentException("midpoint must be positive");
        }
        return Math.addExact(cash, Math.multiplyExact(position, midpoint));
    }
}
