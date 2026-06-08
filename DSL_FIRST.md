# DSL-First, in One Page

> The whole methodology fits here. Everything else in this repo is depth you can reach for later — not a prerequisite.

## The idea

Write your domain **once**, as a model in a small domain-specific language. Make that model the **single source of truth**. Derive everything else — types, state machines, tests, docs, config — *from* it, mechanically. Because the derivation is mechanical, the artifacts can never drift apart.

That's it. Three moves:

1. **Model** the domain in a DSL (a small, readable language for *your* domain).
2. **Validate** the model against a fixed metamodel.
3. **Derive** code, tests, and docs from the model — never hand-edit the derived output.

## The metamodel

A DSL-First model has the same shape regardless of host language. You describe **what things are**:

```
domain        a bounded context
  level       a scope inside it (domain / application / infrastructure)
    model      an entity or value: fields, optional state machine, invariants
      state machine   states + transitions (trigger, guard, emitted events, actions)
    service    externally visible operations
  constraints  architecture rules ("forbid imports from ui.* into core")
```

A tiny example:

```
model Note {
    fields { id: NoteId, title: String, state: NoteState }
    states { Draft, Published }
    transitions {
        Draft -> Published on publish() emits NotePublished
    }
}
```

From that one model you derive a `Note` type, a `NoteState` enum, a `publish` transition with its guard and event, a test for every transition, and a state diagram. You wrote the model; you generated the rest.

When you need to describe **how things happen** — ordered/parallel steps, error handling — that's a second, separate concern; see the [Behavior DSL](specification/BEHAVIOR_DSL.md).

## The one rule that makes it work

> **A new grammar is justified only when a concern has concepts that cannot be expressed in your existing metamodel** — not merely because the concern is different or has its own vocabulary.

Most "new languages" you're tempted to add are just *domains* written in the language you already have. (Worked example: [provider integration looked like a third grammar; it wasn't](docs/case-study-provider-integration.md).)

## How "derive" actually happens depends on your host

DSL-First is paradigm-agnostic. The *mechanism* of derivation is the one thing that changes by language:

- **Grammar-hosted languages** (Java, C#, Go): the DSL is a text file, parsed by a grammar, and a generator emits source. See the [practitioner's guide](DSL_FIRST_DEVELOPMENT_GUIDE.md).
- **Data / homoiconic languages** (Clojure, Lisp): the DSL *is data* (EDN/maps), validated by a schema (malli/spec) and interpreted or macro-expanded directly — **no grammar, no parser, no codegen step**.

The metamodel above is identical in both. Only the carrier and the derivation differ.

## When to use it (and when not)

**Use it** when a domain has structure and rules that several artifacts must agree on, and that structure evolves. **Skip it** for one-off scripts, throwaway prototypes, or domains too small to repay the modeling.

## Where to go next

| You want to… | Read |
|--------------|------|
| Model domain structure & lifecycle | [Kernel DSL spec](specification/KERNEL_DSL.md) |
| Model how procedures run | [Behavior DSL spec](specification/BEHAVIOR_DSL.md) |
| Apply it end-to-end (grammar-hosted) | [Practitioner's guide](DSL_FIRST_DEVELOPMENT_GUIDE.md) |
| See the "is this a new grammar?" judgement | [Provider integration case study](docs/case-study-provider-integration.md) |
