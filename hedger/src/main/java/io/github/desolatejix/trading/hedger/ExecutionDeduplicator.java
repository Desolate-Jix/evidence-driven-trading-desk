package io.github.desolatejix.trading.hedger;

import io.github.desolatejix.trading.model.Execution;

import java.util.HashSet;
import java.util.Set;

public final class ExecutionDeduplicator {
    private final Set<ExecutionKey> seen = new HashSet<>();

    public boolean firstDelivery(Execution execution) {
        return seen.add(ExecutionKey.from(execution));
    }

    public int uniqueCount() {
        return seen.size();
    }
}
