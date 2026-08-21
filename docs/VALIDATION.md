# Validation

## Evidence hierarchy

Validation is layered so that a favourable aggregate result cannot conceal an accounting or risk defect:

1. **Unit tests** cover parsing, formatting, signed ledgers, lifecycle transitions, deduplication, configuration, and failure paths.
2. **Container source builds** show that the components build from source in their intended runtime environment.
3. **Controlled integration checks** verify orders, lifecycle outcomes, execution quantities, and independently reconstructed ledgers.
4. **Soak and full-stack sessions** measure risk envelopes, latency, health, and cleanup.
5. **Economic research** is treated as market-dependent and cannot override a correctness failure.

## Public clean-room regression

The publishable transport-neutral modules currently contain 46 focused JUnit
tests: 7 accounting/model tests, 19 quoting/lifecycle tests, and 20 aggregate
risk tests. Run them with `mvn test`. The publication guard has its own Python
test suite and must also pass on the repository root.

## Historical controlled-system regression

| Check | Result |
|---|---|
| Aggressive execution component tests | 33 passing |
| Research/production isolation tests | 7 passing |
| Passive quoting tests | 46 passing |
| Aggregate-risk tests | 52 passing |
| Container source builds | Passing |
| Bounded full-stack startup | Passing |
| Bounded component termination | Passing |
| Health scan and cleanup | Passing |

These historical test counts and build checks support the frozen controlled
implementation only; the environment-specific adapters are intentionally not
redistributed here. They are not a claim of universal correctness.

## Passive quoting soak

A controlled five-minute soak recorded 1,851 accepted orders and 820 executions. Inventory remained within `[-2, +2]`; there was no duplicate live side, and every accepted order reached a terminal state before clean exit.

This supports lifecycle and inventory safety under sustained activity. It does not establish spread-capture profitability or immunity to adverse selection.

## Full-stack risk sessions

Three independent 10-minute sessions produced the following aggregate measures:

| Metric | Result |
|---|---:|
| Observed duration | 1,807.196 s |
| Peak absolute desk exposure | 8 |
| Weighted average absolute exposure | 1.014 |
| Hedge request latency, p95 | 0.496 s |
| Hedge fill latency, p95 | 0.510 s |
| Duplicate execution notifications | 0 |
| Duplicate live quotes | 0 |
| Internal-ledger mismatches | 0 |
| Timeouts, exceptions, restarts | 0 |

Independent ledger reconstruction matched the components' internal ledgers in every session. A controlled market-data interruption also produced zero new quote, execution, or hedge actions based on stale information.

## Negative controls and limitations

The validation deliberately records what it did not establish:

- the environment is controlled and bounded, not a live market;
- validation covers a single instrument configuration rather than universal market coverage;
- the design fails closed after uncertain recovery rather than claiming durable cross-process reconstruction;
- no post-only execution guarantee was available, so a limit order can become aggressive during a market race; and
- favourable or unfavourable mark-to-mid does not prove a causal strategy result.

## Cleanup is a validation result

Successful testing includes teardown. Each bounded full-stack regression checked that the stack was removed and its externally exposed service endpoint was no longer listening. Cleanup evidence prevents a passed test from silently leaving operational state behind.
