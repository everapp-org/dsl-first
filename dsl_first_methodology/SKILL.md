---
name: dsl-first
description: >-
  Apply the DSL-First software development and QA methodology: interview the user
  to clarify requirements, capture the domain in a structured single-source-of-truth
  model (Kernel, Behavior, Verification, Catalog, and Target Profile DSLs), derive
  deterministic code, tests, living documentation, and build gates, and report
  progress at four checkpoints with a visual architecture floorplan and measured
  technical telemetry (tools, languages, and LoC breakdown). Activate when the user
  wants to specify, build, model, test, gate, audit, or extend an application or
  framework using DSL-First.
---

# DSL-First Methodology Skill

You are applying the **DSL-First Methodology**. All reference specifications live in this folder alongside `SKILL.md`. Load only the files your current step needs (progressive disclosure).

**Core principle:** The **user** speaks in plain product and software terms (what they want to build, who uses it, what to change). **You** handle the methodology internals—authoring the model, separating strata, deriving code/tests/docs, running gates, and presenting visual architecture floorplans with measured technical telemetry at each checkpoint.

---

## 1. Progressive Disclosure — Which File to Read When

Do not load all files at once. Read [DSL_FIRST_CHECKPOINTS_GUIDE.md](./DSL_FIRST_CHECKPOINTS_GUIDE.md) first for the checkpoint format, then load only the specifications relevant to the task:

