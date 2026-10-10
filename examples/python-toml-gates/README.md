# DSL-First in Python: a TOML model, and gates that can fail

A runnable data-hosted pipeline in Python, with **no dependency to install**. The model is TOML, read with `tomllib` from the standard library (Python 3.11 or later). An interpreter wires a small task application from the model. Ten **gates** hold the model and the hand-written code together, and a harness proves that each gate can fail.

This is the companion of the [Gates Guide](../../dsl_first_methodology/DSL_FIRST_GATES_GUIDE.md). Every rule of that guide is implemented here.

## Run it

```bash
cd examples/python-toml-gates

python3 tools/validate.py                  # the gates: exit 0 when every rule holds
python3 tools/verify_gates.py              # put 14 defects back; each gate must fire
python3 -m unittest discover -s tests -v   # drive the application through its bus
python3 tools/generate_docs.py             # derive docs/generated/ from the model
```

Expected:

```
ok    described                  ratchet  read 9   1 in the baseline
ok    docs-fresh                 hard     read 3
ok    event-declared             hard     read 3
ok    gates-declared             hard     read 10
ok    handler-resolves           hard     read 6
ok    not-built-count            report   read 7   count 1, ceiling 1
ok    not-built-stated           hard     read 7
ok    single-registration-site   hard     read 9
ok    vocabulary                 hard     read 25
ok    wiring-typed               hard     read 1

10 gates, 0 failure(s)
```

## How this relates to the other examples

| | `clojure-edn-contexts` | this example |
|---|---|---|
| Host language | Clojure | Python |
| Model carrier | EDN | TOML |
| Pointer → code | `requiring-resolve` | `importlib` + `getattr` |
| Wiring | status-gated interpreter | the same, in `src/taskapp/interpreter.py` |
| A declaration that is not built | stays unwired, silently | stays unwired **and answers with its reason** |
| Checks on the model | a schema | ten declared gates, each with a planted failure |
| Derived documents | — | reference, glossary, and the list of gates |

It shows that the data-hosted binding does not need a Lisp. It needs a data format with a standard reader, and a way to turn a name into a function at run time.

## What is in it

```
model/                         THE MODEL — the single source of truth
  contexts/tasks.toml            commands, queries, events, a subscriber
  contexts/audit.toml            a middleware and a query
  app.toml                       the composition, and the WIRINGS between operations
  language.toml                  the vocabulary: one thing, one name
  gates.toml                     every gate, declared, with its planted failures
  baselines/described.txt        the frozen debt of one ratchet. It only shrinks
src/taskapp/
  model.py                       read the model
  bus.py                         the one seam; refuses what the model does not declare
  interpreter.py                 wires the application. The ONE legal registration site
  tasks/ audit/                  hand-written bodies
tools/
  validate.py                    the gates
  verify_gates.py                the planted-failure harness
  generate_docs.py               the deriver for docs/generated/
docs/generated/                DERIVED — never edited by hand
tests/test_app.py              the application, the conformance test, and both tools
```

## The six things to look at

### 1. An edge of the model is declared, so it can be checked

`model/app.toml` says that the `task_id` of a list row is what `tasks.complete` receives:

```toml
[[wirings]]
name          = "complete-from-list"
from          = "tasks.list"
field         = "task_id"
to            = "tasks.complete"
payload_field = "task_id"
```

Change the type on one side only, in `model/contexts/tasks.toml`, and run the gates:

```
FAIL [wiring-typed] wiring complete-from-list: tasks.list.task_id is str, and
     tasks.complete.task_id is int. Make the two declarations agree.
```

Without the declaration, both operations are correct on their own and the defect is found by a user.

### 2. Every gate is declared

`model/gates.toml` holds one entry per gate: what it refuses, its kind, the population it must read, and one or more planted failures. The gate `gates-declared` compares that file with the functions in `tools/validate.py`, in both directions. `docs/generated/gates.md` is derived from it, so a person can read what the build refuses.

### 3. Three kinds of gate

- **hard** — one item is a failure. Eight of the ten.
- **ratchet** — `described`. One operation had no description on the day the gate arrived. It is frozen in `model/baselines/described.txt`, so the gate was never red on arrival. A new operation with no description fails. And if you write the missing description, the gate fails *until you delete the line*: the baseline is always exactly the remaining debt.
- **report** — `not-built-count`. The count of operations that are declared and not built has a ceiling of 1. A printed count with no ceiling is not a rule.

### 4. Every gate is proven able to fail

`tools/verify_gates.py` copies the project, puts one defect back, runs the gates on the copy, and requires a red build **with the expected message**. It does this for each of the 14 planted failures in `model/gates.toml`.

One of them removes the whole population of a gate:

```toml
[[gates.wiring-typed.planted]]
file    = "model/app.toml"
find    = "[[wirings]]"
replace = "[[removed]]"
expect  = "read 0, and it must read at least 1"
```

A gate that reads nothing cannot fail. The floor `reads_at_least` makes that state red.

The harness earned its place while this example was written. Its first run found that the document generator stopped with a traceback on a model with a missing key, so the gate that should have named the missing key never spoke.

### 5. "Not built" is a declaration

`tasks.archive` is declared with `status = "not-built"` and a reason. The interpreter does not wire it. The bus knows it:

```python
>>> bus.dispatch("tasks.archive", {"task_id": 1})
NotBuilt: 'tasks.archive' is not built yet: No decision yet on how long an archived task is kept.
>>> bus.dispatch("tasks.delete", {"task_id": 1})
Refused: 'tasks.delete' is not a declared operation
```

The generated reference lists it under "What is declared and not built".

### 6. The vocabulary is enforced

`model/language.toml` gives each term the words it refuses. Name an operation `finish` where the term is `complete`:

```
FAIL [vocabulary] wiring finish-from-list: the word 'finish' is refused.
     Write 'complete' (model/language.toml).
```

`docs/generated/glossary.md` is derived from the same file.

## Derivation and conformance, both

This example uses both modes of the [Methodology Guide](../../dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md) §3.5.

- **Derived**: the wiring of every handler, the payload checks of the bus, the list of events an operation may emit, and the three documents. None has a hand-written twin.
- **Conformed**: the handler bodies are written by hand. `handler-resolves` pins each pointer to a real function, and `tests/test_app.py` compares the declared operations with the ones the running bus holds, as an equality.

## Use it in your project

Copy `tools/validate.py`, `tools/verify_gates.py` and `model/gates.toml`, then replace the gate functions with your own rules. Keep the runner, the three kinds, the floor and the harness as they are. Call both tools from your pre-commit hook and from CI.

The gates read files, so they do not need to be in the language of the application. A Java or TypeScript project can use this runner unchanged and write only the gate functions.
