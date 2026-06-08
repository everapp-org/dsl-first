# Process Strategy DSL Specification (v1.0)

> **Origin**: Part of the [DSL-First Methodology](https://github.com/everapp-org/dsl-first) open-source project.  
> **License**: Apache 2.0 — see [LICENSE](../LICENSE)

**Version:** 1.0  
**Companion:** [Kernel DSL Specification v1.1](KERNEL_DSL.md)  
**Purpose:** A modeling language for describing **workflow execution strategies** — how tasks are ordered, delegated, parallelized, and recovered from failure — as M1 process models compiled into runtime executor classes.

---

## 1. Overview

The Process Strategy DSL is the second member of the DSL-First language family. Where the **Kernel DSL** models *domain structure and lifecycle behavior*, the **Process DSL** models *orchestration semantics*.

A process model has two layers:

- **Declarative configuration layer** — process variant name, role constraints, error policies, concurrency limits
- **Imperative orchestration layer** — an embedded execution script with assignments, loops, and calls to a fixed runtime API

The execution layer is intentionally minimal (not Turing-complete). Every construct maps deterministically to a runtime API call, enabling **M2T compilation** to typed executor classes.

### What the Process DSL models

| View | Concern |
|------|---------|
| **Strategy** | Named process variants (Sequential, Hierarchical, Parallel, custom) |
| **Roles** | Agent role requirements and multiplicity constraints |
| **Execution** | Embedded orchestration script (sort, foreach, delegate, call) |
| **Error** | Failure response policies per named failure event |
| **Concurrency** | Parallelism limits expressed as integer or runtime expression |

---

## 2. Levels (M0–M3)

| Level | What it is | Example |
|-------|-----------|---------|
| **M0** | A crew running — tasks completing, failures handled in sequence | A `SequentialProcess` instance executing 5 tasks |
| **M1** | This file: `process Sequential { ... }` | `jcrew-processes.dsl` |
| **M2** | Process metamodel: `Process`, `ExecutionBlock`, `Roles`, `ErrorHandlingPolicy`, `ConcurrencyPolicy` | This specification |
| **M3** | Generic language-definition constructs (kept implicit) | |

**Relationship to Kernel DSL:** Both DSLs operate at M2 for separate concerns and produce M1 `.dsl` files. They share a build toolchain (same grammar infrastructure) but have independent metamodels.

---

## 3. Metamodel (M2): Core Concepts

### 3.1 Process

The top-level named classifier. Represents one execution strategy variant.

```
process Sequential {
    description "Execute tasks one at a time in dependency order."
    ...
}
```

Attributes:
- `name` — identifier; drives the generated class name (`Sequential` → `SequentialProcess`)
- `description` (optional) — human-readable documentation

Blocks (all optional except `execution`):
- `roles` — role constraints for participating agents
- `execution` — orchestration script (required)
- `errorHandling` — failure response policies
- `concurrency` — parallelism limit

### 3.2 Roles

Declares agent role requirements and multiplicity.

```
roles {
    manager:  required
    workers:  1..*
    observer: optional
}
```

| Modifier | Meaning |
|----------|---------|
| `required` | Exactly one agent with this role |
| `optional` | Zero or one |
| `N..*` | At least N agents |
| `N..M` | Between N and M agents |

### 3.3 Execution Script

An embedded orchestration script inside an `execution { ... }` block.

| Construct | Syntax | Meaning |
|-----------|--------|---------|
| Function call | `validateInputs(tasks, agents)` | Invoke a runtime API function |
| Assignment | `sortedTasks = sortByDependencyOrder(tasks)` | Bind result to a local variable |
| Foreach | `foreach task in sortedTasks { ... }` | Sequential iteration |
| Parallel foreach | `parallelForEach task in tasks { ... }` | Concurrent iteration (governed by `concurrency` policy) |

**The callable function set is the runtime API contract.** Domain-specific logic is not embedded in the script; it is referenced by name and implemented as runtime hooks. The generator compiles each statement to a call on this fixed API.

### 3.4 ErrorHandlingPolicy

Defines responses to named failure events.

```
errorHandling {
    onTaskFailure: CONTINUE_OTHERS
    onManagerError: PROMOTE_WORKER_TO_MANAGER
    onAgentError: STOP_CREW
}
```

Standard failure events: `onTaskFailure`, `onAgentError`, `onManagerError`, `onTimeout`.

Standard response values: `STOP_CREW`, `CONTINUE_OTHERS`, `RETRY_WITH_FALLBACK`, `PROMOTE_WORKER_TO_MANAGER`. Projects may add custom response values.

### 3.5 ConcurrencyPolicy

Defines the maximum number of concurrent tasks.

```
concurrency {
    maxParallelTasks: 1
}
```

`maxParallelTasks` accepts an integer literal (`1`, `4`) or a runtime expression (`tasks.count()`, `workers.count()`).

---

## 4. Formal Grammar (EBNF)

```ebnf
start               = process+

process             = "process" NAME "{" process_body+ "}"
process_body        = description
                    | roles_block
                    | execution_block
                    | error_handling
                    | concurrency_block

description         = "description" STRING

roles_block         = "roles" "{" role_decl+ "}"
role_decl           = NAME ":" role_modifier
role_modifier       = "required" | "optional" | multiplicity
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

concurrency_block   = "concurrency" "{" "maxParallelTasks" ":" concurrency_expr "}"
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

### 5.1 Process → Executor Class

For each `process X`, generate class `XProcess` implementing a shared `ProcessStrategy` interface:

```java
// Generated from: process Sequential { ... }
// GENERATED CODE - DO NOT EDIT
@Generated("dsl-codegen")
public class SequentialProcess implements ProcessStrategy {

    @Override
    public ProcessResult execute(List<Task> tasks, List<Agent> agents) {
        validateInputs(tasks, agents);                           // call_stmt
        List<Task> sortedTasks = sortByDependencyOrder(tasks);   // assign_stmt
        for (Task task : sortedTasks) {                          // foreach_stmt
            executeTask(task);
        }
        return aggregateResults(tasks);
    }
}
```

### 5.2 ErrorHandling → Policy Methods

Error policies generate a `getErrorPolicy()` method or a policy object injected at construction, with one case per declared `errorHandling` entry.

### 5.3 ConcurrencyPolicy → Executor Configuration

`maxParallelTasks` is exposed as a method or constructor parameter passed to the parallel executor. Integer literals become constants; expressions become method calls.

### 5.4 Roles → Validator

Role constraints generate `validateAgentRoles(List<Agent> agents)` that asserts the declared multiplicity for each role before execution begins.

---

## 6. Well-Formedness Rules

- Each `process` must have exactly one `execution` block.
- Variables used in expressions must be defined before use (as prior assignments, parameters, or runtime API globals).
- `parallelForEach` with `maxParallelTasks: 1` is a semantic contradiction — validators should warn (not error; the generator still produces correct code).
- Role names referenced in `concurrency` expressions (e.g., `workers.count()`) must appear in the `roles` block.
- Error event names outside the standard set (`onTaskFailure`, `onAgentError`, `onManagerError`, `onTimeout`) should produce a warning; they are valid extensions.

---

## 7. Example: Complete Process File

```dsl
process Sequential {
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
        onTaskFailure: CONTINUE_OTHERS
        onAgentError:  STOP_CREW
    }

    concurrency {
        maxParallelTasks: 1
    }
}