| When the task involves... | Read this file in this folder |
|---|---|
| **Checkpoints, visual architecture floorplans, LoC telemetry & red-flag checks (Always active)** | [DSL_FIRST_CHECKPOINTS_GUIDE.md](./DSL_FIRST_CHECKPOINTS_GUIDE.md) |
| **Overall workflow, Phase 0 interview, grammar-hosted vs. data-hosted bindings, derivation vs. conformance** | [DSL_FIRST_METHODOLOGY_GUIDE.md](./DSL_FIRST_METHODOLOGY_GUIDE.md) |
| **Entities, fields, state machines, transitions, guards, operation payloads/returns, `not-built` status, wirings** | [KERNEL_DSL.md](./KERNEL_DSL.md) |
| **Step-by-step procedures, workflows, parallel steps, error/timeout handling, `when` conditions** | [BEHAVIOR_DSL.md](./BEHAVIOR_DSL.md) |
| **Checkable claims (`given / when / expect`) and living documentation with PASS/FAIL verdicts** | [VERIFICATION_DSL.md](./VERIFICATION_DSL.md) |
| **Declaration tables (locators, config keys, device matrices, suite manifests), `bind` rules, `format` grammars** | [CATALOG_DSL.md](./CATALOG_DSL.md) |
| **Target language profiles (Java 21++, Python 3.12, TS, JS, C#), runners, drivers, static tooling, `@concept` tags** | [TARGET_DSL.md](./TARGET_DSL.md) |
| **Test automation, extracting models from legacy code, Gherkin/BDD generation, test matrices, ISTQB/QA concepts** | [DSL_FIRST_QA_GUIDE.md](./DSL_FIRST_QA_GUIDE.md) |
| **Build gates, two-sided ratchets, baseline files, planted failures, population floors, vocabulary gates** | [DSL_FIRST_GATES_GUIDE.md](./DSL_FIRST_GATES_GUIDE.md) |

---

## 2. Mandatory Operating Rules (Never Skip)

Even when the user gives a short, informal prompt (*"Build me an app that..."* or *"Extend this repo to C#"*), you must enforce these five guardrails automatically:

1. **Separate the 4 Strata on Day 1 ([DSL_FIRST_CHECKPOINTS_GUIDE.md §1](./DSL_FIRST_CHECKPOINTS_GUIDE.md#1-four-boundary-terms-so-levels-never-collide)):**
   - **S3 (`M2`):** Methodology languages (`KERNEL_DSL`, `BEHAVIOR_DSL`, etc.).
   - **S2 (`M1-product`):** The model of the **deliverable product or framework itself** (owned by the vendor/team). Produces the **Product Deliverable**.
   - **S1 (`M1-instance`):** The model of a **specific consumer application or dogfood test specimen** (owned by the end user or quarantined in a test fixture). Produces an **Instance** (`instances/<specimen>/`), never mixed into the Product Deliverable root.
   - **S0 (`transport`):** Derived serialization files (`EDN → JSON` interop dumps). Never call a transport file "the DSL" or treat it as an authored SSOT.
2. **Phase 0 Intake Gate ([DSL_FIRST_METHODOLOGY_GUIDE.md §3.1.1](./DSL_FIRST_METHODOLOGY_GUIDE.md#311-phase-0--the-intake-gate-the-assistants-job)):**
   - Never proceed silently on a hidden assumption. If the user's input is thin or ambiguous, interview them one question at a time with your recommended answer (or emit a grouped question list if they are relaying for stakeholders, or flag explicit assumptions).
3. **Model Behavior, Never a "Vocabulary-Only Fake":**
   - A `.dsl` file that only lists constant names, env vars, or enum words while the real state transitions and procedures sit as hundreds of lines of literal target-language strings inside a generator script is forbidden.
   - Either model the behavior in `KERNEL_DSL` / `BEHAVIOR_DSL` so the generator derives code from AST nodes, declare unbuilt features openly as `status not-built reason "..."`, or place hand-written runtime substrate in `hand-written/` pinned by a bidirectional conformance test ([DSL_FIRST_METHODOLOGY_GUIDE.md §3.5](./DSL_FIRST_METHODOLOGY_GUIDE.md#35-two-modes-of-governance-derivation-and-conformance)).
4. **Fix at the Source, Never in Generated Output:**
   - Run the generated tests and gates. Trace failures to the **model**, the **generator/interpreter**, or **hand-written glue**—never hand-edit generated files.
5. **Vertical First-Lane Acceptance Before Multi-Lane Fan-Out:**
   - When targeting multiple languages ([TARGET_DSL.md](./TARGET_DSL.md)), complete and verify **Lane 1** (including its top-level deliverable directory tree and outside-consumer smoke test at **Stop 3**) before generating Lane 2+.

---

## 3. The Four Checkpoint Stops (Required Reporting)

By default, pause for user review at the **four stops** below (or, if the user explicitly asks for one-pass / "YOLO" execution, run all the way through Lane 1 and print the complete **Stop 3 Checkpoint Report** at the end).

At every stop, output the three-part report defined in [DSL_FIRST_CHECKPOINTS_GUIDE.md §5](./DSL_FIRST_CHECKPOINTS_GUIDE.md#5-one-page-copy-paste-checkpoint-template-for-the-assistant):
1. **Visual Architecture Floorplan (Mermaid `flowchart LR`):** Grouped by Stratum subgraph; 3 lines per card (**Plain Name** + **"What it does today"** + **`path · tool/lang · measured LoC`**); styled with `planned` (dashed), `built` (solid), `changed` (green), `hotspot` (amber warning), `transport` (dotted).
2. **Measured Technical Telemetry Table:** Actual tools, languages, and measured LoC per stratum, explicitly splitting **Generator AST-driven LoC vs. embedded literal template-string LoC**.
3. **Self-Reflection & Red-Flag Audit:** Answer the stop's honesty questions and flag any amber hotspots.

### Stop 1 — Scope & Blueprint *(After Phase 0/1, before writing any DSL)*
- Clarify with the user what the finished product is, who uses it and how, and quarantine any sample/dogfood test specimen (`S1`) and transport files (`S0`) from the product (`S2`).
- Present the specification (PRD) + **Stop 1 Blueprint Floorplan** (all planned parts dashed) and wait for approval.

### Stop 2 — Model Substance *(After Phase 2/3, before generating code)*
- Author the DSL model(s) (`KERNEL_DSL`, `BEHAVIOR_DSL`, `VERIFICATION_DSL`, `CATALOG_DSL`, `TARGET_DSL`).
- Present the **Stop 2 Model Floorplan** + telemetry counts (entities, states, transitions, procedures, verification checks, `not-built` items) confirming behavioral depth and zero specimen leakage into the product model.

### Stop 3 — First-Lane Derivation & Deliverable Surface *(After Phase 4/5 on Lane 1, before adding Lane 2+)*
- Implement the deriver (generator or interpreter), derive code/tests/docs for **Lane 1**, and run all tests and gates.
- Present the **Stop 3 Build Floorplan**, the top-level product output directory tree, the **Generator AST-vs-template-string LoC ratio**, and an outside-consumer usage snippet. Wait for approval before adding more language lanes.

### Stop 4 — Pre-Flight & Delta Check *(Phase 6: Before and after "extend / add language / change" requests)*
- **Before writing code** on an existing project, inspect the repo and state:
  1. Which stratum (`S2 Product` vs. `S1 User/Specimen Instance`) and model file the request touches.
  2. What deliverable class will appear at the root of the output directory.
  3. Whether existing lanes have any structural hotspots (e.g., specimen files at the product root, or large hardcoded template strings inside emitters) that must **not** be cloned blindly.
- Show the updated Floorplan with green `changed` cards and measured telemetry.

