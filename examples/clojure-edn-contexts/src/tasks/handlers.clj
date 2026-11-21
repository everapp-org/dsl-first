(ns tasks.handlers
  "Command handlers for the tasks context. These are the fns that
  :handled-by keywords in tasks.edn resolve to via requiring-resolve.

  Each handler is pure: (state, command) -> events. The bus injects
  :_bus so handlers can emit events and run queries."
  (:require [bus.protocol :as bus]))

(defn create
  "Create a new task. Returns the created task map and emits :task/created."
  [{:keys [title _bus] :as cmd}]
  (let [task {:id (System/nanoTime) :title title :status :open}]
    (bus/emit! _bus {:type :task/created :task task})
    task))

(defn complete
  "Mark a task as completed. Emits :task/completed."
  [{:keys [task-id _bus] :as cmd}]
  (let [result {:task-id task-id :status :completed}]
    (bus/emit! _bus {:type :task/completed :task-id task-id})
    result))
