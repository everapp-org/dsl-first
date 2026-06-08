# DSL-First, in One Page

> **Spec in, app out.** You don't need to know how it works to use it — the second half of this page is for the curious.

## The pitch

Hand your AI coding assistant a specification (or point it at a legacy repo). Get back a working application — code, tests, and documentation that all agree.

The win is **economic**. Instead of having the LLM generate — and you paying tokens for — every line of code, the assistant writes only a small model, and a **deterministic generator** expands that model into the full codebase. You **spend cheap CPU cycles instead of expensive tokens**.

- **Faster** — the slow, expensive part (the LLM thinking) happens once over a small model, not across thousands of lines of output.
- **Cheaper** — CPU expands the model essentially for free; tokens are spent only on *intent*, not on boilerplate.
- **Correct** — deterministic expansion can't hallucinate. The generated code, tests, and docs can't drift from each other, because they all come from one model. And to review what was built, you read a small model, not a large codebase.

That is the whole value proposition. **Everything below is optional.**

---

## How it works (for the curious)

Skip this unless you want to understand the machine. You can use the methodology without any of it.

**The single source of truth.** The assistant captures your intent as a model in a small domain-specific language — the DSL. That model is the one authoritative artifact; the app, tests, and docs are *derived* from it, never written by hand. When something needs investigating, you read the DSL (it's small and readable). When you want surgical control, you edit it directly and regenerate. By default you just keep prompting in natural language, and the assistant updates the model for you.

**What the model captures** — *what things are*:

```
model Note {
    fields { id: NoteId, title: String, state: NoteState }
    states { Draft, Published }
    transitions { Draft -> Published on publish() emits NotePublished }
}
```

From that one model the generator produces the `Note` type, the `NoteState` enum, the `publish` operation and its event, a test for every transition, and a state diagram. (For *how things happen* — ordered or parallel steps — there's a separate [Behavior DSL](specification/BEHAVIOR_DSL.md).)

**The rule that keeps the model lean:** a new grammar is justified only when a concern has concepts the existing model genuinely can't express — not merely because it's different. Most "new languages" are just domains in the language already in hand ([worked example](docs/case-study-provider-integration.md)).

**Paradigm-agnostic:** in grammar-hosted languages (Java, C#, Go) the DSL is a text file, parsed and expanded by a code generator. In data/homoiconic languages (Clojure, Lisp) the DSL *is* data, validated by a schema and interpreted directly — no parser, no codegen step. Same model, different machinery.

## When to use it

Use it when a domain has structure several artifacts must agree on, and it keeps evolving — exactly where hand-written code, tests, and docs drift apart. Skip it for one-off scripts and throwaway prototypes.

## Go deeper

| You want to… | Read |
|--------------|------|
| See what the model looks like (structure & lifecycle) | [Kernel DSL spec](specification/KERNEL_DSL.md) |
| …and how procedures are modeled | [Behavior DSL spec](specification/BEHAVIOR_DSL.md) |
| Apply the methodology end-to-end | [Practitioner's guide](DSL_FIRST_DEVELOPMENT_GUIDE.md) |
| See the "is this a new grammar?" judgement | [Provider integration case study](docs/case-study-provider-integration.md) |
