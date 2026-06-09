# DSL-First in Clojure

DSL-First means: write your domain **once**, as the single source of truth, and derive everything else from it — the running behaviour, validation, a database schema, docs, tests. In Clojure this is unusually natural, because Clojure already blurs the line between *code* and *data*. Your "DSL" needs no new language and no parser — it's just **EDN**, the data notation you already read and write every day.

This guide builds a tiny domain end to end, at the REPL. You need only Clojure and one schema library.

## 1. The model is data

Here is a domain — a note that moves from draft to published — written as EDN in `notes.edn`:

```clojure
{:domain :notes
 :levels
 {:domain
  {:models
   {:note
    {:description "A user note."
     :fields  {:id :NoteId, :title :String, :state :NoteState}
     :states  #{:draft :published}
     :transitions
     ;; [from to :on event :emits [events…]]
     [[:draft :published :on :publish :emits [:note-published]]]}}}}}
```

That is the whole DSL. It's a plain Clojure map — keywords, nested maps, a set of states, a vector of transitions. Nothing to parse; you read it with `clojure.edn`:

```clojure
(require '[clojure.edn :as edn] '[clojure.java.io :as io])

(def model (edn/read-string (slurp (io/resource "notes.edn"))))

(get-in model [:levels :domain :models :note :states])
;; => #{:draft :published}
```

Because it's data, the whole Clojure toolkit applies — `get-in`, `update`, `assoc`, `clojure.walk`, destructuring. The model is the source of truth, and it is *queryable*.

## 2. Validate it with malli

A data DSL needs a guardrail so a malformed model fails loudly and early. [malli](https://github.com/metosin/malli) fits perfectly because its **schemas are themselves data** — written in the same EDN-shaped style as the model:

```clojure
(require '[malli.core :as m] '[malli.error :as me])

(def Model
  [:map
   [:description {:optional true} :string]
   [:fields  [:map-of :keyword :keyword]]
   [:states  {:optional true} [:set :keyword]]
   [:transitions {:optional true}
    [:vector [:cat :keyword :keyword [:= :on] :keyword
              [:? [:cat [:= :emits] [:vector :keyword]]]]]]])

(def Domain
  [:map
   [:domain :keyword]
   [:levels [:map-of :keyword [:map [:models [:map-of :keyword Model]]]]]])

(m/validate Domain model)                      ;; => true

(-> (m/explain Domain {:domain "oops"}) me/humanize)
;; => {:domain ["should be a keyword"], :levels ["missing required key"]}
```

`m/explain` plus `me/humanize` give precise, human-readable errors pointing at the exact path that is wrong — caught the moment you read the file, before any of it runs. (Prefer `clojure.spec`? Same idea; malli just keeps the schema as ordinary data you can inspect and compose.)

## 3. Derive behaviour by interpreting the model

The core move: you do **not** emit files. You write ordinary functions that read the model and act on it. A state machine, for example, is a function built straight from the `:transitions` vector:

```clojure
(defn transition-fn
  "Turn a model's :transitions into a (state, event) -> next-state function."
  [model]
  (let [table (into {} (for [[from to _on ev] (:transitions model)]
                         [[from ev] to]))]
    (fn step [state event]
      (get table [state event] state))))      ; unknown event => no change

(def note (get-in model [:levels :domain :models :note]))
(def step (transition-fn note))

(step :draft :publish)     ;; => :published
(step :draft :archive)     ;; => :draft   (no such transition; stays put)
```

The same model drives anything you can write a function for — each is just another walk over the map:

```clojure
(defn valid-states   [model] (:states model))   ; a state validator
(defn mermaid        [model] ,,,)                ; a state diagram (docs)
(defn datomic-schema [model] ,,,)                ; a DB schema transaction
```

This is the Clojure rhythm: edit the EDN, re-`read-string`, re-derive — live at the REPL, with no build step between you and the result. When you need to handle different *kinds* of model, dispatch with a multimethod:

```clojure
(defmulti compile-model (fn [m] (:kind m :entity)))
(defmethod compile-model :entity   [m] (transition-fn m))
(defmethod compile-model :workflow [m] (compile-workflow m))
```

## 4. Generate code with a macro (optional)

Sometimes you *do* want concrete code — a typed `defrecord`, compile-time guarantees. That is exactly what macros are: they receive the model as data at compile time and return code. The model and the code are the same kind of thing — Clojure data — so there's no template language and no separate generator.

```clojure
(defmacro defentity [model]
  `(defrecord ~(symbol (name (:name model)))
     ~(mapv (comp symbol name) (keys (:fields model)))))

(macroexpand-1
  '(defentity {:name :Note :fields {:id :NoteId :title :String :state :NoteState}}))
;; => (clojure.core/defrecord Note [id title state])
```

`macroexpand-1` lets you *see* the generated code right in the REPL before it compiles.

## 5. Load a whole project at startup

A real app keeps several model files and loads them once on boot: read each, validate it, then assemble runtime registries from the data.

```clojure
(defn load! [resources]
  (let [models (mapv #(edn/read-string (slurp (io/resource %))) resources)]
    (doseq [m models]
      (when-not (m/validate Domain m)
        (throw (ex-info "invalid DSL"
                        {:explain (me/humanize (m/explain Domain m))}))))
    ;; build whatever the runtime needs — each value is a walk over `models`:
    {:models   models
     :machines (into {} (for [dsl     models
                              [_ lvl] (:levels dsl)
                              [k md]  (:models lvl)]
                          [k (transition-fn md)]))}))

(load! ["notes.edn"])
```

That's the entire pipeline: **read EDN → validate with malli → build catalogues by walking the data.** Nothing exotic, and nothing generated to disk.

## Why Clojure fits DSL-First so well

- **Code is data (homoiconic).** Your model, your schema, and any generated code are all just Clojure data structures — one mental model, not three.
- **EDN** is a readable, diff-friendly, editor-agnostic concrete syntax you get for free — no syntax to design.
- **The REPL** makes derive-and-check instantaneous: edit the model, re-derive, see the result.
- **malli / spec** validate the model as data, with precise, path-pointing errors.
- **Macros** cover the rare case where you genuinely want emitted code.

You never leave Clojure, and you never write a parser.

---

> *Coming from a grammar-based DSL toolchain and wondering where the parser, the grammar file, and the codegen step went?* They collapse into the language: reading EDN **is** the parse, the malli schema **is** the grammar, and interpreting the data (or a macro) **is** the generator. Same DSL-First methodology — see [§2.5 of the guide](../dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md#25-two-bindings-grammar-hosted-and-data-hosted) — far fewer moving parts.
