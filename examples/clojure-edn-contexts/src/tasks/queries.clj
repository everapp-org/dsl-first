(ns tasks.queries
  "Query handlers for the tasks context. Read-only — no side effects.")

(defn list-all
  "List all tasks. In a real system this would query a store.
  Here it returns a stub for demonstration."
  [_query]
  [{:id 1 :title "Example task" :status :open}])
