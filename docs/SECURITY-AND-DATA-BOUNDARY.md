# Security and Data Boundary

## Publication model

This repository is a sanitised, clean-room public account. It is designed to communicate engineering decisions without redistributing restricted environment material or personal information.

The repository may contain authored documentation, generic examples, tests, and audit tooling. It intentionally excludes supplied task materials, environment implementation assets, sensitive captures, protected datasets, submission records, personal identifiers, credentials, and deployment-specific artefacts.

## Data-handling principles

- **Minimise disclosure.** Keep only information needed to explain an engineering conclusion.
- **Aggregate before publication.** Prefer counts, ranges, and bounded metrics over identity-bearing event details.
- **Separate evidence from publicity.** Internal evidence may support a conclusion without being copied into a public repository.
- **No credentials in examples.** Configuration examples must not embed secrets, access tokens, passwords, private keys, or personal contact information.
- **Do not imply permission through possession.** Availability of a file or endpoint does not make it suitable for publication.

## Sanitisation rules

Public prose and examples must not include:

- personal names, email addresses, local filesystem locations, or submission destinations;
- account identities, message subjects, instrument identifiers, order identifiers, or exact environment endpoints;
- unfiltered event captures, replay files, binary images, generated build outputs, or large data artefacts;
- protected evaluation terminology or private evaluation results; or
- credentials, connection strings containing authentication material, or secrets in any form.

Generic descriptions such as “message-oriented boundary,” “controlled market environment,” and “component-scoped event” are preferred over environment-specific literals.

## Publication audit

The repository includes a publication audit that rejects prohibited paths, binary or oversized material, local absolute paths, email addresses, protected-dataset terminology, upload-oriented links, and credential-shaped assignments. It is a guardrail, not a complete security review.

Before publication:

1. run the audit and its automated tests;
2. inspect every changed document for identity-bearing details;
3. confirm that all performance language remains negative/inconclusive where required; and
4. treat an audit failure as a release blocker until the material is removed or properly generalised.

## Responsible interpretation

Safety claims here are constrained to the documented controlled validation. They do not establish suitability for a live venue, legal compliance, investment suitability, security certification, or future financial performance.
