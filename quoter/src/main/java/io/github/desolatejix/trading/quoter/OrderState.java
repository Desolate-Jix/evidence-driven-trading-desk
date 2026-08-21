package io.github.desolatejix.trading.quoter;

import io.github.desolatejix.trading.model.Side;

import java.util.Objects;

public record OrderState(
        String orderId,
        Side side,
        long originalQuantity,
        long filledQuantity,
        OrderStatus status
) {
    public OrderState {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(status, "status");
        if (originalQuantity <= 0) {
            throw new IllegalArgumentException("originalQuantity must be positive");
        }
        if (filledQuantity < 0 || filledQuantity > originalQuantity) {
            throw new IllegalArgumentException("filledQuantity must be within the order quantity");
        }

        switch (status) {
            case PENDING, RESTING, REJECTED -> require(filledQuantity == 0, status);
            case PARTIALLY_FILLED -> require(
                    filledQuantity > 0 && filledQuantity < originalQuantity, status);
            case FILLED -> require(filledQuantity == originalQuantity, status);
            case CANCEL_PENDING, CANCELLED -> require(filledQuantity < originalQuantity, status);
        }
    }

    public long remainingQuantity() {
        return Math.subtractExact(originalQuantity, filledQuantity);
    }

    public boolean isActive() {
        return !status.isTerminal();
    }

    private static void require(boolean valid, OrderStatus status) {
        if (!valid) {
            throw new IllegalArgumentException("filledQuantity is inconsistent with " + status);
        }
    }
}
