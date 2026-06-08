# DSL-First, in One Page

> The whole methodology fits here. Everything else in this repo is depth to reach for later — not a prerequisite.

## What it is

DSL-First is a methodology for building software **with an AI coding assistant**. You give the assistant an intent — a written specification, or an existing/legacy repo. The assistant captures that intent as a model in a small domain-specific language (the **DSL**), makes that DSL the **single source of truth**, and mechanically derives everything else from it: the application code, the tests, and the documentation.

**You don't write the DSL — the assistant does.** What you get back is a working app, a test suite, and generated docs, all guaranteed to agree because they came from one model. The DSL is there for you to *read* when you need to understand or verify what was built.

## Who does what

| | Provides / authors | Consumes / observes |
|---|---|---|
| **You (developer)** | a spec, or a legacy repo; change requests in plain language | the running app, the generated docs, the test results — and the DSL when you need to dig in |
| **AI assistant** | the DSL (the SSOT), and the derived code / tests / docs | your spec, legacy code, and prompts |

## The loop

```
  spec  ──►  AI writes / updates  ──►  DSL (SSOT)  ──►  AI derives  ──►  app + tests + docs
or legacy                                                                      │
     ▲                                                                         ▼
     └─────────────────  you observe, and prompt for changes  ◄────────────────┘
```

- **Default — stay in natural language.** You describe what you want; the assistant updates the DSL and regenerates. You read the DSL only to understand or verify.
- **Fallback — edit the DSL directly.** When you want exact, surgical control, you hand-edit the SSOT (it's a small, readable model) and the assistant regenerates from it.
- **Never hand-edit the generated code, tests, or docs.** They are outputs; the next regeneration overwrites them. The DSL is the thing you change.

## What the DSL captures (so you can read it)

The DSL describes your domain — **what things are** — in a fixed shape, the same regardless of host language:

```
domain         a bounded context
  level         a scope inside it (domain / application / infrastructure)
    model        an entity or value: fields, optional state machine, invariants
    service      externally visible operations
  constraints    architecture rules ("forbid imports from ui.* into core")
```

A model you might find in the SSOT:

```
model Note {
    fields { id: NoteId, title: String, state: NoteState }
    states { Draft, Published }
    transitions { Draft -> Published on publish() emits NotePublished }
}
```

From that one model the assistant derived a `Note` type, a `NoteState` enum, a `publish` operation with its event, a test per transition, and a state diagram. When a test fails or the app misbehaves, **this is where you look first** — it's the authoritative statement of intent, far smaller than the code it produced.

When the system needs to describe **how things happen** — ordered/parallel steps, error handling — that's a separate concern with its own model; see the [Behavior DSL](specification/BEHAVIOR_DSL.md).

## The principle that keeps the SSOT lean

The assistant follows one rule when modeling:

> **A new grammar is justified only when a concern has concepts that cannot be expressed in the existing metamodel** — not merely because the concern is different or has its own vocabulary.

Most "new languages" are just *domains* written in the language already in hand. (Worked example: [provider integration looked like a third grammar; it wasn't](docs/case-study-provider-integration.md).) This is what stops the model from sprawling.

## Paradigm-agnostic

The *mechanism* the assistant uses to derive artifacts depends on the host language — and it's the only thing that changes:

- **Grammar-hosted** (Java, C#, Go): the DSL is a text file, parsed by a grammar; a generator emits source. See the [practitioner's guide](DSL_FIRST_DEVELOPMENT_GUIDE.md).
- **Data / homoiconic** (Clojure, Lisp): the DSL *is data* (EDN/maps), validated by a schema (malli/spec) and interpreted or macro-expanded directly — no grammar, no parser, no codegen step.

The model and the SSOT discipline are identical in both.

## When to use it

**Use it** when a domain has structure and rules that several artifacts must agree on, and that keeps evolving — exactly where hand-written code, tests, and docs drift apart. **Skip it** for one-off scripts or throwaway prototypes.

## Where to go next

| You want to… | Read |
|--------------|------|
| See what the SSOT looks like (structure & lifecycle) | [Kernel DSL spec](specification/KERNEL_DSL.md) |
| …and how procedures are modeled | [Behavior DSL spec](specification/BEHAVIOR_DSL.md) |
| Apply the methodology end-to-end (grammar-hosted) | [Practitioner's guide](DSL_FIRST_DEVELOPMENT_GUIDE.md) |
| See the "is this a new grammar?" judgement | [Provider integration case study](docs/case-study-provider-integration.md) |
