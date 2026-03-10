# Kernel DSL Specification (v1.1)

**Version:** 1.1
**Purpose:** A reusable **modeling language** for describing domain structure, lifecycle behavior, service surfaces, and architecture constraints across projects (e.g., Frames, Dynamic UI, Notes).

## 1. Overview

Kernel DSL is a small, domain-agnostic **language engineering artifact**:
- a **concrete syntax** (text DSL),
- a defined **metamodel (M2)** (Domain, Model, State, Transition, …),
- and recommended **transformations** that map models to code/tests/docs.

It is designed to be:
- human-readable in `.dsl` files and code-reviewable,
- easy to parse (small grammar, suitable for tools like ANTLR or lark),
- domain-independent,
- suitable as an **authoritative model (M1)** from which **derived artifacts** are generated.

### What it models (multi-view)
- **Structure view:** domain, level, model, fields
- **Behavior view:** states, transitions, invariants (well-formedness rules)
- **Reactivity view:** emitted events and semantic actions
- **Service view:** service/operation declarations
- **Governance view:** constraints (architecture rules)

---

## 2. Levels (M0–M3 ground rules)

To understand where this DSL fits into Model-Driven Engineering (MDE):
- **M0:** runtime instances (objects/events/executions)
- **M1:** DSL models (your `.dsl` files)
- **M2:** Kernel DSL metamodel (concepts like Model/State/Transition)
- **M3:** meta-metamodel (generic language-definition constructs; kept implicit)

This document defines the Kernel DSL at **M2** (the language you use to write M1 models).

---

## 3. Metamodel (M2): Core Concepts

### 3.1 Domain
A defined bounded context or logical project container.

```kotlin
domain Notes {
    ...
}
```

### 3.2 Level
A sub-namespace that separates concerns by abstraction boundary (e.g., domain, application, infrastructure).
*Note: levels also act as **generation scopes** (e.g., domain model generation vs service layer generation).*

```kotlin
domain Notes {
    level domain {
        ...
    }
    level application {
        ...
    }
}
```

### 3.3 Model
A named classifier representing a domain entity or value object. It can define:
- **fields** (structural features and their types as free-form strings),
- optional **state machine** (lifecycle states + transitions),
- optional **invariants** (semantic constraints / well‑formedness rules).

```kotlin
model Note {
    description "Represents a user note."
    fields {
        id: NoteId
        title: String
        state: NoteState
    }
    states { Draft, Published }
}
```

### 3.4 State Machine (Transitions)
- **states** define the lifecycle values
- **transitions** define allowed state changes and triggers

A transition may include:
- **trigger**: operation name (+ parameters)
- **guard**: boolean condition expression (`if`)
- **else target**: alternative target state (`else`)
- **emitted events**: event names (`emits`)
- **actions**: semantic actions/effects (`do`)

```kotlin
transitions {
    Sent -> Paid on receivePayment(amount)
        if amount > 0
        emits InvoicePaid, LedgerUpdated
        do updateLedger, notifyCustomer

    Sent -> Error on receivePayment(amount)
        if amount <= 0
        emits PaymentFailed
        do logError
}
```

### 3.5 Service & Operation
A `service` groups externally visible operations. Operations may correspond to transition triggers but are not required to.

```kotlin
service NoteService {
    description "Application service for manipulating notes."
    operation createNote(title, body)
    operation publishNote(noteId)
}
```

### 3.6 Constraints
Constraints allow declaring simple architecture governance rules. Interpretation and enforcement are performed by external validators.

```kotlin
constraints {
    forbid imports from package notes.ui.*
    allow imports from domain Core
}
```

---

## 4. Formal Grammar (EBNF)

Below is the concrete syntax grammar.

```ebnf
start           = domain+

domain          = "domain" NAME "{" domain_body "}"
domain_body     = (description | level | constraints)*

description     = "description" STRING

level           = "level" NAME "{" level_body+ "}"
level_body      = (model | service)+

model           = "model" NAME "{" model_body+ "}"
model_body      = (description | fields | states | transitions | invariants)+

fields          = "fields" "{" field+ "}"
field           = NAME ":" TYPE

TYPE            = /[A-Za-z0-9_<>,.\[\] ]+/

states          = "states" "{" NAME+ "}"
transitions     = "transitions" "{" transition+ "}"
transition      = NAME "->" NAME "on" NAME ("(" param_list? ")")?
                  [ "if" condition_expr ]
                  [ "else" NAME ]
                  [ "emits" name_list ]
                  [ "do" name_list ]

param_list      = NAME ("," NAME)*

name_list       = NAME ("," NAME)*

condition_expr  = /[^{}\n]+/

invariants      = "invariants" "{" invariant+ "}"
invariant       = /[^}]+/

service         = "service" NAME "{" service_body+ "}"
service_body    = (description | operation)+

operation       = "operation" NAME ("(" param_list? ")")?

constraints     = "constraints" "{" constraint+ "}"
constraint      = ("forbid" | "allow") "imports" "from" "package" PATTERN

PATTERN         = /[A-Za-z0-9_.*]+/

NAME            = /[A-Za-z_][A-Za-z0-9_]*/
STRING          = ESCAPED_STRING

%import common.ESCAPED_STRING
%import common.WS
%ignore WS
```

*Notes: `TYPE` and `condition_expr` are intentionally permissive and domain-agnostic. You decide how to interpret types and conditions in your generators.*

---

## 5. Semantics (Pragmatic): Transformations

This DSL becomes executable through **meta-programs** (generators). The following are the recommended Model-to-Text (M2T) generation mappings.

### 5.1 Model → Java/Code (M2T)
- Generate a State `enum`.
- Generate the Model class with fields as attributes.
- Generate transition methods corresponding to triggers handling guard assertions, state updates, and adding events.
- Generate Event classes for each `emits`.
- Generate action stubs/hooks for each `do`.

### 5.2 Model/Service → Tests (M2T)
- Generate branch tests for every `transition` (positive path verifying target state and events).
- Generate branch tests for `guard` (if) / `else` combinations.

### 5.3 Model → Documentation (M2T)
- Generate Markdown summaries of domains and models.
- Generate Mermaid state diagrams mapping visual lifecycles.

---

## 6. Well-Formedness Rules

Even if invariants are partially free-form, tools using Kernel DSL should validate at a minimum:
- States referenced in transitions exist in the `states` block.
- Triggers are valid identifiers.
- The `else` target state exists (if used).
- No transitions emanate from declared final states (if finality is defined).
- A `guard` present implies branch coverage tests are required.

---

## 7. Versioning and Evolution

- This document defines Kernel DSL v1.1.
- Future, backward-compatible grammar additions will be tracked as minor versions (v1.2, v1.3).
- Breaking syntactic changes will require a v2.x version.

**Guidelines for Extending:**
- **Do not** bake domain-specific concepts (e.g., "Frame", "UIElement") into this grammar.
- Prefer modeling new ideas using existing constructs.
- If syntax is added, keep it completely orthogonal and verify it across multiple domains before integrating it into Kernel DSL.
