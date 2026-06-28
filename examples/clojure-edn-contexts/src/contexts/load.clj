(ns contexts.load
  "The whole pipeline: read composition -> validate -> interpret -> boot.

  This ties together the composition interpreter (compose) and the
  context interpreter (contexts.embed) into a single boot entry point.
  Compare to clojure-edn-minimal/load.clj which does read -> validate ->
  derive for a single model. Here the same three steps operate over
  multiple context manifests composed into one application."
  (:require [bus.memory   :as mem]
            [compose       :as c]))

(defn boot!
  "Boot an application from a composition file on the classpath.
  Returns {:bus bus :registered registrations}.

  Usage:
    (boot! \"instances/task-app.edn\")
    => {:bus <MemoryBus> :registered [[:commands :tasks/create] ...]}"
  [composition-resource]
  (let [bus          (mem/create-bus)
        composition  (c/load-composition composition-resource)
        registered   (c/interpret! bus composition)]
    {:bus bus :registered registered}))

(comment
  ;; Try it at the REPL:  clj
  (require '[contexts.load :as l])
  (def sys (l/boot! "instances/task-app.edn"))
  (:registered sys)
  ;; => [[:commands :tasks/create] [:commands :tasks/complete]
  ;;     [:queries :tasks/list] [:subscriber :task/created]
  ;;     [:middleware :audit.middleware/record-command]]

  (require '[bus.protocol :as bus])
  (bus/dispatch! (:bus sys) {:type :tasks/create :title "Write docs"})
  ;; [audit] command: :tasks/create
  ;; [subscriber] event: :task/created
  ;; => {:id <nanoTime> :title "Write docs" :status :open}

  (bus/query (:bus sys) {:type :tasks/list})
  ;; => [{:id 1 :title "Example task" :status :open}]
  )
