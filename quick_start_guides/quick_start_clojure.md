# DSL-First: Clojure (Data, Not Grammar) Guide

In a homoiconic language the DSL **is data**. You don't write a grammar, you don't run a parser, and you don't generate source text. You write the model as EDN, validate it with a **schema** (malli or `clojure.spec`), and **derive** the runtime by *interpreting* that data — or, when you want emitted code, with a macro.

> This is the **data-hosted binding** of DSL-First. The metamodel (`domain / level / model / fields / states / transitions`) is identical to the [Kernel DSL](../dsl_first_methodology/KERNEL_DSL.md); only the carrier and the derivation differ. For the grammar-hosted binding, see the [Java + ANTLR guide](quick_start_java.md).

## 1. Add a schema library

```clojure
;; deps.edn
{:deps {metosin/malli {:mvn/version "0.16.0"}}}
```

## 2. Write the DSL as EDN

There is no `.g4` and no concrete syntax to invent — the model is a Clojure map in a `.edn` file:

```clojure
;; notes.edn
{:domain :notes
 :levels
 {:domain
  {:models
   {:note
    {:description "A user note."
     :fields  {:id :NoteId, :title :String, :state :NoteState}
     :states  #{:draft :published}
     :transitions
     [[:draft :published :on :publish :emits [:note-published]]]}}}}}
```

## 3. Validate with a schema (this is your "grammar")

The role ANTLR plays in the grammar-hosted world is played here by a **schema**. One malli schema validates the shape of every model file:

```clojure
(require '[malli.core :as m] '[clojure.edn :as edn])

(def Model
  [:map
   [:description {:optional true} :string]
   [:fields [:map-of :keyword :keyword]]
   [:states {:optional true} [:set :keyword]]
   [:transitions {:optional true}
    [:vector [:cat :keyword :keyword [:= :on] :keyword [:* :any]]]]])

(def Domain
  [:map
   [:domain :keyword]
   [:levels [:map-of :keyword [:map [:models [:map-of :keyword Model]]]]]])

(def dsl (edn/read-string (slurp "notes.edn")))
(assert (m/validate Domain dsl) (m/explain Domain dsl))
```

This is exactly what a real data-hosted runtime does: `edn/read-string` each file, then `m/validate` it against one `:domain` schema — read, then validate, no parse step in between.

## 4. Derive — by interpretation

There is no code-generation step. You **walk the validated data** and build whatever the runtime needs — a registry, a state-machine function, a set of event handlers:

```clojure
(defn transition-fn
  "Build a (state, event) -> next-state fn for one model, straight from the data."
  [model]
  (let [table (into {} (for [[from to _ ev] (:transitions model)]
                         [[from ev] to]))]
    (fn [state event] (get table [state event] state))))

(def note-step (transition-fn (get-in dsl [:levels :domain :models :note])))
(note-step :draft :publish)   ;; => :published
(note-step :published :publish) ;; => :published  (no transition; stays put)
```

The *same* data can drive validation, a Datomic/SQL schema install, documentation, and test generation — each is just another walk over the map. A production runtime assembles its tool / provider / handler catalogues this way at startup, and turns a model's `:attributes` into a database schema transaction.

## 5. Derive — by macro (optional)

When you do want *emitted* source (a typed record, compile-time checks), a macro expands the **same** model data into code at compile time — the homoiconic equivalent of JavaPoet, with no separate template language:

```clojure
(defmacro defmodel [model-name m]
  `(defrecord ~(symbol (name model-name))
     ~(mapv (comp symbol name) (keys (:fields m)))))

(defmodel Note {:fields {:id :NoteId :title :String :state :NoteState}})
;; => (Note. id title state)
```

## Grammar-hosted vs data-hosted, at a glance

| | Grammar-hosted (Java, C#, Go) | Data-hosted (Clojure, Lisp) |
|---|---|---|
| DSL carrier | text file | EDN / Clojure data |
| "Grammar" | ANTLR `.g4` grammar | malli / spec schema |
| Parse step | lexer + parser → AST | `edn/read-string` (no parser) |
| Derivation | generator emits source (JavaPoet) | interpret the data, or a macro |
| Build artifact | generated `.java` files | runtime registries (+ optional macro output) |

Same metamodel. Same single-source-of-truth discipline. Far less machinery.
