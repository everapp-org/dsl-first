# The DSL First Manifesto

> **Origin**: Part of the [DSL-First Methodology](https://github.com/everapp-org/dsl-first) open-source project.  
> **License**: Apache 2.0 — see [LICENSE](../LICENSE)

## The Problem

In traditional software development, the specification, the code, the tests, and the documentation are all separate artifacts. Over time, they *will* drift apart. It's inevitable.

*   **Spec-Driven:** Natural language is ambiguous and invariably drifts from the code.
*   **Code-First:** The code is the truth, but it’s hard for stakeholders to read. Tests and docs are treated as afterthoughts.
*   **Doc-First:** Without automatic verification, documentation quickly becomes stale.

**All traditional approaches suffer from the same problem: Multiple sources of truth that can diverge.**

## Our Belief

We believe there should be only **ONE source of truth**: a single document from which everything else — the code, the tests, the documentation — is produced.

**DSL-First Development** is a software development methodology where that single source is a **model**: a precise description of the application, written in a **Domain-Specific Language (DSL)** — a small language built specifically for describing one kind of thing.

Unlike a traditional specification, the model is not just a description for humans to read — programs read it too, and they produce the application code and the matching tests from it. The same model always produces the same output. (The exact machinery differs by host language; the guide's *[Two Bindings](../dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md#25-two-bindings-grammar-hosted-and-data-hosted)* section describes both styles.)

**Key Insight**: When the code and the tests are *produced from* the model rather than written next to it, they cannot drift away from it — there is nothing left to drift.

## The Pillars of DSL First

1. **Language as Architecture**: Instead of the system being merely classes or services, a targeted language focused on the business domain becomes the architecture — the AI authors it, you own and steer it.
2. **Meta-programming First**: Using code to write code. The model acts as the source of truth, and generators or interpreters handle the heavy lifting of translating that intention into a running system.
3. **High Cohesion**: Changes to business logic happen in the model, not in the boilerplate.
4. **Drift-Free by Design**: Documentation and tests are generated *from* the model. If the model changes, everything downstream updates or breaks at compile time.
