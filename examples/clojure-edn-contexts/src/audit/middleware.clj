(ns audit.middleware
  "Middleware for the audit context. Cross-cutting bus taps declared as data
  in the context manifest, not hand-registered in the boot code.")

(defn record-command
  "Record every command before dispatch. In a real system this would
  write to an audit journal. Here it prints for demonstration."
  [cmd]
  (println (str "[audit] command: " (:type cmd)))
  cmd)
