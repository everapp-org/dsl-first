# DSL-First Methodology

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](#contributing)
[![Examples](https://img.shields.io/badge/Runnable_Examples-Java%20%26%20Clojure-orange.svg)](examples/)

**Welcome to the home of DSL-First Development.**

Hand your AI coding assistant a written description of what you want (or an existing codebase), and the methodology carries the structured part of the work. The assistant writes a small **model** — a precise description of your application, in a special-purpose language (a **DSL**, domain-specific language) — and an ordinary program, not an AI, expands that model into your application's backbone: the domain classes, the state rules, the workflow skeletons, the tests for all of it, and the documentation. The parts no model can express — the user interface, the bodies of the individual steps, the connections to outside systems — are written as ordinary code that plugs into that backbone. Everything that comes from the model always comes out the same way and cannot drift apart — and producing it costs **cheap computing time instead of expensive AI calls**. **You don't need to know how it works to use it.**

> When the code, the tests, and the docs are all produced from one model, they cannot quietly stop agreeing with each other.

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

No long reading list. Three files are all you need to drop into your project; your AI coding assistant does the rest.

### Step 1 — Copy these three files into your project

These files are not for you to study — they are the instructions your AI coding assistant works from. Copy all three into your project; the assistant applies whichever it needs.

[`dsl_first_methodology/KERNEL_DSL.md`](dsl_first_methodology/KERNEL_DSL.md) teaches your assistant the language for describing what your application **manages**: the things it keeps track of (orders, customers, payments), the states each thing can be in (an order can be *placed*, *paid*, *shipped*, or *refunded*), and which state changes are allowed (a refunded order cannot be shipped again).

[`dsl_first_methodology/BEHAVIOR_DSL.md`](dsl_first_methodology/BEHAVIOR_DSL.md) teaches your assistant the language for describing what your application **does**: its step-by-step procedures — for example, "when an order is placed: check the stock, charge the customer, then ship; if charging fails, cancel the order."

[`dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md`](dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md) gives your assistant its working instructions: when to interview you about your project, how to write the model from what you provide, and how to produce the code, the tests, and the docs from that model.

### Step 2 — (Optional) Brainstorm: from a vague idea to a requirements document

This step is for when you don't yet have a written specification — it runs **before any model exists**. You can start here deliberately, but you don't have to remember to: the methodology makes **the assistant check your input and start interviewing on its own when what you gave it is too thin to work from** — that's Phase 0, the [intake gate](dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md#311-phase-0--the-intake-gate-the-assistants-job), and it's the assistant's job, not yours.

The assistant interviews you one question at a time (each with its recommended answer), turns vague or double-duty words into one agreed name per thing, probes edge cases with concrete scenarios, and checks your answers against any existing code. It then writes the result up as a **PRD** — a Product Requirements Document: a short, structured statement of what to build, covering the agreed vocabulary, the things your application manages and the states they pass through, the rules, and the questions still open. You review and approve it, and that approved PRD is what Step 3 works from.

**If you're only a messenger** — relaying for a stakeholder and unable to answer on the spot — ask for a **question list** instead of a live interview, and bring the answers back:

```
I can't answer domain questions myself — I'm relaying to the stakeholders.
Instead of interviewing me, give me a grouped list of the open questions, each
with why it matters and your provisional assumption, so I can collect answers.
```

```
Before writing any model, interview me about this domain to produce a PRD. Ask one
question at a time, each with your recommended answer. Sharpen my terminology into
one agreed name per concept, probe edge cases with concrete scenarios ("what if a
customer cancels an order that has already shipped?"), and challenge anything in my
answers that the existing code contradicts. When the domain is clear, write it up
as a PRD — vocabulary, the things managed and their states, rules, open questions —
and wait for my approval.
```

> The PRD states *intent* — what you asked for. Once Step 3 authors the model, **the model is the source of truth for what the app actually does**; the PRD is your record of original intent, not a second place to edit behavior. To change behavior, prompt the assistant: it updates the model and regenerates the code. When the intent itself changes, refresh the PRD — or have it regenerated from the model as a derived document — so the two never silently disagree.

### Step 3 — Let the assistant author the model and generate the rest

Now point the assistant at the methodology. It writes the model — the single source of truth — produces the code, the tests, and the docs from it, runs the tests, and fixes whatever fails:

```
I want to apply the DSL-First methodology to this project. You have all three
methodology files in context — the Kernel DSL and Behavior DSL specifications and
the methodology guide. Author the model from the approved PRD (or, if I skipped the
brainstorm, from the specification or existing code I point you at), using whichever
language the domain needs. Then generate the code, tests, and docs, run the tests,
and trace any failure to its real cause — fixing it at its source (the model if a
rule is wrong, the generator if the expansion is wrong), never by editing generated
code.
```

One knob: **how much of its work you check**, from most oversight to least. Append one to the prompt above.

**Step-by-step** — review each model file before any code is generated.

```
Show me each model file and wait for my approval before generating any code,
tests, or docs.
```

**YOLO** — the assistant goes all the way to a finished result in one pass; you review at the end.

```
Go all the way in one pass, without stopping for review. I'll check the
finished result.
```

(If you did the Step 2 brainstorm, its approved PRD feeds straight into either mode.)

## Project Structure

```
dsl-first/
├── README.md
├── dsl_first_methodology/          # THE METHODOLOGY — copy these three files into your project
│   ├── KERNEL_DSL.md               #   the language for what your app manages
│   ├── BEHAVIOR_DSL.md             #   the language for what your app does, step by step
│   └── DSL_FIRST_METHODOLOGY_GUIDE.md  #   the instructions your assistant follows
├── in_depth_docs/                  # for the curious: why it works, when not to use it
└── examples/                       # the same small app built two ways — both runnable
    ├── java-antlr-minimal/         # Java: the model is a text file; the code is generated from it
    └── clojure-edn-minimal/        # Clojure: the model is plain data; nothing is generated
```

## Documentation Navigation

### The Methodology (`dsl_first_methodology/`)
* [Kernel DSL Specification v1.1](dsl_first_methodology/KERNEL_DSL.md) — The language for describing what an application **manages**: the things it keeps track of, the states each can be in, and which state changes are allowed.
* [Behavior DSL Specification v1.0](dsl_first_methodology/BEHAVIOR_DSL.md) — The language for describing what an application **does**: its procedures, step by step, including which steps may run side by side and what happens when a step fails. It is a separate language because describing a process needs different building blocks than describing things and their states.
* [DSL-First Methodology Guide](dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md) — The instructions the assistant follows from start to finish: checking your input, interviewing you when needed, writing the model, and producing the code, tests, and docs from it.

### Examples (`examples/`) — the same small app, built two ways

Both are runnable, and both implement the same example — a small agent that can be activated and assigned tasks — for developers who want to see exactly what the assistant builds under the hood:
* [`java-antlr-minimal`](examples/java-antlr-minimal/README.md) — **Java**: the model is a small text file with its own syntax; a parser reads it and the Java code is generated from it. The generated code is committed, so you can read it without building anything.
* [`clojure-edn-minimal`](examples/clojure-edn-minimal/README.md) — **Clojure**: the model is plain structured data; the program checks it against a schema and runs it directly. Nothing is generated.

### In-Depth Docs (`in_depth_docs/`) — for those peeking into internals
* [The Manifesto](in_depth_docs/manifesto.md) — Why this methodology exists: the problem of code, tests, and docs drifting apart, and the belief behind the fix.
* [Core Concepts](in_depth_docs/core_concepts.md) — A walkthrough of the machinery: how a model becomes code, tests, and documentation.
* [AI Synergy](in_depth_docs/ai_synergy.md) — Why an AI assistant produces better results writing a small model than writing a whole codebase.
* [Tradeoffs](in_depth_docs/tradeoffs.md) — An honest list of when this methodology pays off and when it is not worth the cost.
* [Case study: Provider Integration Is a Domain, Not a Grammar](in_depth_docs/case-study-provider-integration.md) — A worked example of a real design decision: when a new concern needs its own language, and when it does not.

## Contributing

We are looking for implementers to contribute guides and examples for:
- Python (`astor`)
- TypeScript (`ts-morph`)
- Go (`jennifer`)
- C# (`Roslyn`)

See an issue or want to contribute? Open a PR!

## License

This project is licensed under the [Apache License 2.0](LICENSE).
