# DSL-First Methodology

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](#contributing)
[![Java](https://img.shields.io/badge/Reference_Implementation-Java%20%2B%20ANTLR-orange.svg)](examples/java-antlr-minimal/README.md)

**Welcome to the home of DSL-First Development.**

**Spec in, app out.** Hand your AI coding assistant a specification (or a legacy repo) and get back a working application — code, tests, and docs that agree. The trick: the assistant writes only a small model (a **DSL**), and a *deterministic generator* expands it into the full codebase. You **spend cheap CPU cycles instead of expensive LLM tokens** — so you get correct code faster and cheaper, with no drift between code, tests, and docs. **You don't need to know how it works to use it.**

> When one model DRIVES the code, tests, and docs, drift between them becomes impossible.

> ⭐ **If you find this methodology useful, please star this repo** — it helps others discover it!
>
> [![Star History Chart](https://api.star-history.com/svg?repos=everapp-org/dsl-first&type=Date)](https://star-history.com/#everapp-org/dsl-first&Date)

## Table of Contents

- [Who is this for?](#who-is-this-for)
- [The problem it solves](#the-problem-it-solves)
- [How it works](#how-it-works)
- [Getting started](#getting-started)
- [Project structure](#project-structure)
- [Documentation navigation](#documentation-navigation)
- [Contributing](#contributing)
- [License](#license)

## Who Is This For?

This methodology is designed for **developers using AI coding assistants** (Cursor, GitHub Copilot, Windsurf, Claude Code, etc.).

The core insight: AI assistants are dramatically more effective when they work from a **structured model** than from natural-language requirements alone. In the DSL-First methodology, the AI coding assistant turns your specification — or your existing/legacy repo — into small, readable models written in our domain-specific languages. These models become the single source of truth, and the assistant then deterministically generates the code, tests, and documentation from them.

**You provide intent and review the results; the assistant authors and maintains the DSL.** You read it when you need to dig in, and edit it directly only when you want surgical control.

**Any host language.** Your app can be in Java, TypeScript, Python, Go, C#, or Clojure. Each is handled a little differently under the hood — a text grammar and code generator in some, plain data and a schema in others — but that's the assistant's concern while applying the methodology, not yours.

## The Problem It Solves

In traditional development, the spec, the code, the tests, and the documentation are separate artifacts — they *will* drift apart over time. DSL-First eliminates drift by making **one artifact generate all the others**.

## How It Works

```
  You provide:  a spec, or a legacy repo
        │
        ▼
  AI assistant  ──authors──►  DSL  (single source of truth)
        │                      │
        │                      ▼   derived deterministically
        │             ┌────────┼─────────┐
        │             ▼        ▼          ▼
        │         app code   tests       docs
        │             │        │          │
        └─────────────┴────────┴──────────┘
            you observe, and prompt for changes
```

## Getting Started

No long reading list. Three files are all you need to drop into your project; your AI coding assistant does the rest.

### Step 1 — Copy these three files into your project

| File | Purpose |
|------|---------|
| [`dsl_first_methodology/KERNEL_DSL.md`](dsl_first_methodology/KERNEL_DSL.md) | Domain structure, state machines, services — the core modeling language |
| [`dsl_first_methodology/BEHAVIOR_DSL.md`](dsl_first_methodology/BEHAVIOR_DSL.md) | How procedures unfold — ordered/parallel steps over a runtime API |
| [`dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md`](dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md) | The practitioner's guide — how to apply the methodology, including DSL families |

> You don't choose between these or wire anything up — make all three available and the assistant applies whichever your domain needs.

### Step 2 — (Optional) Brainstorm the domain into a PRD

This is a **discovery preprocess — it runs *before* any DSL exists**, not part of authoring. You can start here deliberately, but you don't have to remember to: the methodology makes **the assistant check your input and start interviewing on its own when the spec is too thin to model** — that's Phase 0, the [intake gate](dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md#311-phase-0--the-intake-gate-the-assistants-job), and it's the assistant's job, not yours.

The assistant interviews you one question at a time (each with its recommended answer), sharpens vague or overloaded terms into canonical names, probes edge cases with concrete scenarios, and reconciles your answers against any legacy code. It then writes the result up as a **PRD** — glossary, entities and their lifecycles, rules, open questions — for you to review and approve. That approved PRD is the input Step 3 authors the DSL from.

**If you're only a messenger** — relaying for a stakeholder and unable to answer on the spot — ask for a **question list** instead of a live interview, and bring the answers back:

```
I can't answer domain questions myself — I'm relaying to the stakeholders.
Instead of interviewing me, give me a grouped list of the open questions, each
with why it matters and your provisional assumption, so I can collect answers.
```

```
Before writing any DSL, interview me about this domain to produce a PRD. Ask one
question at a time, each with your recommended answer. Sharpen my terminology into
canonical names, probe edge cases with concrete scenarios ("what if an Agent is
assigned a task while already WORKING?"), and challenge anything in my answers that
the legacy code contradicts. When the domain is clear, write it up as a PRD —
glossary, entities and lifecycles, rules, open questions — and wait for my approval.
```

> The PRD states *intent* — your human-facing "spec in". Once Step 3 authors the DSL, **the DSL is the source of truth for what the app actually does**; the PRD is your record of original intent, not a second place to edit behavior. Change behavior DSL-first (prompt → DSL → regenerate); when intent itself changes, refresh the PRD — or regenerate it from the DSL as a derived doc — so the two never silently disagree.

### Step 3 — Author the DSL, then derive

Now point the assistant at the methodology and let it author the DSL (single source of truth) and generate the code, tests, and docs from it:

```
I want to apply the DSL-First methodology to this project. You have all three
methodology files in context — the Kernel DSL and Behavior DSL specs and the
practitioner's guide. Author the DSL from the approved PRD (or, if I skipped the
brainstorm, from the spec or legacy repo I point you at), using whichever DSL the
domain needs.
```

One knob: **how much of its work you check**, from most oversight to least. Append one to the prompt above.

**Step-by-step** — review each DSL file before any code is derived.

```
Produce an initial DSL for [your domain]. Show me each DSL file and wait for my
review before generating any code, tests, or docs.
```

**YOLO** — end-to-end in one pass; fix issues afterwards.

```
Go end-to-end for [your domain]: produce the DSL and derive the whole app from
it — code, tests, and docs, plus whatever the binding needs — in one pass.
I'll review the result.
```

(A Step 2 brainstorm flows straight into either.)

## Project Structure

```
dsl-first/
├── README.md
├── dsl_first_methodology/          # THE METHODOLOGY — copy these into your project
│   ├── KERNEL_DSL.md               #   domain structure + lifecycle (what things are)
│   ├── BEHAVIOR_DSL.md             #   how procedures unfold (how things happen)
│   └── DSL_FIRST_METHODOLOGY_GUIDE.md  #   the practitioner's guide
├── in_depth_docs/                  # For the curious: theory, concepts, case studies
└── examples/                       # Runnable reference implementations (one per binding)
    ├── java-antlr-minimal/         # Grammar-hosted: text DSL → ANTLR → JavaPoet
    └── clojure-edn-minimal/        # Data-hosted: EDN → malli → interpret
```

## Documentation Navigation

### The Methodology (`dsl_first_methodology/`)
* [Kernel DSL Specification v1.1](dsl_first_methodology/KERNEL_DSL.md) — Domain structure, state machines, services. The core modeling language: *what things are*.
* [Behavior DSL Specification v1.0](dsl_first_methodology/BEHAVIOR_DSL.md) — How procedures unfold as ordered/parallel steps over a runtime API: *how things happen*. A separate grammar, because its execution script has no Kernel DSL analog.
* [DSL-First Methodology Guide](dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md) — The practitioner's guide: how to apply the methodology, including DSL families.

### Examples (`examples/`) — runnable, one per binding

The same Agent state machine, implemented both ways — for an implementer building the pipeline by hand:
* [`java-antlr-minimal`](examples/java-antlr-minimal/README.md) — **grammar-hosted**: text DSL → ANTLR parser → JavaPoet → generated `.java`.
* [`clojure-edn-minimal`](examples/clojure-edn-minimal/README.md) — **data-hosted**: EDN model → malli schema → interpret. No grammar, no parser.

### In-Depth Docs (`in_depth_docs/`) — for those peeking into internals
* [The Manifesto](in_depth_docs/manifesto.md) — Why we need DSL-First.
* [Core Concepts](in_depth_docs/core_concepts.md) — How the generator pipeline works.
* [AI Synergy](in_depth_docs/ai_synergy.md) — Why LLMs work better with DSLs than code.
* [Tradeoffs](in_depth_docs/tradeoffs.md) — When you should (and shouldn't) use this.
* [Case study: Provider Integration Is a Domain, Not a Grammar](in_depth_docs/case-study-provider-integration.md) — A worked example of the "when do I add a grammar?" decision.

## Contributing

We are looking for implementers to contribute guides and examples for:
- Python (`astor`)
- TypeScript (`ts-morph`)
- Go (`jennifer`)
- C# (`Roslyn`)

See an issue or want to contribute? Open a PR!

## License

This project is licensed under the [Apache License 2.0](LICENSE).
