(ns bus.memory
  "In-process IBus implementation. Handlers, queries, subscribers, and middleware
  are stored in atoms. dispatch! runs inbound middleware → handler → outbound
  middleware → event emission. Enough for the example; a real system would
  add error handling, async, network transport, etc."
  (:require [bus.protocol :as p]
            [clojure.string :as str]))

(defn- matches?
  "True if event-kw matches pattern. Supports exact match and :namespace/_ wildcards."
  [pattern event-kw]
  (or (nil? pattern)
      (= pattern event-kw)
      (and (str/ends-with? (name pattern) "_")
           (= (namespace pattern) (namespace event-kw)))))

(defrecord MemoryBus [handlers queries subscribers middleware]
  p/IBus
  (register-handler! [_ cmd-kw handler-fn]
    (swap! handlers assoc cmd-kw handler-fn))

  (register-query! [_ query-kw query-fn]
    (swap! queries assoc query-kw query-fn))

  (subscribe! [_ pattern handler-fn]
    (swap! subscribers conj [pattern handler-fn]))

  (register-middleware! [_ phase pattern handler-fn]
    (swap! middleware conj {:phase phase :pattern pattern :mw-fn handler-fn}))

  (dispatch! [this cmd]
    (let [cmd-kw (:type cmd)
          run-mw (fn [target-phase c]
                   (reduce (fn [c' {:keys [phase pattern mw-fn] :as mw}]
                             (if (and (= phase target-phase) (matches? pattern cmd-kw))
                               (mw-fn c')
                               c'))
                           c
                           (sort-by :priority @middleware)))]
      (->> (run-mw :inbound cmd)
           ((@handlers cmd-kw))
           ((fn [result]
              (run-mw :outbound (assoc cmd :result result))
              result)))))

  (query [_ q]
    (let [query-kw (:type q)]
      ((@queries query-kw) q)))

  (emit! [this event]
    (let [event-kw (:type event)]
      (doseq [[pattern handler-fn] @subscribers
              :when (matches? pattern event-kw)]
        (handler-fn event this)))))

(defn create-bus []
  (->MemoryBus (atom {}) (atom {}) (atom []) (atom [])))
