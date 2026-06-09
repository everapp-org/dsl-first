# DSL-First Methodology

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](#contributing)
[![Java](https://img.shields.io/badge/Reference_Implementation-Java%20%2B%20ANTLR-orange.svg)](quick_start_guides/quick_start_java.md)

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
| [`dsl_first_methodology/BEHAVIOR_DSL.md`](dsl_first_methodology/BEHAVIOR_DSL.md) | How procedures unfold — ordered/parallel steps over a runtime API (copy when you need to model behavior) |
| [`dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md`](dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md) | The practitioner's guide — how to apply the methodology, including DSL families |

> The Kernel DSL alone is enough to start. Add the Behavior DSL only when you actually need to model *how things happen*, not just *what they are*.

### Step 2 — Prompt your AI coding assistant

Open these files in your AI assistant's context and use a prompt like:

```
I want to apply the DSL-First methodology to this project.
You have the Kernel DSL specification and the practitioner's guide in context.
Start by analysing the domain and producing an initial DSL for [your domain].
```

### Step 3 — Choose your pace

- **Step-by-step** — ask the AI to show you each DSL file before generating code from it. Review, refine, then proceed.
- **YOLO** — ask the AI to go end-to-end: produce the DSL, the grammar, the code generator, and the generated code in one go. Fix issues afterwards.

Both modes work. Step-by-step gives you more control over the domain model; YOLO gets you to running code faster.

## Project Structure

```
dsl-first/
├── README.md
├── dsl_first_methodology/          # THE METHODOLOGY — copy these into your project
│   ├── KERNEL_DSL.md               #   domain structure + lifecycle (what things are)
│   ├── BEHAVIOR_DSL.md             #   how procedures unfold (how things happen)
│   └── DSL_FIRST_METHODOLOGY_GUIDE.md  #   the practitioner's guide
├── quick_start_guides/             # Per-paradigm quick-starts (grammar-hosted: Java · data-hosted: Clojure)
├── in_depth_docs/                  # For the curious: theory, concepts, case studies
└── examples/
    └── java-antlr-minimal/         # Reference implementation (Java + ANTLR + JavaPoet)
```

## Documentation Navigation

### The Methodology (`dsl_first_methodology/`)
* [Kernel DSL Specification v1.1](dsl_first_methodology/KERNEL_DSL.md) — Domain structure, state machines, services. The core modeling language: *what things are*.
* [Behavior DSL Specification v1.0](dsl_first_methodology/BEHAVIOR_DSL.md) — How procedures unfold as ordered/parallel steps over a runtime API: *how things happen*. A separate grammar, because its execution script has no Kernel DSL analog.
* [DSL-First Methodology Guide](dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md) — The practitioner's guide: how to apply the methodology, including DSL families.

### Quick-Start Guides (`quick_start_guides/`)

Two bindings of the same methodology — pick the one that matches your host language:
* [Java quick start (ANTLR + JavaPoet)](quick_start_guides/quick_start_java.md) — **grammar-hosted**: text DSL → parser → generated source.
* [Clojure quick start (data, not grammar)](quick_start_guides/quick_start_clojure.md) — **data-hosted**: EDN model → malli schema → interpret or macro. No grammar, no parser.

### In-Depth Docs (`in_depth_docs/`) — for those peeking into internals
* [The Manifesto](in_depth_docs/manifesto.md) — Why we need DSL-First.
* [Core Concepts](in_depth_docs/core_concepts.md) — How the generator pipeline works.
* [AI Synergy](in_depth_docs/ai_synergy.md) — Why LLMs work better with DSLs than code.
* [Tradeoffs](in_depth_docs/tradeoffs.md) — When you should (and shouldn't) use this.
* [Case study: Provider Integration Is a Domain, Not a Grammar](in_depth_docs/case-study-provider-integration.md) — A worked example of the "when do I add a grammar?" decision.

### Examples
* `java-antlr-minimal` - A reference architecture for a Java DSL generator.

## Contributing

We are looking for implementers to contribute guides and examples for:
- Python (`astor`)
- TypeScript (`ts-morph`)
- Go (`jennifer`)
- C# (`Roslyn`)

See an issue or want to contribute? Open a PR!

## License

This project is licensed under the [Apache License 2.0](LICENSE).
