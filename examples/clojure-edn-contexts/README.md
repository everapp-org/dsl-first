# Context-Level DSL-First in Clojure (data-hosted, advanced)

A runnable data-hosted pipeline that scales the minimal example from **one model → one function** to **multiple bounded contexts → a composed application**. It demonstrates four techniques that the minimal example doesn't need but production systems do.

## How this relates to `clojure-edn-minimal`

| | Minimal | This example |
|---|---|---|
| Models | One state machine | Multiple bounded contexts |
| Manifest levels | — | Commands, queries, subscribers, middleware |
| Dispatch | Direct function call | `requiring-resolve` from `:handled-by` keyword |
| Wiring | Hand-coded | Status-gated — manifest declares, interpreter wires |
| Application shape | Single file | Composition file leases contexts |
| Bus | — | In-process IBus protocol |

Both follow the same three-step pipeline: **read → validate → interpret**. No grammar, no parser, no generated files.

## The four techniques

### 1. Multi-level interpretation

One context manifest declares four kinds of things — commands, queries, subscribers, and middleware — in a single EDN file. The interpreter walks all four levels and wires each to the bus. See `resources/contexts/tasks.edn`:

```clojure
{:domain :tasks
 :levels
 {:commands   {:models {:create  {:handled-by :tasks.handlers/create ...}
                        :complete {:handled-by :tasks.handlers/complete ...}}}
  :queries    {:models {:list    {:handled-by :tasks.queries/list ...}}}
  :subscribers {:models {:log-events {:handled-by :tasks.subscribers/log-event ...}}}}}
```

The interpreter (`src/contexts/embed.clj`) extracts each level and calls the appropriate bus method. No level-specific boot code — the manifest *is* the wiring.

### 2. `requiring-resolve` dispatch

`:handled-by` is a fully-qualified keyword like `:tasks.handlers/create`. The interpreter resolves it to a Clojure var at install time using `requiring-resolve`. No hand-registration; no code generation. The keyword in the EDN *is* the dispatch table.

```clojure
(defn try-resolve [handler-kw]
  (requiring-resolve (symbol (namespace handler-kw) (name handler-kw))))
```

This means you can add a new command by editing the EDN and writing the handler fn — no boot code to touch.

### 3. Status-gated wiring

Only declarations whose `:status` is active (`:in-progress`, `:in-test`, `:ready`) are wired. A declaration marked `:planned` stays silently unwired — the handler may not exist yet. This lets the manifest describe the **full intended surface** while the system boots with only what is implemented. Flip `:planned` to `:in-progress` when you start writing the handler; the interpreter picks it up on the next boot.

### 4. Composition / leasing

A separate EDN file (`resources/instances/task-app.edn`) declares which contexts to lease:

```clojure
{:application :task-app
 :composes {:leases [:tasks :audit]}}
```

The composition interpreter (`src/compose.clj`) reads this value and installs only the leased contexts. The same interpreter boots any application — it names no application. Adding a context to an application is editing one line of EDN.

## Files

| File | Role |
|------|------|
| `resources/contexts/tasks.edn` | **Context manifest** — commands, queries, subscribers for task management |
| `resources/contexts/audit.edn` | **Context manifest** — cross-cutting middleware |
| `resources/instances/task-app.edn` | **Composition** — which contexts this app leases |
| `src/contexts/schema.clj` | **The "grammar"** — malli schemas for manifests and compositions |
| `src/contexts/embed.clj` | **Context interpreter** — walks a manifest, wires handlers to the bus |
| `src/compose.clj` | **Composition interpreter** — reads a composition, leases contexts |
| `src/contexts/load.clj` | **The pipeline** — boot! ties it together |
| `src/bus/protocol.clj` | **IBus protocol** — register-handler!, dispatch!, query, emit! |
| `src/bus/memory.clj` | **In-process bus** — atom-backed IBus implementation |
| `src/tasks/handlers.clj` | **Handler fns** — what `:handled-by` keywords resolve to |
| `src/tasks/queries.clj` | **Query fns** |
| `src/tasks/subscribers.clj` | **Subscriber fns** |
| `src/audit/middleware.clj` | **Middleware fns** |
| `deps.edn` | Project + dependencies (clojure, malli) |

## Run it

```bash
clj
```
```clojure
user=> (require '[contexts.load :as l])
user=> (def sys (l/boot! "instances/task-app.edn"))
user=> (:registered sys)
;; => [[:commands :tasks/create] [:commands :tasks/complete]
;;     [:queries :tasks/list] [:subscriber :task/created]
;;     [:middleware :audit.middleware/record-command]]

user=> (require '[bus.protocol :as bus])
user=> (bus/dispatch! (:bus sys) {:type :tasks/create :title "Write docs"})
;; [audit] command: :tasks/create
;; [subscriber] event: :task/created
;; => {:id <nanoTime> :title "Write docs" :status :open}

user=> (bus/query (:bus sys) {:type :tasks/list})
;; => [{:id 1 :title "Example task" :status :open}]
```

Edit `tasks.edn`, re-run `boot!`, and the wiring changes immediately — no build step.

## Where this comes from

These techniques were extracted from a production Clojure codebase (IKC) that uses DSL-First with 18 bounded contexts, a command bus, and composition-driven application boot. The case study (`in_depth_docs/case-study-context-interpreters.md`) walks through the extraction: what was generic, what was project-specific, and why the boundary matters.

## Methodology cross-references

- **§2.2** (pipelines) — the data-hosted pipeline: read → schema → interpret → registries
- **§2.5** (two bindings) — this is the data-hosted binding; no grammar, no parser
- **§5.3.4** (interpretation and macros) — the interpreter is "ordinary functions walk the validated data and return runtime values"
- **§5.4** (generator / interpreter testing) — test the interpreter, not what it interprets
