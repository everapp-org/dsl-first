# When One DSL Isn't Enough: Introducing the Behavior DSL

> A second grammar joins the DSL-First family — and an honest look at when a new grammar is genuinely warranted versus when you just have another domain.

---

Every DSL-First project eventually hits the same wall: the Kernel DSL is excellent at modeling *what things are* — domain entities, state machines, services. But some concerns resist that shape. They need to express *how things happen*.

That is the gap the new **Behavior DSL** fills. And the more interesting story is the *second* concern we looked at — provider integration — which felt like it deserved its own grammar too, but didn't. The contrast between the two is the whole lesson.

---

## The Behavior DSL: a genuine second grammar

A behavior describes how a procedure unfolds — ordered steps, parallel steps, error handling, concurrency. The simplest case is a plain pipeline with no actors to coordinate:

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
    }

    concurrency {
        maxParallel: documents.count()
    }
}
```

That `execution { }` block is not domain modeling. It is an embedded **orchestration script** — with assignments, sequential iteration, parallel iteration, and calls to a fixed runtime API. No amount of `fields { }` and `transitions { }` can express it naturally in the Kernel DSL.

This is the criterion for a new language family member: **not that the concern is different, but that the metamodel genuinely differs**. The Kernel DSL's M2 is built around `Domain → Level → Model → State → Transition`. The Behavior DSL's M2 is built around `Behavior → ExecutionBlock → Participants → ErrorPolicy → ConcurrencyPolicy`. Different shapes need different metamodels.

> "Grammar" throughout this post is grammar-hosted shorthand. In a data-hosted binding (Clojure/EDN) the same new metamodel ships as a *second schema*, not a second grammar — there is no parser either way. The test is identical: **does the metamodel differ?**

### It is general, not a crew DSL

An earlier draft of this work made a scoping mistake worth calling out: it presented the Behavior DSL as a *crew-orchestration* language — Sequential, Hierarchical, Parallel strategies, with a `roles` block full of `manager` and `workers`. That over-narrowed it. The execution script is general; multi-agent crews are just *one application* of it. A deployment routine, an ETL pipeline, and a crew strategy are all behaviors — they differ only in which runtime functions they call. The crew case now lives in the spec as an example (§8), with the agent-specific vocabulary (`participants`, `onManagerError`) clearly marked as project-supplied, not grammar.

---

## The counterexample: Provider integration is just a domain

When we looked at `jcrew-providers.dsl` — LLM provider configuration, protocol bindings, model selection policy — the temptation was to declare it a *third* family member. It warranted its own file. It had its own vocabulary. Surely it needed its own grammar?

No. Walk through what it actually declares:

- `provider OpenAI { ... }` — an entity with fields
- `model gpt-4o { capabilities: [...] costPer1kInputTokens: 0.005 }` — a nested entity with fields
- `protocolBinding { baseUrl: "..." authScheme: BEARER_TOKEN }` — a config block of fields
- `selectionPolicy { strategy: COST_OPTIMIZED fallback: gpt-4o-mini }` — another config block

That is a **Kernel DSL domain**. Every construct maps to `model` with `fields`. There is no embedded mini-language. So provider integration does *not* ship as a specification — it ships as a **case study** that walks through this exact judgement call, and then shows how to model the domain well using a three-layer discipline (capability catalog / protocol binding / selection policy) and a `@volatile` annotation convention for fields like pricing that churn independently of structure.

---

## The distinction that matters

| Question | Answer | Consequence |
|----------|--------|-------------|
| Does this concern have concepts that cannot be expressed naturally in any existing grammar? | Yes → **new grammar** | Behavior DSL: `execution { foreach ... }` has no Kernel DSL analog |
| Does this concern have its own vocabulary and volatility, but its concepts map to existing constructs? | Yes → **new Kernel DSL domain** (new file, same grammar) | Provider integration: `provider`, `protocolBinding` are all entities with fields |

**Separate files for separate concerns. Separate grammars only when the metamodel genuinely differs.**

This is the rule that keeps a DSL family from becoming DSL sprawl. Two grammars plus a well-modeled domain is a useful toolkit. Ten grammars is a new problem.

---

## What changed in this release

- **[`dsl_first_methodology/BEHAVIOR_DSL.md`](../dsl_first_methodology/BEHAVIOR_DSL.md)** — Behavior DSL v1.0: full spec with EBNF grammar, metamodel, M2T transformations, well-formedness rules, a general pipeline example, and crew strategies as one application.

- **[`in_depth_docs/case-study-provider-integration.md`](case-study-provider-integration.md)** — moved out of `specification/` and reframed as a case study: why provider integration is a Kernel DSL domain, not a grammar — and how to model it with the three-layer discipline.

- **[`DSL_FIRST_METHODOLOGY_GUIDE.md`](../dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md) §2.4** — "DSL Families: Multiple DSLs, One Toolchain": the architecture, the decision criterion, and the new-grammar-vs-new-domain table.

---

## Try it

Copy `dsl_first_methodology/BEHAVIOR_DSL.md` into your project alongside `KERNEL_DSL.md` when your domain has procedures to model. Grammar-hosted, it's small enough to implement in an afternoon with ANTLR or a PEG parser; data-hosted, it's a handful of EDN keywords and a schema — no parser at all.

Read `in_depth_docs/case-study-provider-integration.md` before you reach for a new grammar on your next integration concern — it might just be a domain.

Both are Apache 2.0. Drop them in, hand them to your AI assistant, and let it build the pipeline from the spec — parser and generators, or schema and interpreter, whichever your host language calls for.
