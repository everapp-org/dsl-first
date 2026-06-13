# Verification DSL Specification (v1.0)

> **Origin**: Part of the [DSL-First Methodology](https://github.com/everapp-org/dsl-first) open-source project.  
> **License**: Apache 2.0 — see [LICENSE](../LICENSE)

**Version:** 1.0  
**Companions:** [Kernel DSL Specification v1.1](KERNEL_DSL.md) · [Behavior DSL Specification v1.0](BEHAVIOR_DSL.md)  
**Purpose:** This document defines the **Verification DSL** — a language an AI coding assistant uses to write a **model** of what must hold: the checkable claims a system makes about itself. Each claim invokes one of the same fixed set of operations the host application provides, observes the result, and asserts on it. From the model, two artifacts are derived: a runnable check suite, and **living documentation** — prose a newcomer can read, in which each claim is rendered with its current verdict.

---

## 1. Overview

The Verification DSL is the third member of the DSL-First language family. Where the **Kernel DSL** models *what things are* — domain structure and lifecycle — and the **Behavior DSL** models *how things happen* — the shape of a procedure — the **Verification DSL** models *what must hold*: the claims the system can be held to, and how to check each one.

It exists because verification is a distinct modeling concern. A claim reuses the Behavior DSL's execution shape for its setup and action — calls to host operations, with results bound to names — but adds a construct neither sibling has: an **expectation** matched against an operation's result, producing a **verdict**. Asserting on a result is foreign to the Kernel DSL's declarative world and to the Behavior DSL, whose procedures run but never judge their own output.

A verification model has two layers:

- **Check layer** — named claims, each a small script: optional setup steps, the operation under test, and an expectation on its result.
- **Documentation layer** — prose that *references checks by name* and reads as ordinary product documentation. Running the model re-renders the same prose with each referenced claim marked **PASS**, **FAIL**, **INVALID**, or **UNVERIFIED**. This is **living documentation**: the document is the test report, and the test report is the document. (The idea is inspired by [Concordion](https://concordion.org); the DSL-First contribution is that the AI authors the checks from the model — the documentation never falls out of sync by hand.)

The check layer is intentionally minimal — **not Turing-complete**. There is no branching and no looping: a claim that needs control flow is two claims, or it is test logic that belongs outside the documentation. Every construct maps to a host-operation call or an expectation, so the model compiles deterministically to a check runner.

### What the Verification DSL models

| View | Concern |
|------|---------|
| **Verification** | A named group of claims about one part of the system |
| **Check** | One claim: setup → operation under test → expectation, yielding a verdict |
| **Given** | Setup steps that establish the world a claim needs, binding results to names |
| **Expect** | The assertion — a partial match against the operation's result |
| **Verdict** | The runtime outcome: PASS / FAIL / INVALID / UNVERIFIED |
| **Instrumentation** | The link in prose (`verify://CheckName`) that ties a documented claim to its check |

> **Not a test framework.** Unit and integration tests live in the host project's test suite and may assert anything in any language. The Verification DSL models only the claims a *document* makes — the subset of behavior a newcomer reads about — so that documentation can prove itself. Its deliberate weakness (no control flow) is what keeps a claim legible as a sentence.

---

## 2. Levels (M0–M3)

| Level | What it is | Example |
|-------|-----------|---------|
| **M0** | A check running — an operation invoked against a fresh world, its result judged, a verdict produced; a document re-rendered with verdicts | An `OrderLifecycle` report showing six green claims and one pending |
| **M1** | This file's instances: `verification OrderLifecycle { ... }` + the prose that links its checks | `order-lifecycle.verify`, `USER_GUIDE.md` |
| **M2** | Verification metamodel: `Verification`, `Check`, `Given`, `Expect`, `Verdict`, `Instrumentation` | This specification |
| **M3** | Generic language-definition constructs (kept implicit) | |

**Relationship to the other DSLs:** all three operate at M2 for separate concerns and produce M1 artifacts. They share a build toolchain and a single **host-operation contract** — the fixed set of named operations the runtime exposes. The Behavior DSL *calls* those operations to do work; the Verification DSL *calls* them to make claims and then judges the result. Independent metamodels, one runtime surface.

---

## 3. Metamodel (M2): Core Concepts

### 3.1 Verification

The top-level named classifier. Groups the claims about one part of the system.

```
verification OrderLifecycle {
    description "What a placed order can and cannot do."
    ...
}
```

Attributes:
- `name` — identifier; drives the generated suite name (`OrderLifecycle` → `OrderLifecycleChecks`)
- `description` (optional) — human-readable documentation

Body: one or more `check` blocks.

### 3.2 Check

One claim. Three parts, in order:

| Part | Syntax | Meaning |
|------|--------|---------|
| Given *(optional)* | `given order = placeOrder(items)` | Setup steps; each calls a host operation and may bind its result to a name |
| When *(required)* | `when paid = pay(order)` | The single operation under test; its result is the subject of the expectation |
| Expect *(required)* | `expect paid.status == "paid"` | A partial match against the result — every stated field must match; unstated fields are ignored |

```
check OrderCanBePaid {
    given  order = placeOrder(sampleItems)
    when   paid  = pay(order)
    expect paid.status == "paid"
}
```

**Setup steps assert nothing.** A `given` step that fails (its operation errors) fails the whole check at the setup stage — but it is not the claim. Only `expect` makes the claim. A check that needs to assert on an intermediate result is two checks.

**The callable operation set is the host-operation contract** — the same one the Behavior DSL calls. Domain logic is never embedded in a check; it is referenced by name. This keeps the language general: a check models only *which* operations a claim exercises and *what* their result must be.

### 3.3 Given — setup and binding

Each `given` step is a host-operation call, optionally binding its result to a name with `=`. Bound names are in scope for later steps, the `when`, and the `expect`. There is no other state.

```
check MainLineIsProjected {
    given root  = branch(topic, "Which store?")
    given choice = branch(root, "Use the relational store")
    given        markMainLine(choice)
    when  tree  = readTree(topic)
    expect tree.mainLine == choice
}
```

A runtime may provide a small set of **ambient bindings** the host injects into every check (for example, a fresh scratch directory). These are declared by the runtime, not by the check.

### 3.4 Expect — partial match

`expect` is a conjunction of field assertions against the `when` result. The match is **partial (a submatch)**: every stated path must equal the corresponding value in the result; fields the expectation does not mention are ignored. This keeps claims stable as results gain fields.

```
expect paid.status == "paid"
       paid.amount == order.total
```

Nested paths (`result.items[0].sku`) and equality against bound names (`== order.total`) are the only operators. There is no negation, comparison, or boolean logic — a claim that needs them is decomposed or is not a documentation claim.

### 3.5 Verdict — the runtime contract

Running a check yields exactly one verdict. The four-way taxonomy is the language's central contribution: it separates *the system is wrong* from *the claim's instrumentation is wrong* from *the claim is not yet backed*. Collapsing these is how living documentation loses trust.

| Verdict | Meaning | Owner |
|---------|---------|-------|
| **PASS** | The check ran; the expectation held | — |
| **FAIL** | The check ran; the expectation did not hold (or a setup step errored). The system does not honor the claim | The code, or the claim |
| **INVALID** | The check itself is malformed — names an operation the host does not expose, or asserts a field the operation never returns. A lint error, **never rendered as failure** | The check author |
| **UNVERIFIED** | The claim is documented but not yet backed by a runnable check (its body is marked pending) | The backlog |

A FAIL means *fix the code or fix the claim*. An INVALID means *fix the check*. An UNVERIFIED is *a work item*, carrying a note on what it is blocked on. Only FAIL is red.

### 3.6 Instrumentation — claims in prose

The documentation layer links a sentence to a check by **name**, never by embedding the check body in the prose. The reference is a link on a reserved scheme:

```
A placed order [can be paid](verify://OrderCanBePaid) once the customer
confirms; a [declined card cancels the order](verify://PaymentFailureCancels).
```

The linked **phrase** is the claim; the check body lives in the verification model. Three consequences follow:

- **The document carries names, not test code.** It reads as documentation. A reader who ignores the links reads ordinary prose.
- **A check name is evaluated once per document run**, and every sentence linking it shares the verdict. Reuse is one execution, many claims.
- **Parameterization:** a check may declare that it receives the linked phrase as an input. It is then evaluated once per distinct phrase — the prose the reader reads *is* the test datum. Two sentences linking the same parameterized check with different phrases get their own verdicts.

References inside code samples are quotation, not instrumentation, and are ignored — a document may show the syntax without invoking it.

---

## 4. Formal Grammar (EBNF)

```ebnf
start              = verification+

verification       = "verification" NAME "{" verification_body+ "}"
verification_body  = description
                   | check_block

description        = "description" STRING

check_block        = "check" NAME parameter? "{" given_stmt* when_stmt expect_stmt "}"
parameter          = "(" NAME ")"               (* receives the linked phrase *)

given_stmt         = "given" ( NAME "=" )? call
when_stmt          = "when"  ( NAME "=" )? call
expect_stmt        = "expect" assertion+

call               = NAME "(" arg_list? ")"
arg_list           = expr ("," expr)*

assertion          = path "==" value
path               = NAME ( "." NAME | "[" INTEGER "]" )*
value              = STRING | INTEGER | BOOLEAN | path
expr               = STRING | INTEGER | BOOLEAN | path

NAME               = /[A-Za-z_][A-Za-z0-9_]*/
STRING             = ESCAPED_STRING
INTEGER            = /[0-9]+/
BOOLEAN            = "true" | "false"

%import common.ESCAPED_STRING
%import common.WS
%ignore WS
COMMENT            : "//" /[^\n]*/
%ignore COMMENT
```

The documentation layer is ordinary prose (Markdown or any host format); its only DSL element is the link grammar `[PHRASE](verify://NAME)`, recognized everywhere except inside code spans and code blocks.

---

## 5. Semantics: Transformations (M2T)

### 5.1 Verification → Check Suite

For each `verification X`, generate a runner `XChecks`. For each `check C`, generate one check function that, against a **fresh world**, executes the `given` steps in order (binding their results), invokes the `when` operation, and partial-matches the `expect` assertions — returning a verdict.

```
// Generated from: verification OrderLifecycle { check OrderCanBePaid { ... } }
// GENERATED CODE - DO NOT EDIT
checkOrderCanBePaid(host):
    world = host.freshWorld()
    order = world.call("placeOrder", sampleItems)     // given
    paid  = world.call("pay", order)                  // when
    return submatch({status: "paid"}, paid)           // expect → PASS | FAIL
            ? PASS : FAIL(expected, paid)
```

Each check runs in an isolated world so checks cannot interfere; a check name is memoized within one document run.

### 5.2 Expect → Submatch

The expectation compiles to a recursive partial-match: maps match key-by-key (recursing), sequences match element-wise, leaves match by equality. Bound names resolve against the check's environment before matching.

### 5.3 Documentation → Verdict Report

The documentation layer compiles to a **projection**, not a rewrite: the source prose is never modified. The renderer parses the document, resolves each `verify://NAME` link to its check's verdict, and emits the same prose with each linked phrase decorated by its verdict — to a terminal, to HTML, and to a machine-readable feed. Every generated report carries provenance: when it was produced and by which revision of the runner.

### 5.4 Lint → INVALID

Before any check executes, each is linted against the host-operation contract: every referenced operation must exist, and every `expect` path must be a field the operation's declared result can carry. A check that fails the lint is reported INVALID and is **not executed** — broken instrumentation never masquerades as a system failure.

---

## 6. Well-Formedness Rules

- Each `check` has exactly one `when` and exactly one `expect`; `given` steps are zero or more.
- No control flow. `given`/`when`/`expect` are the only statements; there is no conditional, loop, or boolean operator. A validator must reject control-flow constructs, not warn.
- Names bound in `given`/`when` must be defined before use; `expect` may reference any bound name, the `when` result, or an ambient runtime binding.
- Every operation named in a `call` must belong to the host-operation contract (else INVALID at runtime).
- A `check` declaring a `parameter` must reference it; the runtime supplies the linked phrase as that parameter's value.
- Documentation links must name a declared check; a link to an unknown name is INVALID, a link to a pending check is UNVERIFIED.
- A claim asserted in prose but with no `check` is UNVERIFIED, not absent — the document's coverage is itself observable.

---

## 7. Example: A Verification Model (Kernel-domain claims)

Claims about the order lifecycle modeled in the Kernel DSL — that a placed order can be paid, and that a declined payment cancels it.

```dsl
verification OrderLifecycle {
    description "What a placed order can and cannot do."

    check OrderCanBePaid {
        given  order = placeOrder(sampleItems)
        when   paid  = pay(order)
        expect paid.status == "paid"
               paid.amount == order.total
    }

    check PaymentFailureCancels {
        given  order  = placeOrder(sampleItems)
        when   result = pay(order, declinedCard)
        expect result.status == "failed"
               order.status  == "cancelled"
    }

    // Parameterized: the documented phrase is the test datum.
    check RefundReasonIsStored(phrase) {
        given  order    = placeOrder(sampleItems)
        given  paid     = pay(order)
        when   refunded = refund(paid, phrase)
        expect refunded.reason == phrase
    }
}
```

The companion documentation:

```markdown
A placed order [can be paid](verify://OrderCanBePaid) once the customer
confirms, and the charge equals the order total. If the card is declined,
the [order is cancelled](verify://PaymentFailureCancels) — no half-paid
state survives.

A refund records its reason verbatim: refunding
[because the item arrived damaged](verify://RefundReasonIsStored) and
refunding [because the customer changed their mind](verify://RefundReasonIsStored)
each store exactly the sentence you just read.
```

Running the model re-renders that prose with `can be paid`, `order is cancelled`, and both refund phrases marked PASS or FAIL — and, because `RefundReasonIsStored` is parameterized, the two refund sentences are checked independently with their own text as input.

---

## 8. Application Example: Self-Proving Product Documentation

The most valuable application is not a separate test document but the project's *actual* documentation made living. A user guide's existing sentences become claims; the guide proves itself on every run.

```markdown
## Sessions

`install()` registers every handler — [a command dispatched after install
returns its event](verify://InstallWiresHandlers). Each prompt is
[appended to the session's history](verify://PromptAppendsToHistory) and
the full history is sent to the model on the next turn.
```

```dsl
verification UserGuide {
    check InstallWiresHandlers {
        given  session = install(freshConfig)
        when   result  = dispatch(session, setModel("fast"))
        expect result.event == "model-changed"
    }

    check PromptAppendsToHistory {
        given  session = install(freshConfig)
        given           dispatch(session, prompt("hello"))
        when   history = readHistory(session)
        expect history[0].content == "hello"
    }
}
```

When `readHistory` is later changed or removed, `PromptAppendsToHistory` turns red *in the documentation* — the stale sentence is visible as a failed claim, not as prose a reader trusts. A documentation page that has drifted from the code does not read poorly; it *fails*. This makes documentation part of the definition of done: a capability is complete when its guide is green.

---

## 9. Versioning and Evolution

- This document defines **Verification DSL v1.0**.
- v1.0 includes: named verifications, checks (`given` / `when` / `expect`), partial-match expectations, the four-way verdict taxonomy, prose instrumentation by link, check reuse, and phrase parameterization.
- Planned v1.x additions: declared **ambient bindings** in the model; declared **result coercions** so a phrase can supply a typed (not only string) argument; a `setup { }` block shared across a verification's checks.
- Breaking grammar changes require v2.0.

**Guidelines for extending:**
- New optional constructs (shared setup, ambient bindings) are additive — v1.x.
- Adding operators to `expect` (comparison, negation) is a **deliberate non-goal**: it erodes the legibility that makes a check a sentence. Resist it; decompose the claim instead.
- Do not bake host-specific operations into this grammar; they belong in the host-operation contract, shared with the Behavior DSL, not in the spec.

---

**Document Version**: 1.0  
**Last Updated**: 2026-06-13  
**License**: Apache 2.0 — https://github.com/everapp-org/dsl-first/blob/main/LICENSE
