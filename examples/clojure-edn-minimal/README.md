# Minimal DSL-First in Clojure (data-hosted)

A complete, runnable data-hosted pipeline in ~40 lines of Clojure. It models the **same Agent state machine** as [`../java-antlr-minimal`](../java-antlr-minimal) — so you can compare the two bindings side by side. There, a grammar and a code generator turn text into a `.java` file. Here, the DSL **is data**: you read it, validate it with a schema, and derive behaviour by interpreting it. No grammar, no parser, no generated files.

This README is for an implementer building the pipeline by hand.

## Files

| File | Role |
|------|------|
| `resources/agent.edn` | **The DSL** — the Agent model, written as plain EDN data |
| `src/agent/schema.clj` | **The "grammar"** — a malli schema that validates the model |
| `src/agent/runtime.clj` | **The deriver** — interprets the model into a state-machine function |
| `src/agent/load.clj` | **The pipeline** — `read → validate → derive` |
| `deps.edn` | Project + the one dependency (malli) |

## The three steps

**1. The model is data** (`resources/agent.edn`):

```clojure
{:domain :agent
 :states #{:off :idle :working}
 :transitions [[:activate :off :idle] [:assign-task :idle :working] ...]}
```

Nothing to parse — `clojure.edn/read-string` turns the file into a map.

**2. Validate with a schema** (`src/agent/schema.clj`). malli schemas are themselves data, so this is the data-hosted stand-in for a `.g4` grammar. `m/explain` + `malli.error/humanize` give path-precise errors the moment you read the file.

**3. Derive by interpreting** (`src/agent/runtime.clj`). No files are generated. `transition-fn` walks the `:transitions` and returns a `(state, action) -> next-state` function. The same data could just as easily drive docs, a DB schema, or tests — each is another walk over the map.

`src/agent/load.clj` ties the three together into `load!`.

## Run it

```bash
clj
```
```clojure
user=> (require '[agent.load :as l])
user=> (def sys (l/load! "agent.edn"))
user=> ((:step sys) :off  :activate)      ;; => :idle
user=> ((:step sys) :idle :assign-task)   ;; => :working
user=> ((:step sys) :idle :explode)       ;; => :idle   (no such transition)
```

Edit `agent.edn`, re-run `load!`, and the behaviour changes immediately — no build step. (There's a copy-paste REPL session in the `(comment …)` block at the bottom of `load.clj`.)

## Want emitted code instead of interpretation?

A macro can expand the same model into a typed `defrecord` at compile time — the data-hosted equivalent of code generation, with no template language. See §5.3.4 of the [Methodology Guide](../../dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md). For the conceptual picture of both bindings, see §2.2 and §2.5.
