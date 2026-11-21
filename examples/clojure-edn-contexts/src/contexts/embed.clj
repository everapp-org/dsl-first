(ns contexts.embed
  "Installs bounded-context manifests (resources/contexts/*.edn) on a bus.

  This is the ADVANCED data-hosted interpreter. The minimal example
  (clojure-edn-minimal) shows a single model → one runtime function. This
  example shows four techniques that scale to production:

  1. **Multi-level interpretation** — one manifest declares commands, queries,
     subscribers, and middleware. The interpreter walks all four levels and
     wires each to the bus.

  2. **requiring-resolve dispatch** — :handled-by is a fully-qualified keyword
     like :tasks.handlers/create. The interpreter resolves it to a Clojure var
     at install time. No hand-registration; no code generation. The manifest
     IS the wiring.

  3. **Status-gated wiring** — only declarations whose :status is active
     (:in-progress, :in-test, :ready) are wired. A declaration marked :planned
     stays silently unwired — the handler may not exist yet. This lets the DSL
     describe the full intended surface while the system boots with only what
     is implemented.

  4. **Composition / leasing** — a separate EDN file (resources/instances/*.edn)
     declares which contexts to lease and which ports to bind. The same
     interpreter boots any composition; it names no application."
  (:require [clojure.edn     :as edn]
            [clojure.java.io :as io]
            [malli.error     :as me]
            [bus.protocol    :as bus]
            [contexts.schema :as schema]))

(def context-resources
  "The context manifests on the classpath, in install order."
  ["contexts/tasks.edn"
   "contexts/audit.edn"])

(def ^:private active-statuses #{:in-progress :in-test :ready})

(defn try-resolve
  "The Clojure var for a fully-qualified keyword like :tasks.handlers/create,
  or nil when the namespace / var doesn't exist yet. This is the
  requiring-resolve dispatch technique: :handled-by is data, the var is
  resolved at install time. Declared-ahead handlers stay silently unwired."
  [handler-kw]
  (when (and (keyword? handler-kw) (namespace handler-kw))
    (try
      (requiring-resolve (symbol (namespace handler-kw) (name handler-kw)))
      (catch Throwable _ nil))))

(defn- registrations
  "[[kind qualified-kw handled-by-kw] ...] for every active, handler-carrying
  command or query declaration in one context manifest."
  [ctx]
  (for [[lvl-k lvl] (:levels ctx)
        :when       (#{:commands :queries} lvl-k)
        [m-name m]  (:models lvl)
        :when       (and (:handled-by m) (active-statuses (:status m)))]
    [lvl-k (keyword (name (:domain ctx)) (name m-name)) (:handled-by m)]))

(defn- subscriptions
  "[[pattern handled-by-kw] ...] for every active subscriber in one context
  manifest's :subscribers level."
  [ctx]
  (for [[lvl-k lvl] (:levels ctx)
        :when       (= lvl-k :subscribers)
        [_ m]       (:models lvl)
        :when       (and (:handled-by m) (:pattern m) (active-statuses (:status m)))]
    [(:pattern m) (:handled-by m)]))

(defn- middlewares
  "[[phase pattern handled-by-kw] ...] for every active middleware entry in one
  context manifest's :middleware level, in ascending :priority."
  [ctx]
  (->> (for [[lvl-k lvl] (:levels ctx)
             :when       (= lvl-k :middleware)
             [_ m]       (:models lvl)
             :when       (and (:handled-by m) (:phase m) (active-statuses (:status m)))]
         m)
       (sort-by #(:priority % 0))
       (map (fn [m] [(:phase m) (:pattern m) (:handled-by m)]))))

(defn install-context!
  "Registers ONE parsed context manifest's active handlers / queries /
  subscribers / middleware on the bus. Returns the [[kind kw] ...] vector
  of what was registered. This is the per-context unit: install! composes
  it over every context-resource, and the composition interpreter leases a
  chosen subset through it."
  [b ctx]
  (when-not (schema/valid? schema/Context ctx)
    (throw (ex-info "invalid context manifest"
                    {:domain (:domain ctx)
                     :explain (me/humanize (schema/explain schema/Context ctx))})))
  (let [registered (atom [])]
    (doseq [[kind kw handler-kw] (registrations ctx)]
      (when-let [f (try-resolve handler-kw)]
        (case kind
          :commands (bus/register-handler! b kw
                      (fn [cmd] (@f (assoc cmd :_bus b))))
          :queries  (bus/register-query! b kw @f))
        (swap! registered conj [kind kw])))
    (doseq [[pattern handler-kw] (subscriptions ctx)]
      (when-let [f (try-resolve handler-kw)]
        (bus/subscribe! b pattern (fn [ev _bus] (@f ev b)))
        (swap! registered conj [:subscriber pattern])))
    (doseq [[phase pattern handler-kw] (middlewares ctx)]
      (when-let [f (try-resolve handler-kw)]
        (bus/register-middleware! b phase pattern @f)
        (swap! registered conj [:middleware handler-kw])))
    @registered))

(defn install!
  "Reads every context manifest from the classpath and registers each active
  handler / query / subscriber / middleware on the bus. Returns the
  [[kind kw] ...] vector of what was registered.

  config: {:bus <IBus>}"
  [config]
  (let [b          (:bus config)
        registered (atom [])]
    (doseq [r     context-resources
            :let  [url (io/resource r)]
            :when url
            :let  [ctx (edn/read-string (slurp url))]]
      (swap! registered into (install-context! b ctx)))
    @registered))
