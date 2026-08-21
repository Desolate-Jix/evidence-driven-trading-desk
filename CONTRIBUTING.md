# Contributing

This repository publishes a small, transport-neutral domain core and a
sanitized engineering retrospective. Contributions should preserve that
boundary.

## Before opening a pull request

1. Add focused tests for behavioural changes.
2. Run `python -B tools/audit_publication.py .` on the clean source tree.
3. Run `mvn test` with Java 17 or newer.
4. Keep examples synthetic and identifiers generic.
5. Do not add broker adapters, private datasets, captured traffic, binaries,
   personal identifiers, or claims of investment performance.

The accounting rule is intentionally strict: only confirmed executions change
position or cash. Acknowledgements and lifecycle events are not fills.
