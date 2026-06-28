(ns tasks.subscribers
  "Event subscribers for the tasks context. These fire asynchronously
  when matching events are emitted on the bus.")

(defn log-event
  "Log an event for debugging. In a real system this would write to
  a log, metric, or audit store."
  [event _bus]
  (println (str "[subscriber] event: " (:type event))))
