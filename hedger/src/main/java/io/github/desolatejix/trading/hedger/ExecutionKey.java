package io.github.desolatejix.trading.hedger;

import io.github.desolatejix.trading.model.Execution;
import io.github.desolatejix.trading.model.Side;

import java.util.Objects;

public record ExecutionKey(
        String feed,
        String eventRole,
        String owner,
        String incomingOrderId,
        String restingOrderId,
        long volume,
        long price,
        long matchId,
        Side ownerSide
) {
    public ExecutionKey {
        Objects.requireNonNull(feed, "feed");
        Objects.requireNonNull(eventRole, "eventRole");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(incomingOrderId, "incomingOrderId");
        Objects.requireNonNull(restingOrderId, "restingOrderId");
        Objects.requireNonNull(ownerSide, "ownerSide");
    }

    public static ExecutionKey from(Execution execution) {
        Objects.requireNonNull(execution, "execution");
        return new ExecutionKey(
                execution.feed(),
                execution.eventRole(),
                execution.owner(),
                execution.incomingOrderId(),
                execution.restingOrderId(),
                execution.volume(),
                execution.price(),
                execution.matchId(),
                execution.ownerSide()
        );
    }
}
