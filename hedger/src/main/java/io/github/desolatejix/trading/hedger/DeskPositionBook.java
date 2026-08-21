package io.github.desolatejix.trading.hedger;

import io.github.desolatejix.trading.accounting.PositionBook;
import io.github.desolatejix.trading.model.Execution;
import io.github.desolatejix.trading.model.PositionSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DeskPositionBook {
    private final Set<String> deskOwners;
    private final Map<String, PositionBook> booksByOwner = new HashMap<>();
    private final ExecutionDeduplicator deduplicator = new ExecutionDeduplicator();

    public DeskPositionBook(Set<String> deskOwners) {
        if (deskOwners == null || deskOwners.isEmpty()) {
            throw new IllegalArgumentException("deskOwners must not be empty");
        }
        for (String owner : deskOwners) {
            requireToken(owner, "owner");
        }
        this.deskOwners = Set.copyOf(deskOwners);
    }

    public boolean apply(Execution execution) {
        Objects.requireNonNull(execution, "execution");
        if (!deskOwners.contains(execution.owner())) {
            return false;
        }
        if (!deduplicator.firstDelivery(execution)) {
            return false;
        }
        booksByOwner.computeIfAbsent(execution.owner(), ignored -> new PositionBook()).apply(execution);
        return true;
    }

    public PositionSnapshot snapshot(String owner, String feed) {
        requireToken(owner, "owner");
        requireToken(feed, "feed");
        var book = booksByOwner.get(owner);
        return book == null ? new PositionSnapshot(0, 0) : book.snapshot(feed);
    }

    public PositionSnapshot aggregate(String feed) {
        requireToken(feed, "feed");
        long position = 0;
        long cash = 0;
        for (String owner : deskOwners) {
            var snapshot = snapshot(owner, feed);
            position = Math.addExact(position, snapshot.position());
            cash = Math.addExact(cash, snapshot.cash());
        }
        return new PositionSnapshot(position, cash);
    }

    private static String requireToken(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
