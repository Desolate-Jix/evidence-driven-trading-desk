package io.github.desolatejix.trading.hedger;

import io.github.desolatejix.trading.model.Bbo;
import io.github.desolatejix.trading.model.Side;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HedgeControllerTest {
    private static final Bbo BBO = new Bbo(600, 4, 610, 4);

    @Test
    void positiveExcessCreatesClippedSellAtExecutableBid() {
        var controller = new HedgeController(1, 3);
        assertEquals(new HedgeDecision(Side.SELL, 3, 600), controller.evaluate("ALPHA", 5, BBO).orElseThrow());
        assertEquals(HedgeState.PENDING, controller.state("ALPHA"));
        assertEquals(3, controller.pendingVolume("ALPHA"));
    }

    @Test
    void negativeExcessCreatesBuyForOnlyExposureOutsideBand() {
        var controller = new HedgeController(2, 10);
        assertEquals(new HedgeDecision(Side.BUY, 3, 610), controller.evaluate("ALPHA", -5, BBO).orElseThrow());
    }

    @Test
    void staysIdleInsideBandAndWithoutFreshMarketData() {
        var controller = new HedgeController(2, 3);
        assertEquals(Optional.empty(), controller.evaluate("ALPHA", 2, BBO));
        assertEquals(Optional.empty(), controller.evaluate("ALPHA", -2, BBO));
        assertEquals(Optional.empty(), controller.evaluate("ALPHA", 5, null));
        assertEquals(Optional.empty(), controller.evaluate("ALPHA", 5, BBO, false));
        assertEquals(HedgeState.IDLE, controller.state("ALPHA"));
    }

    @Test
    void oneUnresolvedOrderSuppressesFurtherHedgesAndAcknowledgementDoesNotResolveIt() {
        var controller = new HedgeController(1, 3);
        controller.evaluate("ALPHA", 5, BBO).orElseThrow();
        assertEquals(Optional.empty(), controller.evaluate("ALPHA", 9, BBO));
        controller.onAcknowledgement("ALPHA");
        assertEquals(HedgeState.PENDING, controller.state("ALPHA"));
        assertEquals(3, controller.pendingVolume("ALPHA"));
    }

    @Test
    void partialConfirmedExecutionUsesActualQuantityAndOnlyFullResolutionReturnsIdle() {
        var controller = new HedgeController(1, 3);
        controller.evaluate("ALPHA", 5, BBO).orElseThrow();
        controller.onConfirmedExecution("ALPHA", 1);
        assertEquals(HedgeState.PENDING, controller.state("ALPHA"));
        assertEquals(2, controller.pendingVolume("ALPHA"));
        assertEquals(Optional.empty(), controller.evaluate("ALPHA", 4, BBO));
        controller.onConfirmedExecution("ALPHA", 2);
        assertEquals(HedgeState.IDLE, controller.state("ALPHA"));
        assertEquals(0, controller.pendingVolume("ALPHA"));
    }

    @Test
    void partialTerminalOrderReturnsIdleAndResidualNeedsAFreshMarketDecision() {
        var controller = new HedgeController(1, 3);
        controller.evaluate("ALPHA", 5, BBO).orElseThrow();
        controller.onConfirmedExecution("ALPHA", 1);

        controller.onTerminal("ALPHA");

        assertEquals(HedgeState.IDLE, controller.state("ALPHA"));
        assertTrue(controller.evaluate("ALPHA", 4, BBO, false).isEmpty());
        assertEquals(new HedgeDecision(Side.SELL, 3, 600),
                controller.evaluate("ALPHA", 4, BBO, true).orElseThrow());
    }

    @Test
    void rejectionAfterPartialExecutionIsContradictoryAndFailsClosed() {
        var controller = new HedgeController(1, 3);
        controller.evaluate("ALPHA", 5, BBO).orElseThrow();
        controller.onConfirmedExecution("ALPHA", 1);

        controller.onRejected("ALPHA");

        assertEquals(HedgeState.UNCERTAIN, controller.state("ALPHA"));
        assertTrue(controller.evaluate("ALPHA", 4, BBO).isEmpty());
    }

    @Test
    void rejectionClearsLifecycleWithoutInventingAnyFill() {
        var controller = new HedgeController(1, 3);
        controller.evaluate("ALPHA", 5, BBO).orElseThrow();
        controller.onRejected("ALPHA");
        assertEquals(HedgeState.IDLE, controller.state("ALPHA"));
        assertEquals(0, controller.pendingVolume("ALPHA"));
    }

    @Test
    void timeoutAndAmbiguousDisconnectFailClosedUntilExplicitReconciliation() {
        var controller = new HedgeController(1, 3);
        controller.evaluate("ALPHA", 5, BBO).orElseThrow();
        controller.onTimeout("ALPHA");
        assertEquals(HedgeState.UNCERTAIN, controller.state("ALPHA"));
        assertEquals(Optional.empty(), controller.evaluate("ALPHA", 5, BBO));
        controller.reconcile("ALPHA");
        assertEquals(HedgeState.IDLE, controller.state("ALPHA"));
        controller.evaluate("ALPHA", -5, BBO).orElseThrow();
        controller.onDisconnected("ALPHA");
        assertEquals(HedgeState.UNCERTAIN, controller.state("ALPHA"));
        assertEquals(Optional.empty(), controller.evaluate("ALPHA", -5, BBO));
    }

    @Test
    void longMinValueIsClippedWithoutOverflow() {
        var controller = new HedgeController(1, 3);
        assertEquals(new HedgeDecision(Side.BUY, 3, 610), controller.evaluate("ALPHA", Long.MIN_VALUE, BBO).orElseThrow());
    }

    @Test
    void rejectsImpossibleTransitionsAndInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new HedgeController(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new HedgeController(1, 0));
        var controller = new HedgeController(1, 3);
        assertThrows(IllegalStateException.class, () -> controller.onAcknowledgement("ALPHA"));
        assertThrows(IllegalStateException.class, () -> controller.onConfirmedExecution("ALPHA", 1));
        assertThrows(IllegalArgumentException.class, () -> controller.evaluate("", 5, BBO));
        controller.evaluate("ALPHA", 5, BBO).orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> controller.onConfirmedExecution("ALPHA", 0));
        assertThrows(IllegalStateException.class, () -> controller.onConfirmedExecution("ALPHA", 4));
    }
}
