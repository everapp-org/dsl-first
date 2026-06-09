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

Unlike traditional spec-driven or code-first approaches, the model is executable — once authored, it drives **derivation** that deterministically produces type-safe code and exhaustive tests: a parser plus code generator in a grammar-hosted binding, or a schema plus interpreter in a data-hosted one.

**Key Insight**: When the model DRIVES derivation — generator or interpreter — specification drift becomes physically impossible.

## The Pillars of DSL First

1. **Language as Architecture**: Instead of the system being merely classes or services, a targeted language focused on the business domain becomes the architecture — the AI authors it, you own and steer it.
2. **Meta-programming First**: Using code to write code. The model acts as the source of truth, and generators or interpreters handle the heavy lifting of translating that intention into a running system.
3. **High Cohesion**: Changes to business logic happen in the model, not in the boilerplate.
4. **Drift-Free by Design**: Documentation and tests are generated *from* the model. If the model changes, everything downstream updates or breaks at compile time.
