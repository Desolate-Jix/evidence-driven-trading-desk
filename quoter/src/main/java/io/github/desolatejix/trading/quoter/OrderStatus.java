package io.github.desolatejix.trading.quoter;

public enum OrderStatus {
    PENDING,
    RESTING,
    PARTIALLY_FILLED,
    CANCEL_PENDING,
    FILLED,
    CANCELLED,
    REJECTED;

    public boolean isTerminal() {
        return this == FILLED || this == CANCELLED || this == REJECTED;
    }
}
