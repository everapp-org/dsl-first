# Kernel DSL Specification (v1.2)

> **Origin**: Part of the [DSL-First Methodology](https://github.com/everapp-org/dsl-first) open-source project.  
> **License**: Apache 2.0 — see [LICENSE](../LICENSE)

**Version:** 1.2
**Purpose:** This document defines the **Kernel DSL** — a language an AI coding assistant uses to write a **model** of an application: the things the application manages, the states each thing can be in, which state changes are allowed, the operations the application offers, and the structural rules its codebase must respect. One language, reusable across projects and host languages.

## TL;DR — this file can be used alone

**What it gives you.** One small language to write down what an application manages: the things, their fields, their states, the allowed state changes, the operations it offers, what joins one operation to another, and what is declared and not built yet.

**Prerequisites: none.** You do not need the other methodology files to write or to read a Kernel model. Every term this file uses is defined in this file.

**Use it now.** Give this file to your AI coding assistant with one of these prompts.

| you want to | say to the assistant |
|---|---|
| model a new application | *"Read KERNEL_DSL.md. Write a Kernel DSL model of this specification. Show me the model before you write code."* |
| understand existing code | *"Read KERNEL_DSL.md. Read this codebase and write the Kernel DSL model that it implements. List what you could not decide."* |
| record what is promised and not built | *"Give every operation of the model a status. For each `not-built` operation, write the reason."* (§3.7) |
| stop defects in the joins between operations | *"Declare a wiring for each place where the result of one operation becomes the input of another. Tell me where the two types differ."* (§3.8) |

**The whole language in one example.**

```kotlin
domain Tasks {
    level application {
        model Task {
            fields {
                id: Int
                title: String
            }
            states { Open, Done }
            transitions {
                Open -> Done on complete emits TaskCompleted
            }
        }
        service TaskService {
            operation listTasks returns many (task_id: Int, title: String)
            operation completeTask(task_id: Int) returns (task_id: Int, done: Bool)
            operation archiveTask(task_id: Int)
                status not-built reason "No decision yet on how long an archived task is kept."
        }
    }
    wiring completeFromList {
        from TaskService.listTasks.task_id
        to   TaskService.completeTask.task_id
    }
}
```

**What is new in v1.2**: typed parameters and a declared result on an operation (§3.5), a status on an operation (§3.7), and the `wiring` element (§3.8). Every v1.1 model is a valid v1.2 model.

## 1. Overview

Kernel DSL is a small, domain-agnostic **metamodel (M2)** — Domain, Level, Model, State, Transition, … — together with recommended **transformations** that map models to code/tests/docs. The metamodel is the portable part; it can be carried in more than one **concrete syntax**:
- a **text grammar** (`.dsl` files parsed by ANTLR, lark, a PEG, …) — the *grammar-hosted* binding, and the syntax §4 specifies;
- a **data literal** (EDN / maps validated by a schema) — the *data-hosted* binding used in homoiconic languages like Clojure.

It is designed to be:
- human-readable and code-reviewable in whichever concrete syntax you choose,
- domain-independent,
- suitable as an **authoritative model (M1)** from which **derived artifacts** are generated.

### What it models (multi-view)
- **Structure view:** domain, level, model, fields
- **Behavior view:** states, transitions, invariants (well-formedness rules)
- **Reactivity view:** emitted events and semantic actions
- **Service view:** service/operation declarations, what each operation receives and returns, and its status (built, or declared and not built)
- **Wiring view:** the joins between operations — a value that one operation returns and another receives
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

*Since v1.2.* A parameter may carry a type, and an operation may declare what it returns. `returns (...)` is one result with those fields. `returns many (...)` is a list, and each row has those fields.

```kotlin
service NoteService {
    operation createNote(title: String, body: String) returns (note_id: NoteId)
    operation listNotes returns many (note_id: NoteId, title: String)
    operation publishNote(note_id: NoteId) returns (note_id: NoteId, state: NoteState)
}
```

Both additions are optional. An operation with no types and no `returns` is valid, as in v1.1. But an operation with no declared result cannot be the source of a wiring (§3.8), because there is nothing to compare.

The order is fixed: the parameters, then `returns`, then `status` (§3.7). `operation a status ready returns (x: Int)` is not valid.

A type in a parameter list is one word with no space and no comma (`NoteId`, `List<Note>`, `String[]`). Give a longer type a name, and use the name.

### 3.6 Constraints
Constraints allow declaring simple architecture governance rules. Interpretation and enforcement are performed by external validators.

```kotlin
constraints {
    forbid imports from package notes.ui.*
    allow imports from domain Core
}
```

### 3.7 Status: built, or declared and not built
*Since v1.2.*

An operation has a **status**. The set is closed.

| status | class | meaning |
|---|---|---|
| `ready` | built | The operation is implemented. This is the default when no status is written. |
| `in-progress` | built | An implementation exists and is not complete. A caller can call the operation. |
| `not-built` | not built | The operation is declared, and nothing implements it. A `reason` is required. |

```kotlin
service NoteService {
    operation publishNote(note_id: NoteId)
    operation archiveNote(note_id: NoteId)
        status not-built reason "No decision yet on how long an archived note is kept."
}
```

Why this is in the language: a product promises more than it does. Without a status, that fact lives in a comment, in a disabled control, or nowhere. With a status, the question *"what does the product promise that it does not do?"* has an answer that a program can list. And a caller of a `not-built` operation gets the reason, not an "unknown operation" error.

A project may refine the *built* class with more names (for example `in-test`). It must keep the set closed, and it must keep the two classes. `not-built` and its required reason are not optional.

### 3.8 Wiring: the joins between operations
*Since v1.2.*

A model that lists only operations describes the **nodes** of a system. Many defects are in the **edges**: one operation returns a value, another operation receives it, and the two do not agree on the type. Each side is correct alone, each side has its own tests, and the defect is found by a user.

A `wiring` declares one such edge. Both ends are then in the model, and a program can compare them.

```kotlin
domain Notes {
    level application {
        service NoteService {
            operation listNotes returns many (note_id: NoteId, title: String)
            operation publishNote(note_id: NoteId)
        }
    }
    wiring publishFromList {
        description "The list row gives the id of the note to publish."
        from NoteService.listNotes.note_id
        to   NoteService.publishNote.note_id
    }
}
```

- `from` names `Service.operation.field`: a field of what the operation **returns**.
- `to` names `Service.operation.parameter`: a parameter that the other operation **receives**.
- A wiring is declared in the domain, not in a level, because an edge can cross levels.
- A wiring between two domains is not in v1.2.

Declare a wiring where a person or a program really carries the value across: a row of a list that becomes the argument of a command, the result of one step that becomes the input of the next. Do not declare every pair of operations that have a field of the same name.

---

## 4. Formal Grammar (EBNF)

Below is the concrete syntax grammar.

```ebnf
start           = domain+

domain          = "domain" NAME "{" domain_body "}"
domain_body     = (description | level | constraints | wiring)*

description     = "description" STRING

level           = "level" NAME "{" level_body+ "}"
level_body      = (model | service)+

model           = "model" NAME "{" model_body+ "}"
model_body      = (description | fields | states | transitions | invariants)+

fields          = "fields" "{" field+ "}"
field           = NAME ":" TYPE

TYPE            = /[A-Za-z0-9_<>,.\[\] ]+/

states          = "states" "{" NAME ([","] NAME)* "}"
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

operation       = "operation" NAME ("(" typed_param_list? ")")?
                  [ "returns" ["many"] "(" typed_param_list ")" ]
                  [ "status" STATUS [ "reason" STRING ] ]

typed_param_list = typed_param ("," typed_param)*
typed_param     = NAME [":" PARAM_TYPE]

PARAM_TYPE      = /[A-Za-z0-9_.<>\[\]]+/
STATUS          = "ready" | "in-progress" | "not-built"

wiring          = "wiring" NAME "{" [description] "from" endpoint "to" endpoint "}"
endpoint        = NAME "." NAME "." NAME

constraints     = "constraints" "{" constraint+ "}"
constraint      = ("forbid" | "allow") "imports" "from" ("package" PATTERN | "domain" NAME)

PATTERN         = /[A-Za-z0-9_.*]+/

NAME            = /[A-Za-z_][A-Za-z0-9_]*/
STRING          = ESCAPED_STRING

%import common.ESCAPED_STRING
%import common.WS
%ignore WS
```

*Notes: `TYPE` and `condition_expr` are intentionally permissive and domain-agnostic. You decide how to interpret types and conditions in your generators. `PARAM_TYPE` (v1.2) is narrower than `TYPE`: it has no space and no comma, because a comma separates parameters. The words `returns`, `many`, `status`, `reason`, `wiring`, `from` and `to` are keywords in the positions the grammar gives them.*

### 4.1 The data-hosted concrete syntax

§4 specifies the **grammar-hosted** concrete syntax. In a **data-hosted** binding (Clojure, Lisp) the same metamodel is carried as a data literal and validated by a *schema* instead of a grammar — there is no parser. The `Note` model from §3.3 becomes:

```clojure
{:domain :notes
 :levels {:domain {:models
   {:note {:fields {:id :NoteId :title :String :state :NoteState}
           :states #{:draft :published}
           :transitions [[:draft :published :on :publish :emits [:note-published]]]}}}}}
```

Both syntaxes express the identical M2. See the [Clojure example](../examples/clojure-edn-minimal/README.md) and §2.5 of the [Methodology Guide](DSL_FIRST_METHODOLOGY_GUIDE.md).

The data-hosted binding is not only for Lisps. Any data format with a standard reader can carry the model. The v1.2 elements, as the two runnable examples [`python-toml-gates`](../examples/python-toml-gates/README.md) (TOML) and [`java-yaml-gates`](../examples/java-yaml-gates/README.md) (YAML) write them:

| Kernel DSL v1.2 | data-hosted key in the two examples |
|---|---|
| a typed parameter list | `payload`, a map of field to type |
| `returns (...)` | `result`, a map of field to type |
| `returns many (...)` | `result_item`, a map of field to type |
| `status ready \| in-progress \| not-built` | `status` |
| `reason "..."` | `reason` |
| `wiring N { from S.op.f to S.op.p }` | one entry of `wirings`, with `name`, `from`, `field`, `to`, `payload_field` |

```yaml
commands:
  archive:
    status: not-built
    reason: No decision yet on how long an archived task is kept.
    payload: {task_id: int}

wirings:
  - name: complete-from-list
    from: tasks.list
    field: task_id
    to: tasks.complete
    payload_field: task_id
```

In those examples a *context* has the role of the service, and commands and queries are its operations.

---

## 5. Semantics (Pragmatic): Transformations

This DSL becomes executable through **meta-programs** (generators) in grammar-hosted bindings, or by **interpreting / macro-expanding the data** in data-hosted bindings. The following are the recommended Model-to-artifact mappings.

### 5.1 Model → Code (M2T)
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

### 5.4 Status → Runtime and Documentation
*Since v1.2.*
- Wire only the operations of the *built* class.
- Keep a `not-built` operation in the catalogue of the application. A call to it is refused with its `reason`. A call to a name that is not declared is refused as unknown. The two answers are different.
- Generate a list "declared and not built" in the documentation, each entry with its reason.
- Do not generate a skipped test for a `not-built` operation. Generate a test that the application refuses it with its reason.

### 5.5 Wiring → Checks and Tests
*Since v1.2.*
- Generate, or write once, a check that compares the two declared types of every wiring and fails the build when they differ.
- In a statically typed host, you can generate a typed adapter for each wiring instead. The compiler then refuses a wiring whose ends disagree.
- Generate one integration test for each wiring: call the `from` operation, take the field, and give it to the `to` operation.

---

## 6. Well-Formedness Rules

Even if invariants are partially free-form, tools using Kernel DSL should validate at a minimum:
- States referenced in transitions exist in the `states` block.
- Triggers are valid identifiers.
- The `else` target state exists (if used).
- No transitions emanate from declared final states (if finality is defined).
- A `guard` present implies branch coverage tests are required.

Since v1.2:
- A status is one of the closed set.
- A `not-built` operation has a non-empty `reason`. An operation of the built class has no `reason`.
- Each endpoint of a wiring resolves to exactly one operation of the domain.
- The `from` operation declares `returns`, and the named field is in it.
- The named parameter of the `to` operation exists.
- Both ends of a wiring have a declared type, and the two types are equal.
- Wiring names are unique in a domain.

---

## 7. Versioning and Evolution

- This document defines Kernel DSL v1.2.
- Backward-compatible grammar additions are tracked as minor versions (v1.2, v1.3).
- Breaking syntactic changes will require a v2.x version.

**Guidelines for Extending:**
- **Do not** bake domain-specific concepts (e.g., "Frame", "UIElement") into this grammar.
- Prefer modeling new ideas using existing constructs.
- If syntax is added, keep it completely orthogonal and verify it across multiple domains before integrating it into Kernel DSL.

### 7.1 What v1.2 adds

| addition | section | why |
|---|---|---|
| typed parameters and `returns` on an operation | §3.5 | a wiring needs a declared type at both ends |
| `status` and `reason` on an operation | §3.7 | what is promised and not built becomes a fact that a program can list |
| `wiring` | §3.8 | the joins between operations become model elements that a program can check |

**Two corrections of the grammar.** The examples of v1.1 used two forms that the grammar of v1.1 refused. The grammar now accepts them, and it accepts the old forms too.

| example in v1.1 | grammar of v1.1 | grammar of v1.2 |
|---|---|---|
| `states { Draft, Published }` (§3.3) | names with no comma only | a comma between names is optional |
| `allow imports from domain Core` (§3.6) | `from package PATTERN` only | `from package PATTERN` or `from domain NAME` |

**Compatibility.** Every v1.1 model is a valid v1.2 model with the same meaning. An operation with no status is `ready`. No v1.1 construct was removed or changed in meaning.

**How the guideline "verify across multiple domains" was met, and where it was not.** The three additions come from one production codebase that uses the data-hosted binding, with about thirty context manifests. They are implemented in two runnable examples of this repository, which share one small domain. A check builds a parser from the grammar of §4 and reads every example of this file, with valid and invalid models (`tools/check_kernel_grammar.py`). That check found the two corrections above. No generator in this repository reads the v1.2 text syntax yet: the example `java-antlr-minimal` has its own smaller grammar.
