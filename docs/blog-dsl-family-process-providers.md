# When One DSL Isn't Enough: Introducing the Process Strategy DSL

> A new specification joins the DSL-First family — and a honest look at when a second grammar is genuinely warranted.

---

Every DSL-First project eventually hits the same wall: the Kernel DSL is excellent at modeling *what things are* — domain entities, state machines, services. But some concerns resist that shape. They need to express *how things happen*.

In jCrew, the concrete example was `jcrew-processes.dsl`: a file that defined Sequential, Hierarchical, and Parallel crew execution strategies. It looked different from the domain DSL:

```dsl
process Hierarchical {
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
        storeResult(aggregateResults(tasks))
    }

    errorHandling {
        onTaskFailure: RETRY_WITH_FALLBACK
        onManagerError: PROMOTE_WORKER_TO_MANAGER
    }

    concurrency {
        maxParallelTasks: workers.count()
    }
}
```

That `execution { }` block is not domain modeling. It is an embedded **orchestration script** — with assignments, sequential iteration, parallel iteration, and calls to a fixed runtime API. No amount of `fields { }` and `transitions { }` can express it naturally in the Kernel DSL.

This is the criterion for a new language family member: **not that the concern is different, but that the metamodel genuinely differs**. The Kernel DSL's M2 is built around `Domain → Level → Model → State → Transition`. The Process DSL's M2 is built around `Process → ExecutionBlock → Roles → ErrorHandlingPolicy → ConcurrencyPolicy`. These are different shapes that need different grammars.

---

## What the Process Strategy DSL Specifies

The new [`specification/PROCESS_DSL.md`](../specification/PROCESS_DSL.md) covers:

**Metamodel (M2):** five core concepts — `Process`, `ExecutionBlock` (the orchestration script), `Roles` (multiplicity constraints), `ErrorHandlingPolicy` (per-failure-event response), and `ConcurrencyPolicy` (parallelism limit as integer or runtime expression).

**Grammar:** a minimal EBNF where the execution script supports four statement forms:
- `functionCall(args)` — invoke a runtime API function
- `variable = expr` — bind a result
- `foreach name in expr { ... }` — sequential iteration
- `parallelForEach name in expr { ... }` — concurrent iteration (governed by the concurrency policy)

The function set is intentionally **closed** — it is the runtime API contract, not a general-purpose scripting language. This keeps the DSL readable and the generator deterministic.

**M2T transformations:** for each `process X`, generate `XProcess` implementing a shared `ProcessStrategy` interface. Execution script statements compile to runtime API calls. Role constraints generate a `validateAgentRoles()` method. Error policies generate per-event case handling.

---

## A Counterexample: Provider Integration Is Just a Domain

When we looked at `jcrew-providers.dsl` — LLM provider configuration, protocol bindings, model selection policy — the temptation was to declare it a third family member. It warranted its own file. It had its own vocabulary. Surely it needed its own grammar?

No. Walk through what it actually declares:

- `provider OpenAI { ... }` — an entity with fields
- `models { gpt-4o { capabilities: [...] costPer1kInputTokens: 0.005 } }` — nested entities with fields
- `protocolBinding { baseUrl: "..." authScheme: BEARER_TOKEN ... }` — a configuration block with fields
- `selectionPolicy { strategy: COST_OPTIMIZED fallback: gpt-4o-mini }` — another configuration block

That is a **Kernel DSL domain**. Every construct maps to `model` or `config` with fields. There is no embedded mini-language. The Kernel DSL handles it without a new grammar:

```dsl
domain providers {
    level catalog {
        model ModelProfile {
            fields {
                provider:              ProviderId
                capabilities:          List<Capability>
                costPer1kInputTokens:  Decimal   // @volatile
            }
        }
    }
    level binding {
        model ProtocolBinding {
            fields {
                baseUrl: String   authScheme: AuthScheme
                endpoint: String  responseSelector: String
            }
        }
    }
    level policy {
        model SelectionPolicy {
            fields { strategy: SelectionStrategy  fallback: ModelId }
        }
    }
}
```

The new [`specification/PROVIDERS_DSL.md`](../specification/PROVIDERS_DSL.md) is therefore not a grammar specification — it is a **domain pattern guide**. Its value is the three-layer discipline it prescribes: keep capability intent (what the provider offers), protocol binding (how to call it), and selection policy (when to use which model) in separate `level` blocks with different change cadences. And the `@volatile` annotation convention for cost and rate-limit fields that change independently of structure.

---

## The Distinction That Matters

| Question | Answer | Consequence |
|----------|--------|-------------|
| Does this concern have concepts that cannot be expressed naturally in any existing grammar? | Yes → new grammar | Process DSL: `execution { foreach ... }` has no Kernel DSL analog |
| Does this concern have its own vocabulary and volatility, but its concepts map to existing constructs? | Yes → new Kernel DSL domain (new file, same grammar) | Provider integration: `provider`, `protocolBinding`, `retryPolicy` are all entities with fields |

**Separate files for separate concerns. Separate grammars only when the metamodel genuinely differs.**

This is the rule that keeps a DSL family from becoming a DSL sprawl. Three grammars in a project is a useful tool. Ten grammars in a project is a new problem.

---

## What Changed in This Release

- **[`specification/PROCESS_DSL.md`](../specification/PROCESS_DSL.md)** — Process Strategy DSL v1.0: full spec with EBNF grammar, metamodel, M2T transformations, well-formedness rules, and a complete jCrew example with Sequential, Hierarchical, and Parallel variants.

- **[`specification/PROVIDERS_DSL.md`](../specification/PROVIDERS_DSL.md)** — Provider Integration Pattern v1.0: how to model external provider integration as a Kernel DSL domain using the three-layer (capability / binding / policy) discipline. Includes `@volatile` annotation convention, Kernel DSL example, and multi-file split guidance.

- **[`DSL_FIRST_DEVELOPMENT_GUIDE.md`](../DSL_FIRST_DEVELOPMENT_GUIDE.md) §2.4** — "DSL Families: Multiple DSLs, One Toolchain": the architecture, the decision criterion, when to add a family member vs. a new domain, and what shared infrastructure looks like.

---

## Try It

Copy `specification/PROCESS_DSL.md` into your project alongside `KERNEL_DSL.md` when you need to model execution strategies. The grammar is small enough to implement in an afternoon with ANTLR or a PEG parser.

Copy `specification/PROVIDERS_DSL.md` as a reference when you are modeling external integrations — adapt the three-layer domain structure to your provider vocabulary.

Both are Apache 2.0. Drop them in, hand them to your AI assistant, and let it build the grammar and generators from the spec.
