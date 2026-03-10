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

We believe there should only be **ONE source of truth**: an executable specification.

**DSL-First Development** is a software development methodology where a **Domain-Specific Language (DSL)** serves as that single source. 

Unlike traditional spec-driven or code-first approaches, the DSL is executable — once crafted, it drives a grammar and a code generator that deterministically produce type-safe code and exhaustive tests.

**Key Insight**: When your specification (DSL) DRIVES your code generator, specification drift becomes physically impossible.

## The Pillars of DSL First

1. **Language as Architecture**: Instead of merely building classes or services, you build a targeted language entirely focused on the business domain.
2. **Meta-programming First**: Using code to write code. The DSL acts as the source of truth, and generators or interpreters handle the heavy lifting of translating that intention into a running system.
3. **High Cohesion**: Changes to business logic happen in the DSL, not in the boilerplate.
4. **Drift-Free by Design**: Documentation and tests are generated *from* the DSL. If the DSL changes, everything downstream updates or breaks at compile time.
