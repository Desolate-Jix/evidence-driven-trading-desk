package io.github.desolatejix.trading.quoter;

import io.github.desolatejix.trading.model.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OrderTrackerTest {
    @Test
    void tracksPendingRestingPartialAndFilledLifecycle() {
        var tracker = new OrderTracker();
        var submitted = tracker.submit(Side.BUY, 3);

        assertEquals("00000001", submitted.orderId());
        assertEquals(OrderStatus.PENDING, submitted.status());
        assertEquals(OrderStatus.RESTING, tracker.onResting(submitted.orderId()).status());
        assertEquals(OrderStatus.PARTIALLY_FILLED,
                tracker.onExecution(submitted.orderId(), 1).status());

        var filled = tracker.onExecution(submitted.orderId(), 2);
        assertEquals(OrderStatus.FILLED, filled.status());
        assertEquals(3, filled.filledQuantity());
        assertEquals(0, filled.remainingQuantity());
        assertTrue(tracker.active(Side.BUY).isEmpty());
    }

    @Test
    void cancelRequestKeepsSideOccupiedUntilTerminalCancellationEvidence() {
        var tracker = new OrderTracker();
        var order = tracker.submit(Side.SELL, 1);
        tracker.onResting(order.orderId());

        var pendingCancel = tracker.requestCancel(order.orderId());
        assertEquals(OrderStatus.CANCEL_PENDING, pendingCancel.status());
        assertEquals(order.orderId(), tracker.active(Side.SELL).orElseThrow().orderId());
        assertThrows(IllegalStateException.class, () -> tracker.submit(Side.SELL, 1));

        assertEquals(OrderStatus.CANCELLED, tracker.onCancelled(order.orderId()).status());
        assertTrue(tracker.active(Side.SELL).isEmpty());
    }

    @Test
    void rejectionIsTerminalAndNeverInventsAFill() {
        var tracker = new OrderTracker();
        var order = tracker.submit(Side.BUY, 2);

        var rejected = tracker.onRejected(order.orderId());

        assertEquals(OrderStatus.REJECTED, rejected.status());
        assertEquals(0, rejected.filledQuantity());
        assertEquals(2, rejected.remainingQuantity());
        assertTrue(tracker.active(Side.BUY).isEmpty());
    }

    @Test
    void toleratesRestingCallbackBeforeAcknowledgementWithoutRegressingState() {
        var tracker = new OrderTracker();
        var order = tracker.submit(Side.BUY, 1);

        assertEquals(OrderStatus.RESTING, tracker.onResting(order.orderId()).status());
        assertEquals(OrderStatus.RESTING, tracker.onAcknowledged(order.orderId()).status());
    }

    @Test
    void acknowledgementAloneDoesNotClaimThatAnOrderIsResting() {
        var tracker = new OrderTracker();
        var order = tracker.submit(Side.BUY, 1);

        assertEquals(OrderStatus.PENDING, tracker.onAcknowledged(order.orderId()).status());
        assertEquals(OrderStatus.PENDING, tracker.active(Side.BUY).orElseThrow().status());
    }

    @Test
    void acceptsConfirmedExecutionBeforeTheRequestReply() {
        var tracker = new OrderTracker();
        var order = tracker.submit(Side.BUY, 1);

        assertEquals(OrderStatus.FILLED, tracker.onExecution(order.orderId(), 1).status());
        assertTrue(tracker.active(Side.BUY).isEmpty());
        assertEquals(OrderStatus.FILLED, tracker.onAcknowledged(order.orderId()).status());
    }

    @Test
    void partialFillDuringCancelRemainsPendingUntilCancellationEvidence() {
        var tracker = new OrderTracker();
        var order = tracker.submit(Side.SELL, 3);
        tracker.onResting(order.orderId());
        tracker.requestCancel(order.orderId());

        var partial = tracker.onExecution(order.orderId(), 1);

        assertEquals(OrderStatus.CANCEL_PENDING, partial.status());
        assertEquals(1, partial.filledQuantity());
        assertEquals(OrderStatus.CANCELLED, tracker.onCancelled(order.orderId()).status());
        assertTrue(tracker.active(Side.SELL).isEmpty());
    }

    @Test
    void rejectsImpossibleTransitionsAndOverfills() {
        var tracker = new OrderTracker();
        var pending = tracker.submit(Side.BUY, 1);

        assertThrows(IllegalStateException.class, () -> tracker.requestCancel(pending.orderId()));
        assertThrows(IllegalArgumentException.class, () -> tracker.onExecution(pending.orderId(), 0));
        tracker.onResting(pending.orderId());
        assertThrows(IllegalStateException.class, () -> tracker.onExecution(pending.orderId(), 2));
        tracker.onExecution(pending.orderId(), 1);
        assertThrows(IllegalStateException.class, () -> tracker.onCancelled(pending.orderId()));
        assertThrows(IllegalArgumentException.class, () -> tracker.onAcknowledged("unknown"));
    }

    @Test
    void permitsOnlyOneActiveOrderPerSideButAllowsOppositeSides() {
        var tracker = new OrderTracker();
        tracker.submit(Side.BUY, 1);
        tracker.submit(Side.SELL, 1);

        assertTrue(tracker.active(Side.BUY).isPresent());
        assertTrue(tracker.active(Side.SELL).isPresent());
        assertThrows(IllegalStateException.class, () -> tracker.submit(Side.BUY, 1));
        assertThrows(IllegalStateException.class, () -> tracker.submit(Side.SELL, 1));
    }

    @Test
    void generatesEightDigitMonotonicIdsAndFailsBeforeOverflow() {
        var tracker = new OrderTracker(99_999_997L);

        var first = tracker.submit(Side.BUY, 1);
        var second = tracker.submit(Side.SELL, 1);

        assertEquals("99999998", first.orderId());
        assertEquals("99999999", second.orderId());
        assertFalse(first.orderId().equals(second.orderId()));

        tracker.onRejected(first.orderId());
        assertThrows(IllegalStateException.class, () -> tracker.submit(Side.BUY, 1));
    }

    @Test
    void validatesSubmittedQuantityAndOrderStateInvariants() {
        var tracker = new OrderTracker();
        assertThrows(IllegalArgumentException.class, () -> tracker.submit(Side.BUY, 0));
        assertThrows(IllegalArgumentException.class, () -> new OrderTracker(-1));
        assertThrows(IllegalArgumentException.class,
                () -> new OrderState("00000001", Side.BUY, 1, 2, OrderStatus.FILLED));
    }
}
