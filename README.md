# Evidence-Driven Trading Desk

A public, clean-room account of an event-driven trading-system engineering project. The work prioritises correct financial state, bounded risk, reproducible validation, and honest research conclusions over attractive backtests or activity volume.

> **Scope.** This repository describes a controlled research and engineering exercise. It is not investment advice, a live trading product, or a certification of profitability.

## What it demonstrates

The design has three independently operated components:

1. an **aggressive execution component** that reacts to a bounded signal;
2. a **passive quoting component** that provides small, inventory-aware quotes; and
3. an **aggregate-risk component** that reconstructs combined exposure and can reduce excess risk.

The components exchange market observations and lifecycle events, but each keeps separately attributable financial state. This separation is intentional: an aggregate hedge can reduce desk exposure without changing another component's own position capacity.

## Engineering posture

- **Execution, not acknowledgement, changes money.** Position and cash update only from confirmed execution evidence.
- **Lifecycle-aware state.** Intent, acknowledgement, active lifecycle, terminal lifecycle, and execution are separate facts.
- **Fail closed under uncertainty.** Stale market information, ambiguous request outcomes, disconnects, and shutdown stop new risk-taking actions.
- **Idempotent accounting.** True repeat deliveries are suppressed without erasing legitimate component-level economic legs.
- **Reproducible gates.** Source builds, focused tests, controlled validation, full-stack risk sessions, and cleanup checks are all treated as separate evidence layers.

See [Architecture](docs/ARCHITECTURE.md), [Validation](docs/VALIDATION.md), and [Reproducibility](docs/REPRODUCIBILITY.md).

## Validation highlights

The final regression recorded:

- 33 Python-component tests, 7 research/production-isolation tests, 46 quoting tests, and 52 hedging tests passing;
- a five-minute controlled quoting soak with 1,851 accepted orders and 820 executions, while inventory stayed within its configured `[-2, +2]` bound;
- three independent 10-minute full-stack sessions totalling 1,807.196 seconds, with peak absolute desk exposure of 8 and weighted average absolute exposure of 1.014;
- independent-ledger agreement in every session, with zero duplicate execution notifications, duplicate live quotes, ledger mismatches, timeouts, exceptions, or component restarts; and
- bounded startup, health, teardown, and cleanup checks passing.

These are **correctness and bounded-risk results in a controlled environment**. They are not return forecasts.

## Research outcome: intentionally not a profit claim

The economic investigation is a negative and inconclusive result, preserved because it changed the production decision:

- A frozen offline evaluation of 1,200 candidates found **zero** candidates passing every hard gate.
- A later SELL-only research finalist executed exactly three sells in each of three 60-second screens and then saturated its own `-3` position limit.
- Across those screens, participation fell from 163 comparator fills to 9 finalist fills. The apparent mark-to-market improvement is therefore descriptive, not evidence of repeatable entry quality.
- Strict 500 ms post-trade markouts had only three eligible fills, all adverse: **-2.5, -2.0, and -2.0 ticks**.
- Three recycling/exit ideas reduced saturation but failed their predeclared worst-case and turnover gates, each requiring roughly **100x** the reference action multiple.

No research variant was promoted as a profitability strategy. The production-oriented baseline remains the more conservative, mechanically tested design. Read [Research Results](docs/RESEARCH-RESULTS.md) for the full interpretation.

## Documentation

- [Architecture](docs/ARCHITECTURE.md) — component boundaries, state flow, and safety invariants.
- [Engineering Journey](docs/ENGINEERING-JOURNEY.md) — evidence-first development sequence and decision points.
- [Research Results](docs/RESEARCH-RESULTS.md) — adverse results, saturation, and the no-promotion decision.
- [Validation](docs/VALIDATION.md) — test, soak, full-stack, and cleanup evidence.
- [Reproducibility](docs/REPRODUCIBILITY.md) — what a repeatable review should establish.
- [Security and Data Boundary](docs/SECURITY-AND-DATA-BOUNDARY.md) — publication and data-handling constraints.

A Simplified Chinese version is available at [README.zh-CN.md](README.zh-CN.md).

## Try the clean-room domain core

Requirements: Java 17+ and Maven 3.9+.

```bash
python -B tools/audit_publication.py .
mvn test
```

Run the publication audit before Maven creates ignored build output.

The three Maven modules contain only transport-neutral models and tests:

- `common`: signed execution accounting and immutable market models;
- `quoter`: passive quote decisions and order lifecycle tracking; and
- `hedger`: per-component desk accounting, exact-delivery deduplication, and a
  fail-closed hedge state machine.

They do not connect to an exchange or submit orders. All included identifiers
and event examples are synthetic.
