# DSL-First Methodology

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)
[![Java](https://img.shields.io/badge/Reference_Implementation-Java%20%2B%20ANTLR-orange.svg)](guides/quick_start_java.md)

**Welcome to the home of DSL-First Development.**

A methodology where a **Domain-Specific Language (DSL)** serves as the *single source of truth*. Unlike traditional spec-driven or code-first approaches, the DSL is executable — once crafted, it drives a grammar and a code generator that deterministically produce type-safe code and exhaustive tests.

> When your specification (DSL) DRIVES your code generator, specification drift becomes impossible.

> ⭐ **If you find this methodology useful, please star this repo** — it helps others discover it!
>
> [![Star History Chart](https://api.star-history.com/svg?repos=everapp-org/dsl-first&type=Date)](https://star-history.com/#everapp-org/dsl-first&Date)

## Who Is This For?

This methodology is designed for **developers using AI coding assistants** (Cursor, GitHub Copilot, Windsurf, Claude, etc.).

The core insight: AI assistants are dramatically more effective when given a **structured DSL** to work from rather than vague natural-language requirements. Instead of prompting your AI to "write some code", you define your domain in a DSL — and your AI assistant builds the grammar, the code generator, and then generates all the boilerplate deterministically from that single source of truth.

## The Problem It Solves

In traditional development, the spec, the code, the tests, and the documentation are separate artifacts — they *will* drift apart over time. DSL-First eliminates drift by making **one artifact generate all the others**.

## How It Works

```
  Your DSL file
       │
       ▼
  ANTLR Grammar  ──►  Parser / AST
                            │
                            ▼
                     Code Generator
                      ┌─────┴──────┐
                      ▼            ▼
               Type-safe code   Exhaustive tests
               (Java, TS, Go…)  (100% transition coverage)
```

## Getting Started

There is no long reading list before you begin. Two files are all you need to drop into your project and then let your AI coding assistant do the heavy lifting.

### Step 1 — Copy these two files into your project

| File | Purpose |
|------|---------|
| [`specification/KERNEL_DSL.md`](specification/KERNEL_DSL.md) | The domain-agnostic modeling language — defines the DSL syntax your AI will write |
| [`DSL_FIRST_DEVELOPMENT_GUIDE.md`](DSL_FIRST_DEVELOPMENT_GUIDE.md) | The practitioner's guide — tells your AI assistant how to apply the methodology |

### Step 2 — Prompt your AI coding assistant

Open both files in your AI assistant's context and use a prompt like:

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
dsl-first-methodology/
├── DSL_FIRST_DEVELOPMENT_GUIDE.md  # Practitioner's guide — copy this to your project
├── docs/                           # Theory, concepts, tradeoffs, AI synergy
├── specification/
│   └── KERNEL_DSL.md               # Modeling language spec — copy this to your project
├── guides/                         # Language-specific quick-start guides
└── examples/
    └── java-antlr-minimal/         # Reference implementation (Java + ANTLR + JavaPoet)
```

## Documentation Navigation

### Theory & Concepts
* [The Manifesto](docs/manifesto.md) - Why we need DSL First.
* [Core Concepts](docs/core_concepts.md) - How the generator pipeline works.
* [AI Synergy](docs/ai_synergy.md) - Why LLMs work better with DSLs than Code.
* [Tradeoffs](docs/tradeoffs.md) - When you should (and shouldn't) use this.
* [The Comprehensive Guide](docs/comprehensive_guide.md) - A massively detailed practitioner's guide to building and integrating your methodology pipeline.

### The Formal Language
* [Kernel DSL Specification v1.1](specification/KERNEL_DSL.md) - The domain-agnostic modeling language specification and EBNF grammar used as the backbone of the methodology.

### Implementation Guides
* [Java quick start (ANTLR + JavaPoet)](guides/quick_start_java.md)

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
