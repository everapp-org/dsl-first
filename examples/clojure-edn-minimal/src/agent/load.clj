(ns agent.load
  "The whole pipeline: read -> validate -> derive."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [malli.error :as me]
            [agent.schema :as schema]
            [agent.runtime :as rt]))

(defn load!
  "Read the EDN model, validate it against the schema, and derive a runtime
   state machine from it. Throws with a humanized explanation if invalid."
  [resource]
  (let [dsl (edn/read-string (slurp (io/resource resource)))]
    (when-not (schema/valid? dsl)
      (throw (ex-info "invalid DSL"
                      {:explain (me/humanize (schema/explain dsl))})))
    {:dsl  dsl
     :step (rt/transition-fn dsl)}))

(comment
  ;; Try it at the REPL:  clj
  (def sys (load! "agent.edn"))
  ((:step sys) :off  :activate)      ;; => :idle
  ((:step sys) :idle :assign-task)   ;; => :working
  ((:step sys) :idle :explode)       ;; => :idle   (no such transition)
  )
