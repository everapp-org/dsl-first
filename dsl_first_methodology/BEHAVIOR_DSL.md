# Behavior DSL Specification (v1.0)

> **Origin**: Part of the [DSL-First Methodology](https://github.com/everapp-org/dsl-first) open-source project.  
> **License**: Apache 2.0 — see [LICENSE](../LICENSE)

**Version:** 1.0  
**Companion:** [Kernel DSL Specification v1.1](KERNEL_DSL.md)  
**Purpose:** A modeling language for describing **behaviors** — how a procedure unfolds as an ordered and parallel sequence of steps invoking a fixed runtime API — as M1 models compiled into typed executor classes.

---

## 1. Overview

The Behavior DSL is the second member of the DSL-First language family. Where the **Kernel DSL** models *what things are* — domain structure and lifecycle (entities, fields, state machines) — the **Behavior DSL** models *how things happen* — the imperative shape of a procedure: what runs, in what order, what runs in parallel, and how failures are handled.

It is **general-purpose process modeling**, not tied to any one domain. A document-ingestion pipeline, an order-fulfilment procedure, a deployment routine, and a multi-agent crew strategy are all *behaviors* — they differ only in which runtime API functions they call.

A behavior model has two layers:

- **Declarative configuration layer** — the behavior's name, optional participant constraints, error policies, and concurrency limits.
- **Imperative orchestration layer** — an embedded execution script with assignments, sequential and parallel loops, and calls to a fixed runtime API.

The execution layer is intentionally minimal (not Turing-complete). Every construct maps deterministically to a runtime API call, enabling **M2T compilation** to typed executor classes that are predictable, testable, and code-reviewable.

### What the Behavior DSL models

| View | Concern |
|------|---------|
| **Behavior** | A named procedure (a pipeline, a routine, a strategy variant) |
| **Execution** | The embedded orchestration script (assign, call, foreach, parallelForEach) |
| **Participants** | *(optional)* Role/actor requirements and multiplicity, when a behavior coordinates multiple actors |
| **Error** | Failure-response policies per named failure event |
| **Concurrency** | Parallelism limit expressed as an integer or a runtime expression |

> **Not a crew DSL.** Multi-agent crew orchestration (Sequential / Hierarchical / Parallel strategies) is one *application* of this DSL — shown in §8 — not its definition. The `participants` block and agent-specific error events are domain vocabulary supplied by a project, not core grammar.

---

## 2. Levels (M0–M3)

| Level | What it is | Example |
|-------|-----------|---------|
| **M0** | A behavior running — steps completing, failures handled, work parallelized | A `DocumentIngestion` executor processing 200 documents |
| **M1** | This file: `behavior DocumentIngestion { ... }` | `pipelines.dsl`, `jcrew-processes.dsl` |
| **M2** | Behavior metamodel: `Behavior`, `ExecutionBlock`, `Participants`, `ErrorPolicy`, `ConcurrencyPolicy` | This specification |
| **M3** | Generic language-definition constructs (kept implicit) | |

**Relationship to Kernel DSL:** Both DSLs operate at M2 for separate concerns and produce M1 `.dsl` files. They share a build toolchain (same grammar infrastructure) but have independent metamodels.

---

## 3. Metamodel (M2): Core Concepts

### 3.1 Behavior

The top-level named classifier. Represents one procedure.

```
behavior DocumentIngestion {
    description "Validate, parse, and index a batch of documents."
    ...
}
```

Attributes:
- `name` — identifier; drives the generated class name (`DocumentIngestion` → `DocumentIngestionBehavior`)
- `description` (optional) — human-readable documentation

Blocks (all optional except `execution`):
- `participants` — actor/role constraints, when the behavior coordinates multiple actors
- `execution` — orchestration script (**required**)
- `errorHandling` — failure-response policies
- `concurrency` — parallelism limit

### 3.2 Execution Script

The heart of the DSL: an embedded orchestration script inside an `execution { ... }` block.

| Construct | Syntax | Meaning |
|-----------|--------|---------|
| Function call | `validate(documents)` | Invoke a runtime API function |
| Assignment | `parsed = parseAll(documents)` | Bind a result to a local variable |
| Foreach | `foreach doc in parsed { ... }` | Sequential iteration |
| Parallel foreach | `parallelForEach doc in parsed { ... }` | Concurrent iteration (governed by `concurrency`) |

**The callable function set is the runtime API contract.** Domain-specific logic is not embedded in the script; it is referenced by name and implemented as runtime hooks. The generator compiles each statement to a call on this fixed API. This is what keeps the language general — only the *shape* of the procedure is modeled; the *work* lives in the runtime.

### 3.3 Participants *(optional)*

Declares actor/role requirements and multiplicity — used only when a behavior coordinates more than one actor (e.g., a manager and workers, a producer and consumers).

```
participants {
    manager:  required
    workers:  1..*
    observer: optional
}
```

| Modifier | Meaning |
|----------|---------|
| `required` | Exactly one actor with this role |
| `optional` | Zero or one |
| `N..*` | At least N actors |
| `N..M` | Between N and M actors |

A behavior with a single implicit actor (a plain pipeline or routine) omits this block entirely.

### 3.4 ErrorPolicy

Defines responses to named failure events.

```
errorHandling {
    onStepFailure: SKIP_AND_CONTINUE
    onTimeout:     RETRY_WITH_FALLBACK
}
```

Generic failure events: `onStepFailure`, `onTimeout`. Projects may declare **domain-specific** events (e.g., a crew project adds `onManagerError`) and **domain-specific** responses (e.g., `PROMOTE_WORKER_TO_MANAGER`). The grammar treats event and response names as open identifiers; only a small generic set is built in (`STOP`, `CONTINUE`, `SKIP_AND_CONTINUE`, `RETRY_WITH_FALLBACK`).

### 3.5 ConcurrencyPolicy

Defines the maximum number of concurrent steps.

```
concurrency {
    maxParallel: 4
}
```

`maxParallel` accepts an integer literal (`1`, `4`) or a runtime expression (`documents.count()`, `workers.count()`).

---

## 4. Formal Grammar (EBNF)

```ebnf
start               = behavior+

behavior            = "behavior" NAME "{" behavior_body+ "}"
behavior_body       = description
                    | participants_block
                    | execution_block
                    | error_handling
                    | concurrency_block

description         = "description" STRING

participants_block  = "participants" "{" participant_decl+ "}"
participant_decl    = NAME ":" participant_modifier
participant_modifier = "required" | "optional" | multiplicity
multiplicity        = INTEGER ".." ("*" | INTEGER)

execution_block     = "execution" "{" statement+ "}"
statement           = call_stmt
                    | assign_stmt
                    | foreach_stmt
                    | parallel_foreach_stmt

call_stmt           = NAME "(" arg_list? ")"
assign_stmt         = NAME "=" expr
foreach_stmt        = "foreach" NAME "in" expr "{" statement+ "}"
parallel_foreach_stmt = "parallelForEach" NAME "in" expr "{" statement+ "}"
arg_list            = expr ("," expr)*
expr                = NAME ( "." NAME | "(" arg_list? ")" )*

error_handling      = "errorHandling" "{" error_policy+ "}"
error_policy        = NAME ":" NAME

concurrency_block   = "concurrency" "{" "maxParallel" ":" concurrency_expr "}"
concurrency_expr    = INTEGER | NAME ( "." NAME "(" ")" )?

NAME                = /[A-Za-z_][A-Za-z0-9_]*/
STRING              = ESCAPED_STRING
INTEGER             = /[0-9]+/

%import common.ESCAPED_STRING
%import common.WS
%ignore WS
COMMENT             : "//" /[^\n]*/
%ignore COMMENT
```

---

## 5. Semantics: Transformations (M2T)

### 5.1 Behavior → Executor Class

For each `behavior X`, generate class `XBehavior` implementing a shared `Behavior` interface:

```java
// Generated from: behavior DocumentIngestion { ... }
// GENERATED CODE - DO NOT EDIT
@Generated("dsl-codegen")
public class DocumentIngestionBehavior implements Behavior {

    @Override
    public BehaviorResult execute(BehaviorContext ctx) {
        validate(ctx.documents);                          // call_stmt
        List<Doc> parsed = parseAll(ctx.documents);       // assign_stmt
        forEachParallel(parsed, doc -> index(doc),        // parallel_foreach_stmt
                        maxParallel(ctx));
        return publishManifest(parsed);
    }
}
```

### 5.2 ErrorHandling → Policy Methods

Error policies generate a `getErrorPolicy()` method or a policy object injected at construction, with one case per declared `errorHandling` entry.

### 5.3 ConcurrencyPolicy → Executor Configuration

`maxParallel` is exposed as a method or constructor parameter passed to the parallel executor. Integer literals become constants; expressions become method calls.

### 5.4 Participants → Validator

When present, participant constraints generate `validateParticipants(...)` that asserts the declared multiplicity for each role before execution begins.

---

## 6. Well-Formedness Rules

- Each `behavior` must have exactly one `execution` block.
- Variables used in expressions must be defined before use (as prior assignments, context inputs, or runtime API globals).
- `parallelForEach` with `maxParallel: 1` is a semantic contradiction — validators should warn (not error; the generator still produces correct code).
- Role names referenced in `concurrency` expressions (e.g., `workers.count()`) must appear in the `participants` block.
- Error event and response names are open; a validator may warn on names outside the generic set but must not reject them.

---

## 7. Example: A General Behavior (no participants)

A plain pipeline — the most common shape. No actors to coordinate, so no `participants` block.

```dsl
behavior DocumentIngestion {
    description "Validate, parse, and index a batch of documents."

    execution {
        validate(documents)
        parsed = parseAll(documents)
        parallelForEach doc in parsed {
            index(doc)
        }
        publishManifest(parsed)
    }

    errorHandling {
        onStepFailure: SKIP_AND_CONTINUE
        onTimeout:     RETRY_WITH_FALLBACK
    }

    concurrency {
        maxParallel: documents.count()
    }
}
```

---

## 8. Application Example: Multi-Agent Crew Strategies

Crew orchestration is one *application* of the Behavior DSL. The same grammar expresses three strategy variants; the only domain-specific additions are the `participants` roles and crew-specific error events — both supplied by the project, not the grammar.

```dsl
behavior Sequential {
    description "Execute tasks one at a time in dependency order."

    execution {
        validateInputs(tasks, agents)
        sortedTasks = sortByDependencyOrder(tasks)
        foreach task in sortedTasks {
            executeTask(task)
        }
        aggregateResults(tasks)
    }

    errorHandling {
        onStepFailure: CONTINUE
    }

    concurrency {
        maxParallel: 1
    }
}

behavior Hierarchical {
    description "Manager delegates tasks to workers; collects results."

    participants {
        manager: required
        workers: 1..*
    }

    execution {
        validateInputs(tasks, agents)
        assignedTasks = delegateToWorkers(tasks, workers)
        parallelForEach task in assignedTasks {
            executeTask(task)
        }
        finalResult = aggregateResults(tasks)
        storeResult(finalResult)
    }

    errorHandling {
        onStepFailure:  RETRY_WITH_FALLBACK
        onManagerError: PROMOTE_WORKER_TO_MANAGER
    }

    concurrency {
        maxParallel: workers.count()
    }
}

behavior Parallel {
    description "All agents work concurrently with no delegation hierarchy."

    execution {
        validateInputs(tasks, agents)
        parallelForEach task in tasks {
            executeTask(task)
        }
        aggregateResults(tasks)
    }

    errorHandling {
        onStepFailure: CONTINUE
    }

    concurrency {
        maxParallel: tasks.count()
    }
}
```

Notice that `onManagerError` and `PROMOTE_WORKER_TO_MANAGER` are project-defined names, not keywords. A non-crew project never sees them.

---

## 9. Versioning and Evolution

- This document defines **Behavior DSL v1.0**.
- v1.0 includes: named behaviors, optional participants, execution scripts (`call` / `assign` / `foreach` / `parallelForEach`), error handling, concurrency policy.
- Planned v1.x additions: `timeout { ... }`, `retryPolicy { ... }`, conditional (`when`) statements, `compensationStep { ... }`.
- Breaking grammar changes require v2.0.

**Guidelines for extending:**
- New optional blocks (timeout, retry, compensation) are additive — v1.x.
- Changes to execution-script semantics or required-block contracts are breaking — v2.0.
- Do not bake application-specific runtime API calls into this grammar; they belong in the runtime API contract, not the DSL spec.

---

**Document Version**: 1.0  
**Last Updated**: 2026-06-08  
**License**: Apache 2.0 — https://github.com/everapp-org/dsl-first/blob/main/LICENSE
