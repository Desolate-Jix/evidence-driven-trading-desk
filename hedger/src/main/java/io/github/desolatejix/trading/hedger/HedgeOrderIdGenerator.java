package io.github.desolatejix.trading.hedger;

import java.util.Locale;

public final class HedgeOrderIdGenerator {
    private static final long MAXIMUM_ID = 99_999_999L;
    private long next;

    public HedgeOrderIdGenerator() {
        this(1);
    }

    public HedgeOrderIdGenerator(long firstId) {
        if (firstId < 1 || firstId > MAXIMUM_ID) {
            throw new IllegalArgumentException("firstId must be from 1 through 99999999");
        }
        this.next = firstId;
    }

    public synchronized String next() {
        if (next > MAXIMUM_ID) {
            throw new IllegalStateException("eight-digit hedge order ID space exhausted");
        }
        String id = String.format(Locale.ROOT, "%08d", next);
        next = Math.addExact(next, 1);
        return id;
    }
}
