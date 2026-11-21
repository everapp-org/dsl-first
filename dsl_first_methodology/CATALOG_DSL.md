# Catalog DSL Specification (v0.1 — PROPOSAL)

> **Origin**: Part of the [DSL-First Methodology](https://github.com/everapp-org/dsl-first) open-source project.
> **License**: Apache 2.0 — see [LICENSE](../LICENSE)
> **Status**: Proposal — motivated by the [qa-tafBase case study](../in_depth_docs/case-study-qa-tafbase.md). Feedback welcome on the tracker.

**Version:** 0.1-draft
**Companions:** [Kernel DSL v1.1](KERNEL_DSL.md) · [Behavior DSL v1.0](BEHAVIOR_DSL.md) · [Verification DSL v1.0](VERIFICATION_DSL.md)
**Purpose:** This document proposes the **Catalog DSL** — a language for **declaration tables**: typed rows of named things that are *data, not lifecycles*. Element catalogs, configuration keys, device/target matrices, suite manifests, lookup maps. It fills the largest gap found when applying the stock DSL trio to a real test-automation framework: the framework's most mechanical mass was neither entities-with-states nor procedures — it was *tables of declarations*.

---

## 1. Why a separate language

The Kernel DSL models *things and their lifecycles*; the Behavior DSL models *procedure shape*; the Verification DSL models *claims*. A fourth modeling concern recurs in real systems and fits none of them:

- **Declaration tables** — sets of named entries, each a typed record: `element email { locator 1 en id "1-email" }`, `key chrome.headless : boolean`, `target w10_edge { os "Windows" ... }`, `suite QaTests { tags "..." glue "..." }`.

Kernel DSL's `fields { name: TYPE }` cannot express these: field names cannot carry dots (`visibility.sleep.time`), the `TYPE` token set cannot hold quoted xpath values, and a sparse `(version × language)` matrix is not a "fields" shape. The Behavior DSL models no data at all. A catalog is the table counterpart of `model` — entries have *shape, not states*.

Three constructs justify the grammar beyond plain rows:

| Construct | What it expresses that rows alone cannot |
|-----------|------------------------------------------|
| `bind` / `route` | **Selection rules** — "when tag X, pick env Y at version Z"; "token `w10_edge` resolves to capabilities C". The dispatch wiring that lives as scattered switches in legacy code. |
| `format` | **Name/path grammars** — `TCID_RunId_Resolution_StepId_Type.PNG`: a declared pattern that generates both a renderer *and* a parser, keeping the two provably inverse (round-trip checked by the Verification DSL). |
| `variant` | **Sparse inheritance** — a row overrides one cell of an earlier row (`locator 627 es xpath "..."` sits beside `locator 1 en xpath "..."`); the table is a matrix with holes, and the *resolution rule* (exact → greatest-version-≤ → fallback column) belongs to the catalog's semantics, like the Kernel DSL's transition semantics. |

---

## 2. Metamodel (M2): Core Concepts

### 2.1 Catalog

A named table kind. Each file holds one or more `catalog` blocks (or the shorthand top-level kind blocks — `page`, `target`, `suite`, `config`, `key` — which are `catalog` instances with a declared entry shape).

### 2.2 Entry

A named record inside a catalog: `entryName "optional label" { property-lines }`. Properties are `name value...` lines; values are strings, integers, booleans, or bare names. Entries nest (a `page` contains `element` entries, each containing `locator` rows) — nesting declares containment, not lifecycle.

### 2.3 Variant / row matrix

Rows sharing a partial key form a **matrix**: `locator <version> <language> <kind> <value>` keys on `(version, language)`. The catalog declares the matrix's **resolution rule** once (`resolve exact | below | fallback <column>`) instead of repeating it per entry.

### 2.4 Binding (`bind`)

A selection rule mapping a *discriminant* to a *result*:

```
bind environment on tag {
    "@qa_env"  -> env qa, version QA
    "@uat_env" -> env uat, version UAT
}
```

`bind` is the model's answer to the hand-written `switch`/tag-scanning glue: it declares *which input selects which row* and compiles to a lookup + dispatch — the same claim then checkable via the Verification DSL.

### 2.5 Format (`format`)

A name grammar producing a renderer **and** a parser:

```
format ScreenshotName {
    pattern "<tcid>_<runid>_<resolution>_<stepid>_<type>.PNG"
    pattern-baseline "<tcid>_<resolution>_<stepid>_Expected.PNG"   // runid absent
}
```

The two declarations generate `renderName(...)` and `parseName(file)`; the Verification DSL asserts the round trip — `parse(render(x)) == x` — as an ordinary check.

---

## 3. Formal Grammar (EBNF)

```ebnf
start            = catalog+ | entry_block+

catalog          = "catalog" NAME "{" catalog_body "}"
entry_block      = KIND NAME STRING? "{" entry_body "}"     // e.g. page, target, suite, config

catalog_body     = (description | entry_block | bind | format | resolve_rule)+
description      = "description" STRING

entry_body       = (property | entry_block)+
property         = NAME ":"? value+
value            = STRING | INTEGER | BOOLEAN | NAME

bind             = "bind" NAME "on" NAME "{" bind_row+ "}"
bind_row         = STRING "->" NAME ("," NAME NAME?)*

format           = "format" NAME "{" format_pattern+ "}"
format_pattern   = "pattern" NAME? STRING

resolve_rule     = "resolve" NAME ("," NAME)*   // e.g. resolve exact, below, fallback en

KIND             = NAME                         // entry kinds are open — declared per project
NAME             = /[A-Za-z_][A-Za-z0-9_.]*/    // dotted names allowed (config keys)
STRING           = ESCAPED_STRING
INTEGER          = /[0-9]+/
BOOLEAN          = "true" | "false"

%import common.ESCAPED_STRING
%import common.WS
%ignore WS
COMMENT          : "//" /[^\n]*/
%ignore COMMENT
```

*Notes: `KIND` is deliberately open — `page`, `element`, `locator`, `target`, `suite`, `key`, `env`, `source` are project vocabulary, like the Behavior DSL's runtime-API names. The grammar fixes only the *shape* of a declaration table.*

---

## 4. Semantics: Transformations (M2T)

| Source | Derived artifact | Example |
|--------|------------------|---------|
| entry block | typed record class + field population | `LoginPomGen`, `TafConfig` accessors |
| row matrix | lookup table + resolver | `Table<version,language,locator>` + `resolve(version,lang)` |
| `bind` | dispatch function | `pickEnvironment(tags)` |
| `format` | renderer + parser pair (inverse-checked) | `renderName` / `parseName` |
| catalog | JSON Schema of the table | `elements.schema.json` |

**Contract with the siblings:** `bind` results feed Behavior-DSL `given`/`when` inputs; `format` pairs are validated by Verification-DSL round-trip checks; entry records are referenced as Kernel-DSL field types. The catalog is the *data* leg of the family triangle.

## 5. Well-Formedness

- Every `bind` discriminant value referenced must be covered by a row (validators warn on partial coverage; an `else` row is allowed).
- Matrix rows must share the declared key arity `(version, language)`; duplicate full keys are an error.
- `format` patterns must name the same placeholders on both sides of any declared pair (baseline vs. typed).
- `resolve` names must include `exact`; a `fallback` column must name a declared matrix column.
- Entry kinds and property names are open; a validator may warn on names outside a project's declared kind set but must not reject them.

## 6. Worked Example

```dsl
// The QA framework's element catalog — one entry kind, a row matrix,
// and the resolution rule declared once.

catalog Elements {

    page LoginPom "Login Pom" {
        package "pom.portal"

        element email "Username field on the Login and Register pages" {
            locator 1 en id "1-email"
        }
        element loginLink "Log in link on the Register screen" {
            locator 1   en xpath "//li/*[contains(text(), 'Log in')]"
            locator 515 es xpath "//li/*[contains(text(), 'Ingresar')]"
        }
    }
    resolve exact, below, fallback en
}
```

Derived: `LoginPom.java` (fields + `VersionedLocator` rows + `addLocator` merges) and the `(version, language)` resolver — 1,902 locator rows in the motivating project, verified row-for-row identical to the hand-written declarations.

## 7. When to use Catalog vs. Kernel vs. plain code

- **Catalog**: rows of the same shape, selection is a lookup/binding, lifecycle absent → declaration tables.
- **Kernel `model`**: the thing has *states and allowed changes* → lifecycle.
- **Plain code**: the rule is genuinely algorithmic (image diff, XPath engine) → runtime.

The qa-tafBase case study shows the split in practice: the catalog absorbed the framework's *declarations* (its biggest generatable slice), the Kernel DSL its *lifecycles*, the Behavior DSL its *procedures* — and the runtime kept the algorithms.

## 8. Versioning

- v0.1-draft: catalog blocks, nested entries, row matrices + `resolve`, `bind`, `format`.
- Planned v1.0 additions: `schema` property constraints (required/optional/default), per-entry `kind` validation sets, multi-file `include`.
