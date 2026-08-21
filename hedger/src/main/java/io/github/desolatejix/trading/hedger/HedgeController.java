package io.github.desolatejix.trading.hedger;

import io.github.desolatejix.trading.model.Bbo;
import io.github.desolatejix.trading.model.Side;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class HedgeController {
    private final long hedgeBand;
    private final long maximumClip;
    private final Map<String, Lifecycle> lifecycleByFeed = new HashMap<>();

    public HedgeController(long hedgeBand, long maximumClip) {
        if (hedgeBand < 0) {
            throw new IllegalArgumentException("hedgeBand must not be negative");
        }
        if (maximumClip <= 0) {
            throw new IllegalArgumentException("maximumClip must be positive");
        }
        this.hedgeBand = hedgeBand;
        this.maximumClip = maximumClip;
    }

    public Optional<HedgeDecision> evaluate(String feed, long aggregatePosition, Bbo bbo) {
        return evaluate(feed, aggregatePosition, bbo, true);
    }

    public Optional<HedgeDecision> evaluate(String feed, long aggregatePosition, Bbo bbo,
                                            boolean marketDataFresh) {
        requireFeed(feed);
        if (state(feed) != HedgeState.IDLE || bbo == null || !marketDataFresh) {
            return Optional.empty();
        }

        Side side;
        long price;
        if (aggregatePosition > hedgeBand) {
            side = Side.SELL;
            price = bbo.bidPrice();
        } else if (aggregatePosition < -hedgeBand) {
            side = Side.BUY;
            price = bbo.askPrice();
        } else {
            return Optional.empty();
        }

        long volume = clippedExcess(aggregatePosition);
        lifecycleByFeed.put(feed, new Lifecycle(HedgeState.PENDING, volume, 0));
        return Optional.of(new HedgeDecision(side, volume, price));
    }

    public HedgeState state(String feed) {
        requireFeed(feed);
        return lifecycleByFeed.getOrDefault(feed, Lifecycle.IDLE).state();
    }

    public long pendingVolume(String feed) {
        requireFeed(feed);
        return lifecycleByFeed.getOrDefault(feed, Lifecycle.IDLE).remainingVolume();
    }

    public void onAcknowledgement(String feed) {
        requirePending(feed, "acknowledgement");
    }

    public void onConfirmedExecution(String feed, long actualVolume) {
        if (actualVolume <= 0) {
            throw new IllegalArgumentException("actualVolume must be positive");
        }
        var lifecycle = requirePending(feed, "execution");
        if (actualVolume > lifecycle.remainingVolume()) {
            throw new IllegalStateException("execution exceeds unresolved hedge volume");
        }
        long remaining = Math.subtractExact(lifecycle.remainingVolume(), actualVolume);
        if (remaining == 0) {
            lifecycleByFeed.remove(feed);
        } else {
            lifecycleByFeed.put(feed, new Lifecycle(
                    HedgeState.PENDING,
                    remaining,
                    Math.addExact(lifecycle.executedVolume(), actualVolume)));
        }
    }

    /** Records terminal lifecycle evidence for an order that may have filled only partially. */
    public void onTerminal(String feed) {
        requirePending(feed, "terminal event");
        lifecycleByFeed.remove(feed);
    }

    public void onRejected(String feed) {
        var pending = requirePending(feed, "rejection");
        if (pending.executedVolume() == 0) {
            lifecycleByFeed.remove(feed);
        } else {
            lifecycleByFeed.put(feed, new Lifecycle(
                    HedgeState.UNCERTAIN, pending.remainingVolume(), pending.executedVolume()));
        }
    }

    public void onTimeout(String feed) {
        var pending = requirePending(feed, "timeout");
        lifecycleByFeed.put(feed, new Lifecycle(
                HedgeState.UNCERTAIN, pending.remainingVolume(), pending.executedVolume()));
    }

    public void onDisconnected(String feed) {
        requireFeed(feed);
        var current = lifecycleByFeed.getOrDefault(feed, Lifecycle.IDLE);
        lifecycleByFeed.put(feed, new Lifecycle(
                HedgeState.UNCERTAIN, current.remainingVolume(), current.executedVolume()));
    }

    public void reconcile(String feed) {
        requireFeed(feed);
        if (state(feed) != HedgeState.UNCERTAIN) {
            throw new IllegalStateException("reconciliation requires UNCERTAIN state");
        }
        lifecycleByFeed.remove(feed);
    }

    private long clippedExcess(long aggregatePosition) {
        var absolute = BigInteger.valueOf(aggregatePosition).abs();
        var excess = absolute.subtract(BigInteger.valueOf(hedgeBand));
        return excess.min(BigInteger.valueOf(maximumClip)).longValueExact();
    }

    private Lifecycle requirePending(String feed, String event) {
        requireFeed(feed);
        var lifecycle = lifecycleByFeed.getOrDefault(feed, Lifecycle.IDLE);
        if (lifecycle.state() != HedgeState.PENDING) {
            throw new IllegalStateException(event + " requires PENDING state");
        }
        return lifecycle;
    }

    private static void requireFeed(String feed) {
        if (feed == null || feed.isBlank()) {
            throw new IllegalArgumentException("feed must not be blank");
        }
    }

    private record Lifecycle(HedgeState state, long remainingVolume, long executedVolume) {
        private static final Lifecycle IDLE = new Lifecycle(HedgeState.IDLE, 0, 0);
    }
}
