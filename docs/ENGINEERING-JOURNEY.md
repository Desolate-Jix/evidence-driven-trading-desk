# Engineering Journey

## 1. Start with observable facts

The initial problem was not “find the best strategy.” It was to establish what the surrounding event-driven environment actually did. Controlled repetitions were used to distinguish observable behavior from assumptions, especially around request routing, lifecycle ordering, partial execution, ownership of execution notifications, and terminal cleanup.

That work led to a simple rule: financial state follows confirmed execution evidence, not request success.

## 2. Repair the accounting boundary

The first implementation work focused on errors that can create immediate risk:

- correctly signed cash and position updates;
- actual rather than requested execution quantity;
- position-cap checks based on owned state;
- distinct handling of acknowledgements and lifecycle events; and
- reconciliation against an independent ledger.

A controlled partial-execution case demonstrated why this mattered: a request for three units produced only two executed units, and both the independent and internal ledgers agreed on the resulting financial state.

## 3. Build a conservative passive component

The passive component was intentionally small before it was clever. It added typed formatting/parsing, exact ledgers, explicit order lifecycle tracking, inventory-aware quote suppression, cancel-before-replace behavior, and stale-data controls.

The development process used focused failing tests before each repair. Several operational defects were found this way, including reserved-inventory capacity, high-rate market-data backlog, and conservative rate limiting. The resulting design prefers deterministic safety over high quote frequency.

## 4. Add aggregate-risk control

The aggregate-risk component first operated in observation-only mode. This separated the hard problem of reconstructing exposure from the separate problem of placing risk-reducing orders. It then added bounded active hedging with independent reconciliation for positive exposure, negative exposure, partial quantities, rejected requests, and ambiguous outcomes.

An important finding was that two component accounts can each receive a legitimate event for the same economic interaction. Removing one indiscriminately would corrupt component ledgers; processing them without coordination could fabricate temporary aggregate exposure. The final design applies paired economic legs before re-evaluating the controller.

## 5. Validate the whole system, including failure paths

Three independent 10-minute controlled full-stack sessions tested independent ledger reconstruction, position limits, latency, component health, stale-data behavior, and teardown. A controlled market-data interruption produced no new actions from stale information; normal activity required a fresh market observation after recovery.

The objective was not a favourable mark-to-market result. The objective was to demonstrate that financial state and risk behavior remained coherent under normal load and a selected fault condition.

## 6. Keep negative research results

Profitability research was kept separate from the conservative default. A broad frozen offline evaluation found no candidate that passed every hard gate. A later one-sided research finalist appeared less lossy in short screens only while participating far less: it hit its own position limit after three fills and became inactive.

Strict post-trade markouts were sparse and adverse. Recycling designs reduced saturation only by multiplying action count by roughly 100 times, while failing worst-case checks. The correct engineering decision was to stop, avoid turning inactivity into a performance claim, and retain the conservative default.

## What this journey demonstrates

- Evidence hierarchy matters more than a plausible story.
- Accounting and lifecycle correctness are prerequisites for strategy evaluation.
- Risk controls need negative controls and independent reconstruction.
- Aggregate valuation is not causal evidence of a signal edge.
- A well-documented negative result is a successful decision when it prevents overfitting or unsafe promotion.
