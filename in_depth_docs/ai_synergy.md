# DSL First and AI: The Perfect Synergy

> **Origin**: Part of the [DSL-First Methodology](https://github.com/everapp-org/dsl-first) open-source project.  
> **License**: Apache 2.0 — see [LICENSE](../LICENSE)

The most exciting aspect of DSL-First is how perfectly it pairs with LLMs. 

## The Problem with LLMs and Code

AI models excel at structured input and output but struggle with massive, sprawling codebases.
* They lose context in large codebases.
* They often generate logically sound but architecturally incorrect code.
* Verifying the output of an LLM code generation takes significant developer effort.

## Why DSLs are Perfect for LLMs

1. **Structured Input/Output**: DSL syntax is formal and unambiguous. This is much easier for an LLM to reason about than arbitrary code.
2. **High-Level Intent**: The model expresses "what" not "how". This matches the LLM's strength (reasoning) and avoids low-level implementation details.
3. **Machine-Checked Output**: The derived code is checked by machines, not by eyeballs — the compiler or a schema rejects anything malformed, and the generated tests check the behavior.

## Human-AI Collaboration Pattern

Instead of asking an AI to "update the codebase to add a suspended state to the agent," you describe that intent and the AI updates the model — then regenerates the code from it. The place where change happens moves from the code to the model.

```
┌──────────────────────────────────┐
│ Human: Describe intent           │
│ "Add delegation capability"      │
└────────────┬─────────────────────┘
             ↓
┌──────────────────────────────────┐
│ AI: Generates DSL                │
│ [States, transitions, guards]    │
└────────────┬─────────────────────┘
             ↓
┌──────────────────────────────────┐
│ Human: Reviews DSL (not code!)   │
│ [Validates domain correctness]   │
└────────────┬─────────────────────┘
             ↓
┌──────────────────────────────────┐
│ Derivation: Produces artifacts   │
│ [Code, tests, docs]              │
└────────────┬─────────────────────┘
             ↓
┌──────────────────────────────────┐
│ Tests: Validate behavior         │
│ [Automatic verification]         │
└──────────────────────────────────┘
```

The human reviews the business intent in the model, and derivation safely produces the thousands of lines of code updates required.
