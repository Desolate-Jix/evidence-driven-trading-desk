package io.github.desolatejix.trading.accounting;

import io.github.desolatejix.trading.model.Execution;
import io.github.desolatejix.trading.model.PositionSnapshot;
import io.github.desolatejix.trading.model.Side;

import java.util.HashMap;
import java.util.Map;

public final class PositionBook {
    private final Map<String, PositionSnapshot> byFeed = new HashMap<>();

    public void apply(Execution execution) {
        var before = snapshot(execution.feed());
        long signedVolume = execution.ownerSide() == Side.BUY
                ? execution.volume()
                : Math.negateExact(execution.volume());
        long position = Math.addExact(before.position(), signedVolume);
        long cashDelta = Math.negateExact(Math.multiplyExact(signedVolume, execution.price()));
        long cash = Math.addExact(before.cash(), cashDelta);
        byFeed.put(execution.feed(), new PositionSnapshot(position, cash));
    }

    public PositionSnapshot snapshot(String feed) {
        if (feed == null || feed.isBlank()) {
            throw new IllegalArgumentException("feed must not be blank");
        }
        return byFeed.getOrDefault(feed, new PositionSnapshot(0, 0));
    }
}
