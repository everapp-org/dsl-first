# DSL-First Methodology

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](#contributing)
[![Examples](https://img.shields.io/badge/Runnable_Examples-Java%2C%20C%23%2C%20Python%20%26%20Clojure-orange.svg)](examples/)

**Welcome to the home of DSL-First Development.**

Hand your AI coding assistant a written description of what you want (or an existing codebase), and the methodology carries the structured part of the work. The assistant writes a small **model** — a precise description of your application, in a special-purpose language (a **DSL**, domain-specific language) — and an ordinary program, not an AI, expands that model into your application's backbone: the domain classes, the state rules, the workflow skeletons, the tests for all of it, and the documentation. The parts no model can express — the user interface, the bodies of the individual steps, the connections to outside systems — are written as ordinary code that plugs into that backbone. Everything that comes from the model always comes out the same way and cannot drift apart — and producing it costs **cheap computing time instead of expensive AI calls**. **You don't need to know how it works to use it.**

The same model also drives **test automation and QA**: the state machine yields transition coverage, the procedure model yields workflow tests, and the verification model produces living documentation — prose where each claim shows its current pass/fail verdict. See the [QA & Test Automation Guide](dsl_first_methodology/DSL_FIRST_QA_GUIDE.md).

> When the code, the tests, and the docs are all produced from one model, they cannot quietly stop agreeing with each other.

> ⭐ **If you find this methodology useful, please star this repo** — it helps others discover it!
>
> [![Star History Chart](https://api.star-history.com/svg?repos=everapp-org/dsl-first&type=Date)](https://star-history.com/#everapp-org/dsl-first&Date)

## Table of Contents

- [Who is this for?](#who-is-this-for)
- [The problem it solves](#the-problem-it-solves)
- [How it works](#how-it-works)
- [Getting started](#getting-started)
- [Pick one use case](#pick-one-use-case)
- [Project structure](#project-structure)
- [Documentation navigation](#documentation-navigation)
- [Contributing](#contributing)
- [License](#license)

## Who Is This For?

This methodology is designed for **developers using AI coding assistants** (Cursor, GitHub Copilot, Devin (Windsurf), Claude Code, etc.).

The core insight: AI assistants are dramatically more effective when they work from a **structured model** than from natural-language requirements alone. In the DSL-First methodology, the AI coding assistant turns your specification — or your existing codebase — into a small, readable model, written in the languages this methodology provides. That model becomes the single source of truth: the one place where the heart of the application is defined. The assistant then generates the application's backbone from it — code, tests, and documentation that agree with each other.

**You provide intent and review the results; the assistant authors and maintains the model.** You read it when you need to dig in, and edit it directly only when you want precise, hands-on control.

**It is also for the newcomer joining a legacy project.** Point the assistant at the existing code and it distils a small, readable model of the system: what it manages, the states things pass through, and the rules it enforces. Reading that model is a far shorter path into the project's internals than reading the code itself.

**Any host language.** Your app can be in Java, TypeScript, Python, Go, C#, or Clojure. Each is handled a little differently under the hood, but that is the assistant's concern while applying the methodology, not yours.

## The Problem It Solves

In traditional development, the specification, the code, the tests, and the documentation are separate artifacts — they *will* drift apart over time. DSL-First removes that drift from the generated part of the application by producing the code, the tests, and the documentation **from one model**. The hand-written part remains ordinary code — but the backbone it plugs into can no longer lie.

## How It Works

```
  You provide:  a specification, or an existing codebase
        │
        ▼
  AI assistant  ──authors──►  model (single source of truth)
        │                      │
        │                      ▼   derived, always the same way
        │             ┌────────┼─────────┐
        │             ▼        ▼          ▼
        │     core app code  tests       docs
        │             │        │          │
        └─────────────┴────────┴──────────┘
            you observe, and prompt for changes
```

### What gets generated — and what doesn't

The model produces your application's **backbone**:

* **Domain classes** — the things your application manages (orders, customers, payments), with their fields and construction.
* **State rules** — the states each thing can be in, and which changes are allowed. An order that was refunded cannot be shipped again, and the generated code physically refuses to do it.
* **Validation rules and events** — the checks the model declares, and the notifications sent when something changes state.
* **The operations the application offers** — what other code (or other systems) can ask it to do.
* **Workflow skeletons** — the step-by-step procedures: which steps run, in what order, which run side by side, and what happens when a step fails.
* **Tests** — derived from the same model, so they check exactly what the code enforces.
* **Documentation** — the vocabulary, the state diagrams, the reference of operations.

The rest is written as ordinary code that plugs into that backbone:

* **The user interface** — screens, layout, look and feel. It calls the generated operations, but no model can describe how an app should feel.
* **The body of each step** — the model decides that "charge the customer" runs before "ship the order"; the code that actually charges the customer is ordinary code.
* **Connections to outside systems** — payment providers, mail servers, other services.
* **The genuinely clever parts** — pricing logic, matching, optimization. Algorithms are written, not modeled.

The boundary between the two moves in one direction: when a hand-written pattern repeats often enough, it gets promoted into the model. The generated share grows as the project matures.

## Getting Started

No long reading list, and no methodology jargon to memorize. Copy **one folder** into your project; its [`SKILL.md`](dsl_first_methodology/SKILL.md) teaches your AI coding assistant when to interview you, which DSL specifications to load on demand, and when to pause with visual architecture diagrams and technical telemetry reports.

### Step 1 — Copy the `dsl_first_methodology` folder into your project

Drop the [`dsl_first_methodology/`](dsl_first_methodology/) folder into your project — either as an agent skill (so your assistant discovers [`SKILL.md`](dsl_first_methodology/SKILL.md) automatically) or at the project root:

```bash
# Option A: Install as an auto-discovered skill (Claude Code, Codex, Antigravity, Cursor)
mkdir -p .agents/skills
cp -r path/to/dsl-first/dsl_first_methodology .agents/skills/dsl-first

# Option B: Copy the folder directly into your project root
cp -r path/to/dsl-first/dsl_first_methodology ./dsl_first_methodology
```

You do not need to read the files inside the folder — [`SKILL.md`](dsl_first_methodology/SKILL.md) routes your assistant to the right specification only when needed:
* [`SKILL.md`](dsl_first_methodology/SKILL.md) — **The entry point.** Teaches the assistant how to run the methodology, enforce product-vs-example boundaries, and pause at the four checkpoints without requiring you to write technical prompts.
* [`DSL_FIRST_CHECKPOINTS_GUIDE.md`](dsl_first_methodology/DSL_FIRST_CHECKPOINTS_GUIDE.md) — Defines the **four review checkpoints**: the evolving **visual architecture floorplan** (saying what each part *actually does today*), the **measured technical telemetry report** (tools, languages, and LoC breakdown), and the LLM self-reflection check.
* [`DSL_FIRST_METHODOLOGY_GUIDE.md`](dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md) — The complete workflow reference (grammar-hosted and data-hosted bindings, derivation vs. conformance).
* [`KERNEL_DSL.md`](dsl_first_methodology/KERNEL_DSL.md), [`BEHAVIOR_DSL.md`](dsl_first_methodology/BEHAVIOR_DSL.md), [`VERIFICATION_DSL.md`](dsl_first_methodology/VERIFICATION_DSL.md), [`CATALOG_DSL.md`](dsl_first_methodology/CATALOG_DSL.md), [`TARGET_DSL.md`](dsl_first_methodology/TARGET_DSL.md) — The five modeling languages (what your app **manages**, what it **does** step by step, what claims must **hold**, **declaration tables**, and **target language profiles**).
* [`DSL_FIRST_QA_GUIDE.md`](dsl_first_methodology/DSL_FIRST_QA_GUIDE.md) & [`DSL_FIRST_GATES_GUIDE.md`](dsl_first_methodology/DSL_FIRST_GATES_GUIDE.md) — How to apply the models to test automation, living documentation, and build gates (ratchets, baselines, planted failures).

### Step 2 — Talk to your assistant in plain language

Because [`SKILL.md`](dsl_first_methodology/SKILL.md) carries the methodology rules, you can talk about **your software and its specification** in everyday terms:

| What you want to do | What to say to your assistant |
|---|---|
| **Start from an idea (Clarify specification & visual blueprint first)** | *"Use the `dsl-first` skill. Interview me one question at a time about the software I want to build so we can write a clear specification and a visual blueprint diagram before any code is written."* |
| **Build the software with review checkpoints (Recommended)** | *"Build this software using the `dsl-first` skill. Pause at the 4 checkpoints to show me the visual architecture diagram, the technical report (tools, languages, lines of code), and your self-reflection check."* |
| **Build in one pass (YOLO, with full report at the end)** | *"Build this software using the `dsl-first` skill in one pass without stopping, and finish with the visual architecture diagram, the technical report of tools/languages/LoC, and the self-reflection check."* |
| **Extend, add a programming language, or change an existing project** | *"Using the `dsl-first` skill, run the Stop 4 pre-flight check before writing code: show me on the architecture diagram what part of the product this changes, the current technical report, and any issues in existing code before copying it."* |
| **Audit an existing codebase or AI session** | *"Using the `dsl-first` skill, audit this repository and give me the visual architecture floorplan (what each part actually does today), the measured technical report (tools, languages, LoC), and any red flags."* |
| **Relay questions to stakeholders (when you can't answer on the spot)** | *"I'm relaying to stakeholders — instead of interviewing me live, give me a grouped list of the open product questions, each with why it matters and your recommended default."* |

## Pick One Use Case

You do not need all six files, and you do not need to read any of them. **Each file can be used alone, and each starts with a TL;DR** that gives the prompts. Pick the row that is your case, copy its file into your project, and give the assistant the prompt from the TL;DR of that file.

| you want to | copy this file | example that runs |
|---|---|---|
| build a new application from an idea or a specification | [`DSL_FIRST_METHODOLOGY_GUIDE.md`](dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md) + [`KERNEL_DSL.md`](dsl_first_methodology/KERNEL_DSL.md) | [`java-antlr-minimal`](examples/java-antlr-minimal/README.md), [`csharp-antlr-minimal`](examples/csharp-antlr-minimal/README.md), [`clojure-edn-minimal`](examples/clojure-edn-minimal/README.md) |
| get a short, readable model of a legacy codebase | [`KERNEL_DSL.md`](dsl_first_methodology/KERNEL_DSL.md) | — |
| model a step-by-step procedure, with its failure handling | [`BEHAVIOR_DSL.md`](dsl_first_methodology/BEHAVIOR_DSL.md) | — |
| get documentation where each claim shows PASS or FAIL | [`VERIFICATION_DSL.md`](dsl_first_methodology/VERIFICATION_DSL.md) | — |
| generate tests for an existing system | [`DSL_FIRST_QA_GUIDE.md`](dsl_first_methodology/DSL_FIRST_QA_GUIDE.md) | — |
| know that a green test suite can fail | [`DSL_FIRST_QA_GUIDE.md`](dsl_first_methodology/DSL_FIRST_QA_GUIDE.md), §13 only | [`python-toml-gates`](examples/python-toml-gates/README.md), [`java-yaml-gates`](examples/java-yaml-gates/README.md) |
| add a rule to an old project, and not break the build | [`DSL_FIRST_GATES_GUIDE.md`](dsl_first_methodology/DSL_FIRST_GATES_GUIDE.md) | [`python-toml-gates`](examples/python-toml-gates/README.md), [`java-yaml-gates`](examples/java-yaml-gates/README.md) |
| stop defects in the joins between operations | [`KERNEL_DSL.md`](dsl_first_methodology/KERNEL_DSL.md), §3.8 | [`python-toml-gates`](examples/python-toml-gates/README.md), [`java-yaml-gates`](examples/java-yaml-gates/README.md) |
| make visible what is promised and not built | [`KERNEL_DSL.md`](dsl_first_methodology/KERNEL_DSL.md), §3.7 | [`python-toml-gates`](examples/python-toml-gates/README.md), [`java-yaml-gates`](examples/java-yaml-gates/README.md) |
| review AI progress with a visual architecture floorplan, LoC telemetry, and LLM red-flag checks | [`DSL_FIRST_CHECKPOINTS_GUIDE.md`](dsl_first_methodology/DSL_FIRST_CHECKPOINTS_GUIDE.md) | — |
| apply all of it | all methodology files, as in [Getting started](#getting-started) | — |

A dash means that this repository has no runnable example for that case yet.

## Project Structure

```
dsl-first/
├── README.md
├── dsl_first_methodology/          # THE METHODOLOGY — copy this one folder into your project
│   ├── SKILL.md                    #   the entry-point skill: teaches your assistant the workflow & checkpoints
│   ├── KERNEL_DSL.md               #   the language for what your app manages
│   ├── BEHAVIOR_DSL.md             #   the language for what your app does, step by step
│   ├── VERIFICATION_DSL.md         #   the language for what must hold — and living documentation
│   ├── CATALOG_DSL.md              #   the language for declaration tables, bind rules, and formats
│   ├── TARGET_DSL.md               #   the language for target language profiles and @concept mappings
│   ├── DSL_FIRST_METHODOLOGY_GUIDE.md  #   the instructions your assistant follows
│   ├── DSL_FIRST_QA_GUIDE.md       #   how to apply the DSLs to test automation and QA
│   ├── DSL_FIRST_GATES_GUIDE.md    #   how to keep model and code honest: declared gates, ratchets, planted failures
│   └── DSL_FIRST_CHECKPOINTS_GUIDE.md  #   4 review stops: visual architecture floorplan, LoC telemetry, LLM red flags
├── in_depth_docs/                  # for the curious: why it works, when not to use it
├── tools/                          # for contributors only: EBNF grammar gates for all five DSL specs
└── examples/                       # runnable examples — from minimal to advanced
    ├── java-antlr-minimal/         # Java: the model is a text file; the code is generated from it
    ├── csharp-antlr-minimal/       # C# / .NET: the model is a text file; C# is generated via Roslyn
    ├── clojure-edn-minimal/        # Clojure: the model is plain data; nothing is generated
    ├── clojure-edn-contexts/       # Clojure advanced: multi-level interpreters, composition, status-gated wiring
    ├── python-toml-gates/          # Python: the model is TOML; ten declared gates, each proven able to fail
    └── java-yaml-gates/            # Java: the same application and gates; the model is YAML, wired by reflection
```

## Documentation Navigation

### The Methodology (`dsl_first_methodology/`)
* [DSL-First Skill (`SKILL.md`)](dsl_first_methodology/SKILL.md) — The agent skill entry point: teaches your AI coding assistant when to interview you, how to separate product from specimen/transport strata, which DSL specification to load on demand, and how to report at the four visual floorplan checkpoints.
* [Kernel DSL Specification v1.2](dsl_first_methodology/KERNEL_DSL.md) — The language for describing what an application **manages**: the things it keeps track of, the states each can be in, and which state changes are allowed. Since v1.2 also: what each operation receives and returns, what is declared and not built yet, and the joins between operations.
* [Behavior DSL Specification v1.0](dsl_first_methodology/BEHAVIOR_DSL.md) — The language for describing what an application **does**: its procedures, step by step, including which steps may run side by side and what happens when a step fails. It is a separate language because describing a process needs different building blocks than describing things and their states.
* [Verification DSL Specification v1.0](dsl_first_methodology/VERIFICATION_DSL.md) — The language for describing what must **hold**: the checkable claims an application makes about itself, and the **living documentation** derived from them — prose that renders each claim with its current pass/fail verdict. It is a separate language because asserting on a result is foreign to describing things or procedures.
* [Catalog DSL Specification v0.1 (proposal)](dsl_first_methodology/CATALOG_DSL.md) — *proposal on this branch*: the language for **declaration tables** — typed rows of named things (element catalogs, config keys, device matrices, suite manifests), plus `bind` selection rules and `format` name-grammars that generate renderer+parser pairs.
* [Target Profile Specification v0.1](dsl_first_methodology/TARGET_DSL.md) — *on this branch*: the platform-binding specification that maps a platform-independent DSL model to concrete host languages (Rust, Java 21++, Python 3.12, TypeScript, JavaScript ESM), runners (`cargo test`, `JUnit 5`, `pytest`, `@playwright/test`), drivers (`Axum`, `Playwright`, `Selenium`), compile-time tooling (`clippy`, `rustfmt`, JSpecify, Error Prone, NullAway, `tsc`, `mypy`), and GAKS `@concept` ontologies.
* [Case study: qa-tafBase](in_depth_docs/case-study-qa-tafbase.md) — *on this branch*: the field report motivating the proposal — a 91.5k-line test framework where declaration tables were the largest modeling surface the stock trio didn't cover.
* [DSL-First Methodology Guide](dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md) — The instructions the assistant follows from start to finish: checking your input, interviewing you when needed, writing the model, and producing the code, tests, and docs from it.
* [DSL-First QA & Test Automation Guide](dsl_first_methodology/DSL_FIRST_QA_GUIDE.md) — How to apply the three DSL languages to QA work: extracting testable specifications from existing code, generating test suites (Gherkin, step definitions, data factories), and producing living documentation with live verdicts. Covers both grammar-hosted (Java) and data-hosted (Clojure) bindings.
* [DSL-First Gates Guide](dsl_first_methodology/DSL_FIRST_GATES_GUIDE.md) — How the rules that hold a model and its code together are themselves declared, introduced without breaking the build (ratchets), and proven able to fail (planted failures). Also: declaring the joins between operations, declaring what is not built, and a vocabulary that the build enforces. Language-neutral, with Python and Java bindings, each with a runnable example.
* [DSL-First Checkpoints & Architecture Floorplan Guide](dsl_first_methodology/DSL_FIRST_CHECKPOINTS_GUIDE.md) — Four concise review stops across Phases 0–6 where the assistant outputs a visual architecture floorplan (grouped by stratum, with plain-English *"what it does today"* labels), measured technical telemetry (tools, languages, LoC, and AST-driven vs. literal-string generator ratios), and a self-reflection card to catch subpar LLM shortcuts early.

### Examples (`examples/`) — from minimal to advanced, all runnable

* [`java-antlr-minimal`](examples/java-antlr-minimal/README.md) — **Java**: the model is a small text file with its own syntax; a parser reads it and the Java code is generated from it. The generated code is committed, so you can read it without building anything.
* [`csharp-antlr-minimal`](examples/csharp-antlr-minimal/README.md) — **C# / .NET**: the same text DSL parsed by ANTLR 4 on modern .NET, with C# code derived via Roslyn (`SyntaxFactory`). The generated `.cs` files are committed, so you can read them without building anything.
* [`clojure-edn-minimal`](examples/clojure-edn-minimal/README.md) — **Clojure**: the model is plain structured data; the program checks it against a schema and runs it directly. Nothing is generated.
* [`clojure-edn-contexts`](examples/clojure-edn-contexts/README.md) — **Clojure advanced**: multi-level interpretation, `requiring-resolve` dispatch, status-gated wiring, and composition/leasing. Demonstrates techniques extracted from a production codebase, with zero project-specific dependencies.

* [`python-toml-gates`](examples/python-toml-gates/README.md) — **Python**: the model is TOML read with the standard library; an interpreter wires the application from it. Ten gates are declared as data, and a harness puts 14 defects back to prove that each gate fires. No dependency to install.
* [`java-yaml-gates`](examples/java-yaml-gates/README.md) — **Java, data-hosted**: the same application, model and ten gates as the Python example. The model is YAML, an interpreter wires it by reflection, and `mvn verify` runs the gates and 15 planted failures. One run-time dependency (SnakeYAML).

### In-Depth Docs (`in_depth_docs/`) — for those peeking into internals
* [The Manifesto](in_depth_docs/manifesto.md) — Why this methodology exists: the problem of code, tests, and docs drifting apart, and the belief behind the fix.
* [Core Concepts](in_depth_docs/core_concepts.md) — A walkthrough of the machinery: how a model becomes code, tests, and documentation.
* [AI Synergy](in_depth_docs/ai_synergy.md) — Why an AI assistant produces better results writing a small model than writing a whole codebase.
* [Tradeoffs](in_depth_docs/tradeoffs.md) — An honest list of when this methodology pays off and when it is not worth the cost.
* [Case study: Provider Integration Is a Domain, Not a Grammar](in_depth_docs/case-study-provider-integration.md) — A worked example of a real design decision: when a new concern needs its own language, and when it does not.
* [Case study: Context Interpreters — Techniques vs. Code](in_depth_docs/case-study-context-interpreters.md) — How generic interpreter techniques (multi-level interpretation, `requiring-resolve` dispatch, status-gated wiring, composition/leasing) were extracted from a production Clojure codebase into a self-contained example, and why the interpreters themselves don't belong in the methodology distribution.

## Contributing

We are looking for implementers to contribute guides and examples for:
- Python, grammar-hosted (`Lark`, `astor`) — a data-hosted Python example exists: [`python-toml-gates`](examples/python-toml-gates/README.md)
- Java, data-hosted with Jackson, ArchUnit or Gradle — [`java-yaml-gates`](examples/java-yaml-gates/README.md) uses SnakeYAML, the parser of the JDK compiler and Maven; the alternatives in the bindings table of the [Gates Guide](dsl_first_methodology/DSL_FIRST_GATES_GUIDE.md) §13 are not run
- TypeScript (`ts-morph`)
- Go (`jennifer`)

If you change any of the five DSL specifications (`KERNEL_DSL.md`, `BEHAVIOR_DSL.md`, `VERIFICATION_DSL.md`, `CATALOG_DSL.md`, `TARGET_DSL.md`), run `python3 tools/check_kernel_grammar.py` and `python3 tools/check_all_grammars.py` (they need `pip install lark`). They build parsers directly from the EBNF blocks in the specifications and validate every example plus planted failures. The workflow `.github/workflows/check.yml` runs both checks on each pull request. Nobody needs these tools to *use* the methodology.

See an issue or want to contribute? Open a PR!

## License

This project is licensed under the [Apache License 2.0](LICENSE).
