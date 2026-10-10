# DSL-First Development — Gates Guide

> **A model governs code only as far as a program checks it. This guide is about those programs: what a gate is, how to declare one, how to bring one into a project that is not clean yet, and how to know that a gate can fail.**

**Version**: 1.0
**Who reads this document**: the **AI coding assistant**. It follows this guide when it adds a rule to a project, and when it adds a model that existing rules must cover. You can read it too.
**Depends on**: the [Methodology Guide](DSL_FIRST_METHODOLOGY_GUIDE.md) §3.5, "Two Modes of Governance: Derivation and Conformance". That section says *why* conformance exists. This guide says *how to run it for years*.
**Runnable companion**: [`examples/python-toml-gates`](../examples/python-toml-gates/README.md) implements every rule below in about 900 lines of Python with no dependency. Every claim of the form "the build fails when…" can be watched there.

> **Origin**: Part of the [DSL-First Methodology](https://github.com/everapp-org/dsl-first) open-source project.
> **License**: Apache 2.0 — see [LICENSE](../LICENSE)

---

## Table of Contents

1. [Why this guide exists](#1-why-this-guide-exists)
2. [A rule is a program, not a paragraph](#2-a-rule-is-a-program-not-a-paragraph)
3. [Declare the edges, not only the nodes](#3-declare-the-edges-not-only-the-nodes)
4. [A gate is a declaration](#4-a-gate-is-a-declaration)
5. [Three kinds of gate](#5-three-kinds-of-gate)
6. [A gate must be able to fail](#6-a-gate-must-be-able-to-fail)
7. [Declare what is not built](#7-declare-what-is-not-built)
8. [Statements, not prose](#8-statements-not-prose)
9. [The vocabulary is a model](#9-the-vocabulary-is-a-model)
10. [A model in the words of the domain](#10-a-model-in-the-words-of-the-domain)
11. [The rules about models are a model](#11-the-rules-about-models-are-a-model)
12. [What a static gate cannot see](#12-what-a-static-gate-cannot-see)
13. [Bindings: Python, Java and others](#13-bindings-python-java-and-others)
14. [Checklist](#14-checklist)
15. [Where these rules come from](#15-where-these-rules-come-from)

---

## 1. Why this guide exists

The Methodology Guide makes one promise: **the model governs the code**. It names two mechanisms. *Derivation* makes disagreement impossible, because the code is the output of the model. *Conformance* makes disagreement loud, because a test compares the two.

A project that follows the guide for a few months collects many such checks. Then a second-order problem appears, and nothing in the first-order rules prevents it:

- a check that passes because it **reads an empty list**;
- a count that the build **prints and does not limit**, so it grows;
- a rule that lives in a **checklist** that nobody runs;
- a check that guards one side of a join, while the defect is **in the join**;
- a model that names an operation that **nothing declares**, which the check skips because there is nothing to compare.

Every item in that list was found in a codebase where all gates were green. This guide is the set of rules that would have made each one red.

The word used here is **gate**: a program that reads the model, or the code, or both, and stops the build when a rule is broken. A conformance test (Methodology Guide §3.5) is one kind of gate. A schema validation is another. A freshness check on a generated file is a third.

---

## 2. A rule is a program, not a paragraph

**When a rule is worth writing, write it where it executes.** Prose that describes a rule documents an intention. Only a check is the rule.

Two forms of the same failure:

- **The rule that depends on memory.** A pre-commit hook was installed by copying a file. The versioned copy gained two checks. The installed copy did not, for 25 days. Nothing compared the two. A guard that nothing guards is not a guard.
- **The rule written as a checklist.** A quality rule was written as a Markdown checklist, with automation "deferred until the checklist proves itself". It was never run once. When it was finally compared with real defects, it would have passed three of four: it asked whether an empty list *has a label*, and the broken screen had one.

Consequences for the assistant:

1. A new convention arrives with its gate **in the same change**. "We will add the check later" is the checklist failure.
2. A rule that cannot be checked by a program is written down **as advice**, and named as advice. It is not listed among the rules.
3. The mechanism that runs the gates is itself checked: the installed hook is compared with the versioned one, or the hook is a one-line call into the versioned script.

---

## 3. Declare the edges, not only the nodes

A model usually lists **nodes**: commands, queries, events, entities, screens. It is easy to stop there. The defects then live in what joins the nodes.

A real case. A query returned a list of rows. Each row carried an `id`, as a **string**. A command took an `id`, as a **UUID**. A screen passed the first to the second. Both sides were written down. Only the command's side was written in a form a program could read; the query described its result in a comment. So nothing could compare them. Editing was unusable from the user interface while every test was green, because the tests exercised the handler and never the join.

> **A seam that is not declared cannot be checked, and will be found by a user.** The count of undeclared seams is the count of defects still waiting.

The rule has two parts.

**Every provider declares what it returns.** A query, a data source, an event: each one states its result shape in the model, the way a command states its payload.

**Every wiring is a model element.** Where one operation's output becomes another operation's input, the model says so:

```toml
[[wirings]]
name          = "complete-from-list"
from          = "tasks.list"        # the provider
field         = "task_id"           # what it gives
to            = "tasks.complete"    # the consumer
payload_field = "task_id"           # where it goes
```

A gate then checks three things: both ends are declared, the provider declares a result, and the two types agree. Change the type on one side only, and the build fails with both types in the message.

When you bring this rule into an existing project, expect a large first number. Introduce it as a **ratchet** (§5), one context at a time.

---

## 4. A gate is a declaration

Gates accumulate inside a validator script. After a year the script is thousands of lines, and three questions have no cheap answer: *What does the build refuse? Which of these counts is a limit, and which is only printed? Which checks have ever failed?*

So the gates get the treatment every other concern gets: **they are declared as data, and the declaration is checked against the implementation.**

One entry per gate:

| key | holds |
|---|---|
| `refuses` | one sentence: what the gate refuses |
| `kind` | `hard`, `ratchet` or `report` (§5) |
| `baseline` | ratchet only: the file that holds the frozen debt |
| `ceiling` | report only: the number the count may not exceed |
| `reads_at_least` | the population the gate must read (§6) |
| `planted` | one or more defects to put back, each with the failure text to expect (§6) |
| `governed_by` | optional: the decision or requirement that justifies the gate |

```toml
[gates.wiring-typed]
refuses        = "A wiring whose two ends are not both declared, or do not agree on the type."
kind           = "hard"
reads_at_least = 1

[[gates.wiring-typed.planted]]
file    = "model/contexts/tasks.toml"
find    = 'result_item = { task_id = "int", title = "str", done = "bool" }'
replace = 'result_item = { task_id = "str", title = "str", done = "bool" }'
expect  = "tasks.list.task_id is str, and tasks.complete.task_id is int"
```

Three things follow from the declaration.

- **A meta-gate compares the two lists in both directions.** A gate that is implemented and not declared fails the build. A gate that is declared and not implemented fails the build. This is the bidirectional conformance check of the Methodology Guide §3.5, applied to the gates themselves.
- **The gates become a page.** A generator projects the declaration to a document: each gate, its kind, what it refuses, its limit. A person can read what the build refuses with no read of the validator.
- **A grammar argument is settled in a diff.** Raising a ceiling, or freezing new debt into a baseline, is a visible change to a small file that a reviewer reads. It is not an edit inside a script.

---

## 5. Three kinds of gate

| kind | the build fails when | use it for |
|---|---|---|
| **hard** | the gate finds one item | a rule the project already meets |
| **ratchet** | the gate finds an item that is **not in the baseline**, *or* the baseline holds an item that **no longer fails** | a rule the project does not meet yet |
| **report** | the count is **above the ceiling** | a number you watch, such as "declared and not built" |

### Never red on arrival

A new rule in an old project finds hundreds of violations. If the gate is red on its first day, people disable it. A gate that is red on arrival gets disabled instead of obeyed.

So a new rule arrives as a **ratchet**: today's violations are written to a baseline file, and the gate is green. From that moment a *new* violation is red.

### A ratchet has two sides

A one-sided ratchet only refuses new debt. Its baseline then rots: entries stay after the debt is paid, and the file stops meaning anything. Later someone "refreshes" it, and freezes new debt by accident.

A two-sided ratchet also fails when a baseline entry **no longer fails**. The message says: delete the line. So the baseline is always exactly the remaining debt, and it only shrinks.

```
# model/baselines/described.txt
# THIS FILE ONLY SHRINKS. Write the description, then delete the line.
audit.record
```

One trap: when the gate's input population disappears (a renamed directory, a changed file pattern), every baseline entry looks paid. A two-sided ratchet then tells a person to delete correct lines. The population floor of §6 stops that first.

### A report has a ceiling

"23 items awaiting a declaration" printed in a build log is not a rule. It is a number that can become 40 with no failure. Either the count has a ceiling that the build enforces, or the line is removed. **A ceiling only goes down:** when the count drops, lower the ceiling in the same change. A ceiling of 72 over a count of 66 permits six new violations.

### Baseline the items, not the gate

Before freezing a long list as "debt", classify a sample: *is the item wrong, or is the gate's question wrong?* In one measured case 219 of 226 baselined items were correct, and the gate was asking the wrong question. A baseline can hide a wrong gate as easily as it hides real debt.

---

## 6. A gate must be able to fail

A green gate has two explanations: the rule holds, or the gate looks at nothing. From the outside they are the same. The Methodology Guide §3.5 gives the first defence, *independent provenance*: the two sides of a comparison come from different origins. This section adds two more.

### 6.1 A planted failure for every gate

**A check that nobody has watched fail is not yet a rule.** Each gate ships with at least one *planted failure*: a named defect, the file to put it in, and the text the failure message must hold. A harness copies the project, puts the defect back, runs the gates on the copy, and requires a red build **with that message**.

```
$ python3 tools/verify_gates.py
fires  wiring-typed       tasks.list.task_id is str, and tasks.complete.task_id is int
fires  wiring-typed       read 0, and it must read at least 1
fires  described          'audit.record' is in model/baselines/described.txt and no longer fails
…
14 planted failures, 14 fire, 0 do not
```

Rules for a good planted failure:

- **It reintroduces the exact defect the gate exists for**, not a syntax error. A typo that breaks the parser proves the parser.
- **The expected text names the fix**, or at least the two things that disagree. A gate whose message is "validation failed" has to be debugged before it can be obeyed.
- **A ratchet gets two**: one for new debt, one for a stale baseline entry.
- **The harness requires the clean project to be green first.** Otherwise a red result proves nothing about the plant.

The harness finds defects on its first run. In the companion example it found that the documentation generator crashed on a model with a missing key, so the gate that should have named the missing key never spoke. In the codebase these rules come from, it found the eleventh of eleven checks wrong.

### 6.2 A population floor

A gate declares how many things it must read: `reads_at_least`. The runner fails the gate when it reads fewer.

This catches the most common silent failure: the gate iterates over a list that became empty. A directory moved. A file pattern stopped matching. A list that the gate reads was refactored away. In a real validator, three sections resolved references over a list that had been empty for three months. They printed "0 models declared" on every build and could never fail.

Set the floor near the real count, not at 1. A floor of 1 passes when a refactor loses 90% of the population.

### 6.3 The gate reads the thing, not a product of the thing

A gate must not read what it checks from a file that the same build generated from the model: both sides then have one origin, and the comparison is a tautology. A gate must not be scoped by a map that answers a different question ("the files we lint" is not "the files that hold handlers"). And a gate that scans source text must state **which extensions and which call names** it covers, because a rule that scans one language of a two-language project is half a rule that looks like a whole one.

---

## 7. Declare what is not built

Intent and absence usually live in three places that no query reaches: a comment, a disabled flag, or nothing at all. The result is a screen with five buttons, three of which do nothing, over a green build. The gate is green because an operation that is not declared has no payload to compare.

**"Not built" is a status in the model.**

```toml
[commands.archive]
description = "Move a done task out of the list."
status      = "not-built"
reason      = "No decision yet on how long an archived task is kept."
payload     = { task_id = "int" }
```

Four things then become true:

1. **The model can name the whole intended surface.** A view may refer to `tasks.archive`, and the reference resolves.
2. **The gate goes to zero and stays there.** "Names an operation that nothing declares" is a hard gate, because every name is declared — as built, or as not built.
3. **The running system answers honestly.** A call gets a refusal that carries the reason. A user interface draws the control inert, with the reason.
4. **The absence is a fact that a query can list.** "What does the product promise that it does not do?" has an answer, and the generated reference prints it.

Put a `report` gate with a ceiling on the count. "Not built" must not become a place where promises go to be forgotten.

This extends the *status-gated wiring* of the `clojure-edn-contexts` example. There, a `:planned` declaration stays unwired silently. Here it is unwired **and answers by name**.

---

## 8. Statements, not prose

Most models carry a `description` on each element, and a gate checks that it **exists**. No gate can check that it is **true**. A description once claimed that a pane "shows one thing at a time", in a product whose specification showed two things side by side. Nothing could catch it, because a sentence has no side that resolves.

A statement can be checked:

```
[reading-area holds one-or-two reading-pane]
```

That is three names and a relation from a closed list. Each name either resolves to a declared element or fails the build.

**A prose description is an unchecked claim wherever it appears.** Keep descriptions; a reader needs them. But when a description carries weight — a count, a containment, an ordering, a "never" — move the claim into a statement, a typed field, or a Verification DSL claim ([VERIFICATION_DSL.md](VERIFICATION_DSL.md)), and let the prose only explain.

The same rule applies to prose that must stay prose: make it **data, not a comment**. An open question written in a `notes` field can be listed, rendered and answered. The same question in a comment is invisible to everyone who does not have the file open.

---

## 9. The vocabulary is a model

The Methodology Guide's intake gate (§3.1.1) produces "one agreed name per concept". Without a gate, that agreement lasts until the second contributor. Two names for one thing then cost real time: two people move different objects that they both call "the right column", and each believes the other agreed.

**The vocabulary is a declaration, and the build reads it.**

```toml
[terms.task]
kind       = "noun"
definition = "One piece of work that a person must do."
refuses    = ["todo", "ticket", "item"]
```

- **One thing, one name.** Each term lists the synonyms it refuses.
- **The gate checks identifiers, not prose.** It refuses a refused word used as a name in any model: an operation, a field, an event. The failure names the approved word. It does not lint sentences, because a gate on years of existing prose is red on arrival (§5).
- **The glossary is derived.** It is generated from the declaration and never edited by hand, so it cannot disagree with what the build enforces.
- **A missing word is added, not used quietly.** Needing a new term is a one-line change to the vocabulary, reviewed like any model change.

### A vocabulary is measured, not designed

When you design a small language — the relations of §8, the statuses of §7, the kinds of §5 — do not design the word list first. **Write the corpus first, count, then close the vocabulary.**

A measured case: five model files written freely produced 132 statements using 29 relations. Counted, four relations carried 67% of the statements, and thirteen were used exactly once. The long tail was not richness. It was reaching for English where the model already had a word. Two rules removed it, and 13 relations then covered 130 statements. Read as prose, 29 words had looked like expressiveness.

---

## 10. A model in the words of the domain

Every model in the kernel is written in technical terms: commands, queries, events, schemas. All of them are true, and none is readable by the person the product is for. A product owner once opened the model that described his own screen and could not read a line of it.

A model level may be added **above** the technical one, written in the product's own words — "reading area", "sidebar", "holds", "shows" — and held to exactly the same discipline: a closed vocabulary, declared names, mechanical checks. Technical names then live in one section at the foot of the file, for the reader who needs them.

This is the *domain / level* structure of the [Kernel DSL](KERNEL_DSL.md) used for its original purpose. The test of the upper level is simple: **the person who asked for the product can read the file, and can say that one line of it is wrong.**

When an assistant writes facts into such a model that the person has not confirmed, they go under a separate key (`proposed_facts`), checked by the same grammar. Confirming one is moving a line, and the diff shows what was agreed.

---

## 11. The rules about models are a model

The grammar of a model family — which keys a file may hold, in which order, how many names a statement takes, which relations exist — is usually written inside the validator. A rule written inline in one of N scripts is a rule that N−1 scripts do not have.

Declare the grammar as data, in a file the validator reads. A gate declaration (§4), a vocabulary (§9) and a grammar file are the same move at three levels: **the thing that governs is held to the rule it enforces.** In MDE terms this is making part of M2 explicit and versioned, instead of leaving it implicit in code.

Do not over-apply this. The Methodology Guide §3.5 already says where the regress stops: at a small, readable artifact that a reviewer checks by reading. One meta-gate that compares "gates declared" with "gates implemented" is that artifact. A gate for the meta-gate is not needed.

---

## 12. What a static gate cannot see

Gates over declarations are static. Some contracts are observable only while the system runs: whether a clickable control has a non-zero hit area, whether a pane that says "empty" sits beside data, whether an event arrives in order.

**When you add a gate, ask what it cannot see**, and write the answer in its declaration or beside it. Then put the runtime contract where it can be checked: a browser sweep, an end-to-end run, a Verification DSL document executed against the live system. That is the same principle applied where a model cannot reach. It is not a reason to leave the contract to a human eye.

The runtime can also refuse directly. In the companion example the bus refuses an operation that the model does not declare, a payload of the wrong shape, and an event that the operation did not declare — each with the name in the message. A static gate and a runtime refusal derived from the same model cover each other.

---

## 13. Bindings: Python, Java and others

Nothing in this guide needs a particular language. Each technique needs one capability from the host.

| technique | needs | Python | Java |
|---|---|---|---|
| model as data | a data format with a standard reader | TOML (`tomllib`, standard library from 3.11), JSON, YAML | YAML or JSON with Jackson; or records and annotations as an internal DSL |
| a pointer resolves | name → callable at run time | `importlib.import_module` + `getattr` | `Class.forName` + `getMethod`; or `ServiceLoader` |
| one registration site | a scan of source for a call | `ast.walk` over `ast.Call` nodes | an ArchUnit rule; or an annotation processor |
| wiring types agree | both ends declared in the model | plain comparison of the declared types | the same; or generate typed adapters and let `javac` refuse |
| derived files are fresh | deterministic generation | regenerate in memory, compare | regenerate in `generate-sources`, then `git diff --exit-code` |
| planted failures | copy a tree, edit a file, run a process | `shutil.copytree` + `subprocess` | the same script; or JUnit with a `@TempDir` copy |
| runs on every change | a build hook | pre-commit + CI: `python3 tools/validate.py` | `mvn verify` (exec plugin) or a Gradle `check` task |

Two notes.

**Data-hosted is not only for Lisps.** The Methodology Guide §2.5 presents the data-hosted binding with Clojure. Any language that can read a data file and resolve a name to a callable can host one. Python does it with the standard library, as the companion example shows. What a Lisp adds is that the model and the code share one syntax. That is a convenience, not a requirement.

**The gates can be in a different language from the application.** A gate reads files. The declared model, the baseline files and the planted failures are language-neutral data. A Java application can keep its gates in a Python script. The gate then cannot share a bug with the code it checks.

A runnable Java example of these gates is not in this repository yet. The Python example is the reference. The existing [`java-antlr-minimal`](../examples/java-antlr-minimal/README.md) example shows the *derivation* side for Java, and its `verify-generated-preview.sh` is a `docs-fresh` gate in shell.

---

## 14. Checklist

**When you add a rule**

- [ ] The rule is a program, and it arrives in the same change as the convention
- [ ] It is declared: `refuses`, `kind`, `reads_at_least`, `planted`
- [ ] Its failure message names the element and the fix
- [ ] The project does not meet it yet → it is a **ratchet**, with today's items in a baseline
- [ ] A sample of the baseline was classified: item wrong, or gate wrong?
- [ ] The planted failure was run, and the gate was seen red with the expected text
- [ ] You wrote down what the gate cannot see, and where that is checked instead

**When you add a model element**

- [ ] A provider declares what it returns
- [ ] A value that flows from one operation to another is a declared wiring
- [ ] Every name is in the vocabulary, or the vocabulary gains the word in this change
- [ ] Something the model promises and the code does not do is declared `not-built`, with a reason
- [ ] A claim that carries weight is a statement or a typed field, not only a description

**On every change**

- [ ] The gates run, and so does the planted-failure harness
- [ ] A count that dropped took its ceiling down with it
- [ ] A baseline line that no longer fails is deleted
- [ ] No gate was skipped wholesale. A red gate is measured on the main branch first; only that one gate is skipped, and the skip is written down as work

---

## 15. Where these rules come from

These rules were extracted from one production codebase that applies DSL-First in the data-hosted binding: about thirty context manifests, a validator of several thousand lines, and a corpus of architecture documents linked to the models. Each rule above answers a defect that was found there while every gate was green. The numbers in the examples (25 days, 132 statements and 29 relations, 219 of 226, an empty list for three months) are measurements from that codebase, not illustrations.

What is proven there, and what is newer:

| rule | state in the source codebase |
|---|---|
| a provider declares its result; wirings are type-checked (§3) | in use, introduced as a ratchet |
| two-sided ratchets with a baseline file (§5) | in use |
| a planted failure per check (§6.1) | in use for one model family; the rule for all gates is newer |
| "not built" as a declared fact (§7) | in use in the domain-level models |
| statements over prose; a measured vocabulary (§8, §9) | in use |
| the vocabulary gate and the derived glossary (§9) | in use |
| a domain-level model above the technical one (§10) | in use |
| grammar as data (§11) | in use for one model family |
| **every gate declared in one file, with a population floor and a ceiling on every report (§4, §5, §6.2)** | **designed there and not built yet. The companion example in this repository is its first implementation** |

That last row is stated plainly because this project's rule is honest claims: the single gate declaration is the youngest idea here. It is included because the defects it answers are measured, and because the example shows that it costs very little.

---

*See also:* [Methodology Guide](DSL_FIRST_METHODOLOGY_GUIDE.md) §3.5 (derivation and conformance), §10 (anti-patterns) · [QA & Test Automation Guide](DSL_FIRST_QA_GUIDE.md) · [Verification DSL](VERIFICATION_DSL.md)
