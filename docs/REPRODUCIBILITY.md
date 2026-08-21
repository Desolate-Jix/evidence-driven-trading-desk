# Reproducibility

## What can be reproduced publicly

This repository is a clean-room engineering narrative and set of publishable examples. A reproducible review should establish the following:

1. documentation is internally consistent and does not expose protected material;
2. the public examples build and test from the repository's declared dependencies;
3. validation claims remain scoped to controlled, bounded evidence; and
4. economic conclusions remain negative/inconclusive rather than being rewritten as performance marketing.

## Reproducible decision rules

The important result is not one number but a set of repeatable rules:

- financial state changes only on confirmed executions;
- ambiguous outcomes block new risk-taking actions;
- lifecycle terminality is required before replacement;
- stale information blocks new actions;
- independent reconciliation can contradict internal state; and
- no strategy is promoted unless its predeclared correctness, risk, robustness, and economic gates all pass.

These rules are more portable than environment-specific connection details or market identifiers.

## Evidence levels

| Level | Reproducible question | Required conclusion |
|---|---|---|
| Unit | Does a local state transition preserve its invariant? | A focused invariant passes or fails. |
| Build | Can the documented component be constructed from source? | The declared runtime artefact is buildable. |
| Controlled integration | Does observed behavior match the accounting/lifecycle model? | State is reconciled against independent evidence. |
| Full stack | Do components remain within risk and health bounds together? | Bounds, latency, and cleanup are measured. |
| Research | Is a candidate robust enough to promote? | A negative result remains negative if gates do not pass. |

## Review checklist

- Read the architecture and identify the execution-driven accounting boundary.
- Confirm that passive quoting and aggregate-risk controls fail closed on uncertain inputs.
- Compare validation metrics with their stated controlled-environment limits.
- Read the research result before interpreting any mark-to-mid metric.
- Run the publication audit and its tests before sharing the repository.

## What is deliberately not reproduced here

The public repository does not contain private environments, sensitive captures, identity-bearing traces, deployment credentials, or environment-specific submission materials. Those omissions are intentional. Public reproducibility means repeatable reasoning, testable clean-room examples, and accurate scope—not redistribution of restricted artefacts.
