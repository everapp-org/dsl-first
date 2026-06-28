# Case Study: Context Interpreters — Generic Techniques, Not Generic Code

> **Origin**: Part of the [DSL-First Methodology](https://github.com/everapp-org/dsl-first) open-source project.  
> **License**: Apache 2.0 — see [LICENSE](../LICENSE)

**Type:** Case study — extracting reusable interpreter techniques from a production codebase  
**Uses:** [Data-hosted binding](../dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md#25-two-bindings-grammar-hosted-and-data-hosted) (Clojure + EDN + malli)  
**Companion example:** [`../examples/clojure-edn-contexts`](../examples/clojure-edn-contexts/README.md)

---

## 1. The Question

A production Clojure application (IKC — Infinite Knowledge Canvas) uses DSL-First with 18 bounded contexts, a command bus, and composition-driven application boot. Each context is an EDN manifest declaring commands, queries, subscribers, and middleware. Two generic interpreters wire these manifests to a running bus at startup:

- **Context interpreter** — walks one manifest, resolves `:handled-by` keywords to Clojure vars, registers them on the bus
- **Composition interpreter** — reads an application EDN value, leases a subset of contexts, binds ports

"Generic" here means the interpreters **name no application** — the same code boots any composition. But does "generic" mean they belong in the DSL-First methodology distribution?

**No — and the distinction matters.** The interpreters are *IKC's application of DSL-First*, not *part of DSL-First itself*. The methodology teaches the pattern; the project demonstrates it. What *is* reusable is the set of **techniques** the interpreters embody. This case study extracts those techniques into a self-contained example and explains why the boundary is where it is.

---

## 2. What Was Generic vs. What Was Project-Specific

### Generic (the techniques, extracted into the companion example)

| Technique | What it does | Why it's reusable |
|-----------|-------------|-------------------|
| **Multi-level interpretation** | One manifest declares commands, queries, subscribers, middleware; one interpreter walks all four | Any bus-based system has these same categories |
| **`requiring-resolve` dispatch** | `:handled-by` is a keyword; the interpreter resolves it to a Clojure var at install time | Any Clojure project can use this indirection — no framework needed |
| **Status-gated wiring** | Only `:active` declarations are wired; `:planned` stays silently unwired | Any incremental project benefits from declaring intent before implementation |
| **Composition / leasing** | A separate EDN file names which contexts to install; the interpreter is application-agnostic | Any multi-context system needs to choose a subset |

### Project-specific (NOT extracted)

| What | Why it stays in IKC |
|------|---------------------|
| `IBus` protocol with TCP/WebSocket transports | IKC's spine — not a DSL-First primitive |
| Context manifest schema (`:levels`, `:models`, `:domain`) | IKC's EDN shape — other projects may shape manifests differently |
| Port setters (`tools.handlers/set-permission-port!`) | IKC's extension points — hardcoded to IKC's contexts |
| Datomic schema installation, frame store rehydration | IKC's persistence layer — nothing to do with interpretation |
| State-machine interpretation (`:executable`, `:transitions`, guard/action resolution) | IKC's ADR-030 feature — a further technique that could be its own case study |
| Saga declaration and cross-context coordination | IKC's orchestration layer |

The boundary is clear: **the techniques are how you interpret; the project-specific code is what you interpret.** DSL-First teaches the former. The latter is your domain.

---

## 3. The Extraction Process

### Step 1: Identify the techniques

Reading the two interpreters (`contexts.embed` and `ikc.compose`), four patterns appeared repeatedly:

1. The same shape of `for` comprehension over `:levels` → `:models`, filtering by level kind and active status
2. `try-resolve` — the same `requiring-resolve` call, with the same nil-on-failure semantics
3. The `active-statuses` set — a gate that lets the manifest describe more than what exists
4. The composition's `:leases` vector driving `install-context!` per domain keyword

### Step 2: Remove project coupling

The original interpreters depend on:
- `ikc.bus.protocol` — replaced with a minimal `IBus` protocol (6 methods)
- `ikc.dsl.state-machine` — removed (state-machine interpretation is a separate technique)
- `ikc.platform.schema` — removed (Datomic schema installation is persistence, not interpretation)
- IKC's 18 context manifests — replaced with 2 trivial contexts (`tasks`, `audit`)

The result is ~200 lines of interpreter code with zero IKC dependencies — only Clojure and malli.

### Step 3: Preserve the shape

The companion example's `contexts.embed` has the same structure as the original:
- `try-resolve` — identical
- `registrations` / `subscriptions` / `middlewares` — same `for` comprehension shape
- `install-context!` — same `doseq` over each level, same `swap! registered conj` pattern
- `install!` — same `doseq` over `context-resources`, same `io/resource` lookup

A reader who learns the pattern on the example will recognize it immediately in the production code.

---

## 4. Why Not Ship the Interpreters Themselves?

Three reasons:

### 4.1 The methodology teaches the pattern, not the implementation

DSL-First's data-hosted binding (§2.5, §5.3.4) already defines what an interpreter *is*: "a function that derives behaviour by walking the model data at runtime." The minimal example (`clojure-edn-minimal`) shows this in 10 lines. The advanced example shows it at larger scale. Both are *examples* — they illustrate the methodology, they don't *implement* it.

Shipping the IKC interpreters would invert this: instead of "here's the pattern, apply it to your domain," it would be "here's our domain's interpreters, adapt them to yours." That's a framework, not a methodology.

### 4.2 Project coupling is structural, not accidental

The interpreters reference `ikc.bus.protocol`, `ikc.dsl.state-machine`, and IKC's context manifest schema. These aren't optional plugins — they're the interpreters' *reason for existing*. The context interpreter exists *because* IKC has a bus with handler registration. A project without a bus doesn't need this interpreter; a project with a different bus protocol can't use it without rewriting the bus calls.

Decoupling the interpreters from IKC would mean either:
- **Stripping them to nothing** — remove the bus, the schema, the state machines → you're left with `requiring-resolve`, which is a one-liner, not a module
- **Abstracting over every dependency** — define protocols for bus, schema, state machines → you've built a framework, which DSL-First explicitly isn't

### 4.3 The techniques are more valuable than the code

A reader who understands "use `requiring-resolve` to turn a keyword into a dispatch target" can apply it in 5 lines to their own project. A reader who imports a 200-line interpreter module has to learn its abstractions, its bus protocol, its schema shape — and then fight the places where those don't match their needs.

The case study + example format delivers the *understanding* without the *coupling*.

---

## 5. What the Companion Example Demonstrates

The [`clojure-edn-contexts`](../examples/clojure-edn-contexts/README.md) example is a self-contained Clojure project with:

- **2 context manifests** (`tasks.edn`, `audit.edn`) — one with commands/queries/subscribers, one with middleware
- **1 composition** (`task-app.edn`) — leases both contexts
- **A minimal IBus** — 6-method protocol, in-process atom-backed implementation
- **The context interpreter** — walks a manifest, wires handlers via `requiring-resolve`
- **The composition interpreter** — reads a composition, installs leased contexts
- **Handler fns** — trivial implementations that `:handled-by` keywords resolve to

### What it deliberately doesn't include

- State-machine interpretation (guards, actions, transition tables) — a separate technique, possibly a future case study
- Port binding (`:binds` in the composition) — included as a schema field but not wired, to keep the example focused
- Network transport, persistence, async dispatch — all orthogonal to the interpretation techniques
- Saga coordination — an orchestration pattern, not an interpretation pattern

---

## 6. Decision Rule: When to Extract vs. When to Keep

If you're building a DSL-First project and wondering whether your interpreters belong in the methodology distribution, ask:

**Is the technique reusable independent of your domain?**

- **Yes** → Extract it as an example or case study. The technique is the contribution; your code is the illustration.
- **No, it references your bus / store / protocol** → Keep it. Write a case study describing what you did and why. The *description* is the contribution; the *code* stays home.

**Would a reader from a different project recognize their system in your interpreter?**

- **Yes, after removing project-specific names** → Extract the cleaned version.
- **No, the shape is specific to your architecture** → Write a case study. The pattern may inspire, even if the code can't be reused.

The IKC interpreters passed the first test (the techniques are reusable) but not the second (the code references IKC's bus, schema, and state machines throughout). So: techniques out, code stays.

---

## 7. Relationship to the Minimal Example

| Aspect | `clojure-edn-minimal` | `clojure-edn-contexts` |
|--------|----------------------|----------------------|
| Purpose | Show the data-hosted binding | Show it at production scale |
| Models | 1 state machine | 2 contexts, 4 level types |
| Dispatch | Direct function call | `requiring-resolve` from keyword |
| Wiring | Hand-coded | Manifest-declared, status-gated |
| Application | Single file | Composition file leases contexts |
| Dependencies | clojure, malli | clojure, malli (same) |
| Lines of interpreter code | ~10 | ~200 |

Both follow the same pipeline: **read → validate (schema) → interpret (function walks data)**. The advanced example adds scale, not complexity — the same mental model, applied to more levels and more files.

---

**Document Version**: 1.0  
**Last Updated**: 2026-06-28  
**License**: Apache 2.0 — https://github.com/everapp-org/dsl-first/blob/main/LICENSE
