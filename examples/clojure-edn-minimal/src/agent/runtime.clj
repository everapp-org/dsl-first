(ns agent.runtime
  "Derive behaviour by interpreting the model data — no code generation.")

(defn transition-fn
  "Build a (state, action) -> next-state function from the model's :transitions."
  [dsl]
  (let [table (into {} (for [[action from to] (:transitions dsl)]
                         [[from action] to]))]
    (fn step [state action]
      (get table [state action] state))))   ; unknown action => state unchanged
