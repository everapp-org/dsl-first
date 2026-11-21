# Target Profile Specification (v0.1 — Platform Binding & Concept Mapping)

> **Origin**: Part of the [DSL-First Methodology](https://github.com/everapp-org/dsl-first) open-source project.
> **License**: Apache 2.0 — see [LICENSE](../LICENSE)

**Version:** 0.1  
**Companions:** [Kernel DSL v1.2](KERNEL_DSL.md) · [Behavior DSL v1.0](BEHAVIOR_DSL.md) · [Verification DSL v1.0](VERIFICATION_DSL.md) · [Catalog DSL v0.1](CATALOG_DSL.md) · [Gates Guide](DSL_FIRST_GATES_GUIDE.md)  
**Purpose:** The Kernel, Behavior, Verification, and Catalog DSLs define a **Platform-Independent Model (PIM)** — what the domain manages, what procedures it runs, what claims must hold, and what declaration tables exist. When a generator or emitter translates that PIM into executable host code (Java, Python, TypeScript, JavaScript, Clojure), it requires a **Target Profile (Platform Binding)**: an explicit declaration of the target language version, runner, driver, compile-time tooling, complexity budgets, and the mapping from abstract programming/QA archetypes to concrete target-language constructs.

---

## TL;DR — this file can be used alone

**What it gives you.** A declarative **Target Profile** (`target <name> { ... }`) that governs *how* an emitter translates a DSL model into one or more host languages, which language constructs it prefers or refuses, and which `@concept` provenance tags it stamps into generated files.

**Prerequisites: none.** Every term this file uses is defined below.

**Use it now.** Give this file to your AI coding assistant with one of these prompts.

| you want to | say to the assistant |
|---|---|
| define a target lane for code generation | *"Read TARGET_DSL.md. Write a `target` profile for our project (language version, test runner, driver, compile-time checks, preferred constructs, and refused anti-patterns). Show me the profile before generating code."* |
| emit multiple language lanes from one model | *"Read TARGET_DSL.md. Using our single DSL model, generate deterministic implementations for the declared `target` profiles (`python-312`, `java-21pp`, `ts-pwtest`, `js-esm-pwtest`) and stamp `@concept` tags for every realized construct."* |
| upgrade a target language baseline (e.g. Java 11 → Java 21++) | *"Read TARGET_DSL.md §4 and §6. Update our Java target profile to Java 21++ (`record`, `sealed interface`, pattern-matching `switch`, JSpecify `@NullMarked` + NullAway), regenerate the code, and run the build gates."* |
| propose a new language construct | *"Read TARGET_DSL.md §7. Open a `[!decision]` proposal comparing the candidate construct against our current Target Profile before changing the emitter."* |

**The whole specification in one example.**

```dsl
target java_21pp {
    description "Modern Java 21 LTS ('Java++') + JUnit 5 + Playwright + JSpecify/NullAway"
    language    java "21"
    runner      junit "5.11"
    driver      playwright "1.49"

    catalogs    prog, java, istqb, qa, playwright, junit

    tooling {
        nullability   jspecify "1.0.0" package_default NullMarked
        static_check  error_prone "2.36.0", nullaway "0.12.3"
        compiler_args "-Werror", "-XDcompilePolicy=simple"
    }

    constructs {
        // Archetype -> Target realization
        realize CONCEPT-prog-v1-001-01-immutable-data-carrier       with CONCEPT-java-v1-002-01-records
        realize CONCEPT-prog-v1-002-01-closed-type-union            with CONCEPT-java-v1-004-01-sealed-classes
        realize CONCEPT-prog-v1-003-01-exhaustive-branching         with CONCEPT-java-v1-006-01-pattern-matching-instanceof-and-switch
        realize CONCEPT-prog-v1-004-01-null-safe-optionality        with CONCEPT-java-v1-019-01-jspecify-and-nullaway
        realize CONCEPT-prog-v1-005-01-scoped-resource-cleanup      with CONCEPT-java-v1-012-01-try-with-resources-and-cleaner
        realize CONCEPT-prog-v1-006-01-scoped-test-fixture          with CONCEPT-junit-v1-001-01-per-method-test-instance-lifecycle
        realize CONCEPT-prog-v1-008-01-immutable-collection-literal with CONCEPT-java-v1-011-01-immutable-collection-factories
        realize CONCEPT-prog-v1-010-01-external-environment-binding with CONCEPT-java-v1-031-01-system-getenv-binding

        prefer  CONCEPT-java-v1-003-01-local-variable-type-inference
        prefer  CONCEPT-java-v1-008-01-text-blocks
        prefer  CONCEPT-java-v1-009-01-virtual-threads

        avoid   CONCEPT-java-v1-030-01-double-brace-initialization-antipattern
        avoid   CONCEPT-java-v1-028-01-manifold-compiler-plugin-tradeoffs
    }

    budget {
        max_method_lines        40
        max_nesting_depth       3
        max_parameters          5
    }
}
```

---

## 1. Why a Target Profile Exists

Without an explicit Target Profile, an AI coding assistant or code generator makes silent, ad-hoc platform choices inside emitter templates:
- It emits Java 8 mutable POJOs with getters/setters when the project runs on Java 21 (`record` + `sealed interface`).
- It emits raw `null` returns without nullability annotations or mixes `Optional<T>` into class fields.
- It emits `Thread.sleep(...)` or brittle XPath locators in one language lane while using semantic role locators and auto-waiting in another.

Separating the **Domain Model (PIM)** from the **Target Profile (Platform Binding)** gives three guarantees:
1. **One domain model, many target lanes.** The domain files (`kernel.dsl`, `behaviors.dsl`, `verification.dsl`, `catalog.dsl`) stay 100% free of host-language syntax. Switching from Java 11 to Java 21++, or adding Python, TypeScript, and JavaScript lanes, touches only the Target Profile and the emitter bound to it.
2. **Ontological traceability (`@concept`).** Every construct the emitter writes is chosen by mapping an abstract **Tier 1 Archetype** (`CONCEPT-prog-v1-*` or `CONCEPT-qa-v1-*`) to the Target Profile's **Tier 2 Realization** (`CONCEPT-java-v1-*`, `CONCEPT-python-v1-*`, `CONCEPT-ts-v1-*`, `CONCEPT-js-v1-*`, `CONCEPT-playwright-v1-*`, `CONCEPT-selenium-v1-*`). When paired with GAKS, the emitter stamps `# @concept <ID>` or `// @concept <ID>` into the generated code so a checker (`gaks_check.py --code`) verifies every link and learners can read the concept pages side by side.
3. **Governed evolution.** New language features (e.g., from a conference presentation like *"Java++"* or a new JDK/Python/TypeScript release) do not sneak into half the codebase by accident; they enter through a reviewable Target Profile update.

---

## 2. Metamodel (M2): Core Concepts

### 2.1 `target`
A named platform binding configuration. A project with a single implementation language declares one `target`; a multi-lane reference project (such as `tafbase`) declares one `target` per lane (`python_312`, `java_21pp`, `ts_pwtest`, `js_esm_pwtest`).

### 2.2 `language`, `runner`, and `driver`
- `language <id> "<version>"` — the host compiler or runtime baseline (`java "21"`, `python "3.12"`, `typescript "5.6"`, `javascript "ES2024"`).
- `runner <id> "<version>"` — the test or execution harness (`junit "5.11"`, `pytest "8.3"`, `pwtest "1.49"`).
- `driver <id> "<version>"` — the automation or protocol engine (`playwright "1.49"`, `selenium "4.27"`, `http "std"`).

### 2.3 `catalogs`
The list of shared GAKS concept catalogs imported by this target lane (e.g., `prog, java, istqb, qa, playwright, junit`). In a GAKS-enabled repository, these map directly to the `"catalogs"` array in `gaks.project.json`.

### 2.4 `tooling`
Compile-time and static-analysis gates enforced on all generated and hand-written code in this lane (e.g., JSpecify `@NullMarked` + Error Prone + NullAway for Java; `mypy --strict` / `ruff` for Python; `tsc --noEmit --strict` for TypeScript; `tsc --allowJs --checkJs` for JavaScript ESM).

### 2.5 `constructs` (`realize`, `prefer`, `avoid`)
- `realize <Archetype-ID> with <Realization-ID>` — binds a language-neutral Tier 1 archetype (`CONCEPT-prog-v1-*` or `CONCEPT-qa-v1-*`) to the exact Tier 2 language/tool concept (`CONCEPT-<lang>-v1-*`) the emitter must produce.
- `prefer <Concept-ID>` — enables idiopathic target constructs that do not require a 1-to-1 cross-language archetype (e.g., `CONCEPT-java-v1-003-01-local-variable-type-inference`, `CONCEPT-java-v1-008-01-text-blocks`).
- `avoid <Concept-ID>` — explicitly forbids legacy or hazardous constructs (`standing: discouraged`), such as `CONCEPT-java-v1-030-01-double-brace-initialization-antipattern` or `CONCEPT-java-v1-028-01-manifold-compiler-plugin-tradeoffs`.

### 2.6 `budget`
Quantitative readability and complexity bounds enforced on generated and hand-written code in the lane (`max_method_lines`, `max_nesting_depth`, `max_parameters`).

---

## 3. Formal Grammar (EBNF)

```ebnf
start            = target_block+

target_block     = "target" NAME "{" target_body "}"
target_body      = (description | language_decl | runner_decl | driver_decl
                   | catalogs_decl | tooling_block | constructs_block | budget_block)+

description      = "description" STRING
language_decl    = "language" NAME STRING
runner_decl      = "runner" NAME STRING
driver_decl      = "driver" NAME STRING
catalogs_decl    = "catalogs" NAME ("," NAME)*

tooling_block    = "tooling" "{" tooling_entry+ "}"
tooling_entry    = NAME value (","? value)*

constructs_block = "constructs" "{" construct_rule+ "}"
construct_rule   = "realize" CONCEPT_ID "with" CONCEPT_ID
                 | "prefer" CONCEPT_ID
                 | "avoid" CONCEPT_ID

budget_block     = "budget" "{" budget_entry+ "}"
budget_entry     = NAME INTEGER

value            = STRING | INTEGER | BOOLEAN | NAME
CONCEPT_ID       = /CONCEPT-[a-z0-9]+-v[0-9]+-[0-9]{3}-[0-9]{2}-[a-z0-9-]+/
NAME             = /[A-Za-z_][A-Za-z0-9_.-]*/
STRING           = ESCAPED_STRING
INTEGER          = /[0-9]+/
BOOLEAN          = "true" | "false"

%import common.ESCAPED_STRING
%import common.WS
%ignore WS
COMMENT          : "//" /[^\n]*/
%ignore COMMENT
```

*Data-hosted note:* Just like the Kernel and Gates specifications, a Target Profile may be written in the text grammar above (`targets.dsl`), or as structured data in JSON/YAML/TOML/EDN (`targets.json` / `targets.toml` / `targets.edn`) with identical keys.

---

## 4. The Cross-Language Concept Mapping Matrix (Rosetta Stone)

When an emitter translates a DSL model across multiple target lanes, every M2 construct maps first to a **Tier 1 Archetype** and then, via the active `target` profile, to the **Tier 2 Realization**:

### 4.1 Programming Archetypes (`CONCEPT-prog-v1-*`)

| DSL Construct (M2) | Tier 1 Archetype (`prog`) | Java 21++ (`java`) | Python 3.12 (`python`) | TypeScript (`ts`) | JavaScript ESM (`js`) |
|---|---|---|---|---|---|
| `model` / `entry` value carrier | `CONCEPT-prog-v1-001-01-immutable-data-carrier` | `CONCEPT-java-v1-002-01-records` (`record`) | `CONCEPT-python-v1-001-01-frozen-dataclass` (`@dataclass(frozen=True, slots=True)`) | `CONCEPT-ts-v1-001-01-readonly-interface-and-type` (`readonly` interface / `as const`) | `CONCEPT-js-v1-001-01-object-freeze-and-destructuring` (`Object.freeze({...})`) |
| `states` / outcome variants | `CONCEPT-prog-v1-002-01-closed-type-union` | `CONCEPT-java-v1-004-01-sealed-classes` (`sealed interface ... permits`) | `CONCEPT-python-v1-002-01-union-type` (`X \| Y` union) | `CONCEPT-ts-v1-002-01-discriminated-union` (`type U = A \| B`) | `CONCEPT-js-v1-002-01-tagged-object-discriminant` (`{ kind: '...' }`) |
| `transitions` / `bind` dispatch | `CONCEPT-prog-v1-003-01-exhaustive-branching` | `CONCEPT-java-v1-006-01-pattern-matching-instanceof-and-switch` + `005-01-switch-expressions` | `CONCEPT-python-v1-003-01-structural-pattern-matching` (`match`/`case` + `assert_never`) | `CONCEPT-ts-v1-003-01-exhaustive-switch-never` (`switch` + `never`) | `CONCEPT-js-v1-003-01-switch-with-default-throw` (`switch` + default `throw`) |
| `optional` field / nullable return | `CONCEPT-prog-v1-004-01-null-safe-optionality` | `CONCEPT-java-v1-019-01-jspecify-and-nullaway` (`@NullMarked`, `@Nullable`) + `013-01-optional-discipline` | `CONCEPT-python-v1-004-01-none-aware-typing` (`T \| None`, strict mypy) | `CONCEPT-ts-v1-004-01-strict-null-checks` (`strictNullChecks`) | `CONCEPT-js-v1-004-01-nullish-coalescing-and-optional-chaining` (`?.`, `??`) |
| Scoped session / client handle | `CONCEPT-prog-v1-005-01-scoped-resource-cleanup` | `CONCEPT-java-v1-012-01-try-with-resources-and-cleaner` (`try (...)`) | `CONCEPT-python-v1-005-01-context-manager` (`with ...:`) | `CONCEPT-ts-v1-005-01-using-disposable` (`using` / `try-finally`) | `CONCEPT-js-v1-005-01-try-finally-and-explicit-resource-management` |
| Scoped test fixture | `CONCEPT-prog-v1-006-01-scoped-test-fixture` | `CONCEPT-java-v1-001-01-junit-lifecycle-annotations` (`@BeforeEach`/`@AfterEach`) | `CONCEPT-python-v1-006-01-pytest-yield-fixture` (`@pytest.fixture` + `yield`) | `CONCEPT-ts-v1-006-01-typed-test-fixture-extension` (`test.extend<T>()`) | `CONCEPT-js-v1-006-01-async-fixture-callback` (`async ({ page }, use) =>`) |
| `parallelForEach` / fan-out | `CONCEPT-prog-v1-007-01-parallel-fan-out` | `CONCEPT-java-v1-009-01-virtual-threads` (`newVirtualThreadPerTaskExecutor`) | `CONCEPT-python-v1-007-01-taskgroup-and-executor` (`TaskGroup` / `Executor`) | `CONCEPT-ts-v1-007-01-typed-promise-concurrency` (`Promise.all`) | `CONCEPT-js-v1-007-01-promise-all-and-allsettled` (`Promise.all`) |
| Static catalog tables / lists | `CONCEPT-prog-v1-008-01-immutable-collection-literal` | `CONCEPT-java-v1-011-01-immutable-collection-factories` (`List.of`, `Map.of`, `Stream.toList()`) | `CONCEPT-python-v1-008-01-tuple-and-frozenset` (`tuple`, `frozenset`, `MappingProxyType`) | `CONCEPT-ts-v1-008-01-readonly-array-and-const-tuple` (`ReadonlyArray<T>`, `as const`) | `CONCEPT-js-v1-008-01-frozen-array-and-map-set` (`Object.freeze([...])`) |
| Guard / predicate / transform | `CONCEPT-prog-v1-009-01-higher-order-function` | `CONCEPT-java-v1-014-01-lambdas-and-method-references` (`Predicate<T>`, `::`) | `CONCEPT-python-v1-009-01-callable-and-protocol` (`Callable`, `Protocol`) | `CONCEPT-ts-v1-009-01-generic-function-signature` (`(x: T) => boolean`) | `CONCEPT-js-v1-009-01-arrow-functions-and-closures` (`(x) => ...`) |
| `env:NAME` / `env-pool:NAME` | `CONCEPT-prog-v1-010-01-external-environment-binding` | `CONCEPT-java-v1-031-01-system-getenv-binding` (`System.getenv`) | `CONCEPT-python-v1-010-01-os-environ-get` (`os.environ.get`) | `CONCEPT-ts-v1-010-01-typed-env-config` (`process.env`) | `CONCEPT-js-v1-010-01-process-env-binding` (`process.env.KEY ?? ...`) |

### 4.2 QA & Test Automation Archetypes (`CONCEPT-istqb-v1-*` → `CONCEPT-qa-v1-*` → Tool/Runner)

| ISTQB Foundation (`istqb`) | QA Archetype (`qa`) | Playwright (`playwright`) | Selenium 4 (`selenium`) | Runner Realization (`junit` / `pytest` / `pwtest`) |
|---|---|---|---|---|
| `012-01-test-oracle`, `014-01-entry-and-exit-criteria` | `001-01-arrange-act-assert` | Setup → Action → `expect(locator)` | Setup → Action → `wait.until` + `assertEquals` | JUnit `@Test` / pytest `def test_*` / PW `test(...)` |
| `005-01-test-types` | `002-01-the-locator-ladder` | `002-01-semantic-locators` (`getByRole`, `getByLabel`, `getByTestId`) | `002-01-by-locators-and-webelement` (`By.id`, `By.cssSelector`, `RelativeLocator`) | — |
| `006-01-static-vs-dynamic-testing` | `003-01-synchronization-and-actionability` | `003-01-auto-waiting-and-actionability` | `003-01-webdriverwait-and-expected-conditions` | — |
| `013-01-confirmation-vs-regression-testing` | `004-01-session-and-context-isolation` | `001-01-browser-context-page-hierarchy` + `008-01-storage-state-reuse` | `001-01-webdriver-session-lifecycle` + `010-01-remote-webdriver-and-grid` | `junit-001` / `pytest-001` / `pwtest-001` |
| `004-01-test-levels`, `014-01-entry-and-exit-criteria` | `006-01-api-fixture-seeding` | `005-01-api-request-context` | `005-01-external-http-seeding` | Fixture precondition |
| `012-01-test-oracle` | `007-01-hard-vs-soft-assertions` | `004-01-web-first-assertions` | `004-01-one-shot-dom-assertions` | `junit-003-assertall` / `pytest-003-assert` / `pwtest-003-expect-soft` |
| `001-01-error-defect-failure`, `015-01-defect-lifecycle` | `008-01-failure-funnel-and-artifacts` | `006-01-trace-viewer-and-artifacts` | `006-01-takes-screenshot-and-logs` | `junit-004-testwatcher` / `pytest-004-makereport` / `pwtest-004-automatic-trace` |
| `007-01-equivalence-partitioning`, `008-01-boundary-value-analysis` | Data-driven parameterization | — | — | `junit-002-parameterized-test` / `pytest-002-mark-parametrize` / `pwtest-002-data-driven-test-loop` |

---

## 5. Reference Target Profiles for Multi-Lane Projects

Below are the four standard Target Profiles used in multi-lane QA and application codebases:

### 5.1 Lane 1: Python 3.12 + pytest + Playwright (`python_312`)
- **Language & Tooling:** Python 3.12+, `pytest` 8.x, `playwright` Python sync API, `ruff` / `mypy --strict`.
- **Key Realizations:** `@dataclass(frozen=True, slots=True)` for all catalog and state records; `match`/`case` for transition dispatch; `@pytest.fixture` with `yield` for `BrowserContext` / `Page` lifecycle; `os.environ.get` for `env:` bindings.

### 5.2 Lane 2: Modern Java 21++ + JUnit 5 + Playwright (`java_21pp`)
- **Language & Tooling:** Java 21 LTS (forward-compatible with Java 25), JUnit Jupiter 5.11+, Playwright Java 1.49+, **JSpecify 1.0.0** (`@NullMarked` at package level, `@Nullable` on optional components), **Google Error Prone** + **Uber NullAway** configured in Maven `maven-compiler-plugin`.
- **Key Realizations:** `record` with compact constructors for immutable data carriers; `sealed interface ... permits ...` + exhaustive switch expressions with record patterns; `List.of` / `Map.of` / `Stream.toList()` for unmodifiable collections; `var` for local variables with clear right-hand types; multi-line text blocks `"""..."""`; `try-with-resources` on `Playwright` / `Browser` / `BrowserContext`.
- **Refused Anti-Patterns:** Double-brace initialization (`CONCEPT-java-v1-030-01-double-brace-initialization-antipattern`), non-standard javac AST plugins like Manifold (`CONCEPT-java-v1-028-01-manifold-compiler-plugin-tradeoffs`), Lombok `@Data`/`@Value` where standard `record` applies (`CONCEPT-java-v1-027-01-selective-lombok-with-modern-java`).

### 5.3 Lane 3: TypeScript + `@playwright/test` (`ts_pwtest`)
- **Language & Tooling:** TypeScript 5.6+, `@playwright/test` 1.49+, `tsconfig.json` with `"strict": true`, `"noUncheckedIndexedAccess": true`, `"exactOptionalPropertyTypes": true`.
- **Key Realizations:** `readonly` interfaces and `as const` objects/tuples (`CONCEPT-ts-v1-001-01-readonly-interface-and-type`, `008-01-readonly-array-and-const-tuple`); discriminated unions with exhaustive `never` checks (`CONCEPT-ts-v1-002-01-discriminated-union`, `003-01-exhaustive-switch-never`); strongly-typed `test.extend<Fixtures>()` (`CONCEPT-ts-v1-006-01-typed-test-fixture-extension`, `CONCEPT-pwtest-v1-001-01-test-extend-fixtures`); web-first `await expect(locator)...` assertions.

### 5.4 Lane 4: JavaScript ESM + `@playwright/test` (`js_esm_pwtest`)
- **Language & Tooling:** Node 22+ ES Modules (`"type": "module"`), `@playwright/test` 1.49+, JSDoc `@ts-check` for zero-build static verification.
- **Key Realizations:** `Object.freeze({...})` and frozen arrays (`CONCEPT-js-v1-001-01-object-freeze-and-destructuring`, `008-01-frozen-array-and-map-set`); optional chaining `?.` and nullish coalescing `??` (`CONCEPT-js-v1-004-01-nullish-coalescing-and-optional-chaining`); `test.extend({...})` async fixture callbacks (`CONCEPT-js-v1-006-01-async-fixture-callback`).

---

## 6. Emitting `@concept` Provenance Annotations

When a project uses DSL-First together with **GAKS** (Governance & Architecture Knowledge System), every generated file stamps the GAKS `@concept` annotations of the Tier 0/1/2 concepts its emitter used:

```java
// GENERATED FROM DSL — DO NOT EDIT
// @concept CONCEPT-prog-v1-001-01-immutable-data-carrier
// @concept CONCEPT-java-v1-002-01-records
// @concept CONCEPT-java-v1-011-01-immutable-collection-factories
// @concept CONCEPT-java-v1-019-01-jspecify-and-nullaway
```

The GAKS code checker (`gaks_check.py --code` or `check.mjs --code`) scans the generated source tree and verifies that every `@concept` annotation resolves to an accepted `CONCEPT` document in the project's corpus or imported catalogs (`code-link-unresolved: 0`). Simultaneously, the GAKS static site (`site.mjs`) builds a clickable backlink from each concept page to the exact files and lines in the codebase that realize it.

---

## 7. Governance: How New Concepts Arrive (The 3-Channel Funnel)

Programming languages and test tools evolve continuously. New concepts arrive from three sources:
1. **Channel 1 — Human-Curated Source (Talks, Books, Post-Mortems):** An architect or developer watches a talk (e.g., *"Java++"*) or reads a post-mortem and wants to adopt a construct for better readability or safety.
2. **Channel 2 — AI Assistant Proposal:** During code generation or refactoring, the AI coding assistant notices that a newer construct (e.g., Java 24 `Gatherers.windowFixed` or Playwright `page.clock`) would simplify a generated helper.
3. **Channel 3 — Upstream Toolchain Release:** A project upgrades its compiler or test runner (e.g., Java 21 → 25, Playwright 1.40 → 1.50).

**Rule:** Neither the AI assistant nor a developer may silently change the emitter to output a new language construct ad hoc. Every candidate construct passes through a three-step funnel:
1. **Catalog the Concept (`CONCEPT-<cat>-v1-NNN-01-<slug>.md`):** Record what the construct is (`## Definition`), why it exists (`## Why it exists`), its minimum version (`since:`), and which archetype it realizes (`realizes:`).
2. **Open a Decision Gate (`[!decision]` with `state: open`):** Compare the candidate against the current Target Profile (`constructs_prefer`, `constructs_avoid`, `budget`, compiler requirements).
3. **Promote on Approval (`state: decided`):** Once approved by the human maintainer, update the `target` block (`realize` / `prefer` / `avoid`), update the deterministic emitter, and regenerate the codebase in one atomic commit.

