package io.github.desolatejix.trading.model;

import java.util.Objects;
import java.util.Set;

public record Execution(
        String feed,
        String owner,
        String incomingOrderId,
        String restingOrderId,
        long volume,
        long price,
        long matchId,
        Side ownerSide,
        String eventRole
) {
    private static final Set<String> EVENT_ROLES = Set.of("MAKER", "TAKER");

    public Execution {
        feed = requireToken(feed, "feed");
        owner = requireToken(owner, "owner");
        incomingOrderId = requireToken(incomingOrderId, "incomingOrderId");
        restingOrderId = requireToken(restingOrderId, "restingOrderId");
        ownerSide = Objects.requireNonNull(ownerSide, "ownerSide");
        eventRole = requireToken(eventRole, "eventRole");
        if (!EVENT_ROLES.contains(eventRole)) {
            throw new IllegalArgumentException("eventRole must be MAKER or TAKER");
        }
        if (volume <= 0) {
            throw new IllegalArgumentException("volume must be positive");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }
        if (matchId <= 0) {
            throw new IllegalArgumentException("matchId must be positive");
        }
    }

    private static String requireToken(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
