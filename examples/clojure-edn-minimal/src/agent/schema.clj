(ns agent.schema
  "The 'grammar' for our data DSL — a malli schema. Schemas are themselves data,
   so this is the data-hosted analogue of a .g4 grammar, with no parser."
  (:require [malli.core :as m]))

(def Domain
  [:map
   [:domain :keyword]
   [:states [:set :keyword]]
   [:transitions [:vector [:tuple :keyword :keyword :keyword]]]])  ; [action from to]

(defn valid?  [dsl] (m/validate Domain dsl))
(defn explain [dsl] (m/explain  Domain dsl))