process Hierarchical {
    description "Manager delegates tasks to workers; collects results."

    roles {
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
        onTaskFailure: RETRY_WITH_FALLBACK
        onManagerError: PROMOTE_WORKER_TO_MANAGER
        onAgentError:   CONTINUE_OTHERS
    }

    concurrency {
        maxParallelTasks: workers.count()
    }
}

process Parallel {
    description "All agents work concurrently with no delegation hierarchy."

    execution {
        validateInputs(tasks, agents)
        parallelForEach task in tasks {
            executeTask(task)
        }
        aggregateResults(tasks)
    }

    errorHandling {
        onTaskFailure: CONTINUE_OTHERS
        onAgentError:  CONTINUE_OTHERS
    }

    concurrency {
        maxParallelTasks: tasks.count()
    }
}
```

---

## 8. Versioning and Evolution

- This document defines **Process Strategy DSL v1.0**.
- v1.0 includes: process variants, roles, execution scripts (`call` / `assign` / `foreach` / `parallelForEach`), error handling, concurrency policy.
- Planned v1.x additions: `timeout { ... }`, `retryPolicy { ... }`, `compensationStep { ... }`.
- Breaking grammar changes require v2.0.

**Guidelines for extending:**
- New optional blocks (timeout, retry, compensation) are additive — v1.x.
- Changes to execution script semantics or required block contracts are breaking — v2.0.
- Do not bake application-specific runtime API calls into this grammar; they belong in the runtime API contract, not the DSL spec.

---

**Document Version**: 1.0  
**Last Updated**: 2026-06-08  
**License**: Apache 2.0 — https://github.com/everapp-org/dsl-first/blob/main/LICENSE
