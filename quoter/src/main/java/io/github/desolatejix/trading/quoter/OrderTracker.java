package io.github.desolatejix.trading.quoter;

import io.github.desolatejix.trading.model.Side;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class OrderTracker {
    private static final long MAX_SEQUENCE = 99_999_999L;

    private final Map<String, OrderState> orders = new HashMap<>();
    private final EnumMap<Side, String> activeBySide = new EnumMap<>(Side.class);
    private long sequence;

    public OrderTracker() {
        this(0);
    }

    public OrderTracker(long lastIssuedSequence) {
        if (lastIssuedSequence < 0 || lastIssuedSequence > MAX_SEQUENCE) {
            throw new IllegalArgumentException("lastIssuedSequence must be between 0 and 99999999");
        }
        sequence = lastIssuedSequence;
    }

    public OrderState submit(Side side, long quantity) {
        Objects.requireNonNull(side, "side");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (activeBySide.containsKey(side)) {
            throw new IllegalStateException("an unresolved order already occupies side " + side);
        }
        if (sequence == MAX_SEQUENCE) {
            throw new IllegalStateException("eight-digit order id space exhausted");
        }

        String orderId = "%08d".formatted(++sequence);
        OrderState state = new OrderState(orderId, side, quantity, 0, OrderStatus.PENDING);
        orders.put(orderId, state);
        activeBySide.put(side, orderId);
        return state;
    }

    public Optional<OrderState> active(Side side) {
        Objects.requireNonNull(side, "side");
        String orderId = activeBySide.get(side);
        return orderId == null ? Optional.empty() : Optional.of(orders.get(orderId));
    }

    public OrderState onAcknowledged(String orderId) {
        return requireOrder(orderId);
    }

    public OrderState onResting(String orderId) {
        OrderState current = requireOrder(orderId);
        if (current.status() == OrderStatus.RESTING) {
            return current;
        }
        return replace(current, OrderStatus.PENDING, OrderStatus.RESTING, current.filledQuantity());
    }

    public OrderState onExecution(String orderId, long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("execution quantity must be positive");
        }
        OrderState current = requireOrder(orderId);
        if (current.status() != OrderStatus.PENDING
                && current.status() != OrderStatus.RESTING
                && current.status() != OrderStatus.PARTIALLY_FILLED
                && current.status() != OrderStatus.CANCEL_PENDING) {
            throw invalidTransition(current, "execution");
        }

        long filled = Math.addExact(current.filledQuantity(), quantity);
        if (filled > current.originalQuantity()) {
            throw new IllegalStateException("execution would overfill order " + orderId);
        }
        OrderStatus next;
        if (filled == current.originalQuantity()) {
            next = OrderStatus.FILLED;
        } else if (current.status() == OrderStatus.CANCEL_PENDING) {
            next = OrderStatus.CANCEL_PENDING;
        } else {
            next = OrderStatus.PARTIALLY_FILLED;
        }
        return replace(current, next, filled);
    }

    public OrderState requestCancel(String orderId) {
        OrderState current = requireOrder(orderId);
        if (current.status() != OrderStatus.RESTING
                && current.status() != OrderStatus.PARTIALLY_FILLED) {
            throw invalidTransition(current, "cancel request");
        }
        return replace(current, OrderStatus.CANCEL_PENDING, current.filledQuantity());
    }

    public OrderState onCancelled(String orderId) {
        OrderState current = requireOrder(orderId);
        return replace(current, OrderStatus.CANCEL_PENDING,
                OrderStatus.CANCELLED, current.filledQuantity());
    }

    public OrderState onRejected(String orderId) {
        OrderState current = requireOrder(orderId);
        return replace(current, OrderStatus.PENDING, OrderStatus.REJECTED, 0);
    }

    private OrderState replace(
            OrderState current,
            OrderStatus required,
            OrderStatus next,
            long filledQuantity
    ) {
        if (current.status() != required) {
            throw invalidTransition(current, next.toString());
        }
        return replace(current, next, filledQuantity);
    }

    private OrderState replace(OrderState current, OrderStatus next, long filledQuantity) {
        OrderState updated = new OrderState(
                current.orderId(), current.side(), current.originalQuantity(), filledQuantity, next);
        orders.put(updated.orderId(), updated);
        if (updated.status().isTerminal()) {
            activeBySide.remove(updated.side(), updated.orderId());
        }
        return updated;
    }

    private OrderState requireOrder(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        OrderState state = orders.get(orderId);
        if (state == null) {
            throw new IllegalArgumentException("unknown order " + orderId);
        }
        return state;
    }

    private static IllegalStateException invalidTransition(OrderState state, String event) {
        return new IllegalStateException(
                "cannot apply " + event + " to " + state.orderId() + " in " + state.status());
    }
}
