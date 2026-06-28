(ns contexts.schema
  "The 'grammar' for context manifests — a malli schema. This is the data-hosted
  analogue of a .g4 grammar, with no parser. Validates the shape of every
  context manifest before interpretation."
  (:require [malli.core :as m]))

(def ActiveStatus
  "Statuses whose handlers are wired onto the bus at install time.
  A declaration marked :planned stays silently unwired — the handler
  may not exist yet. This is the status-gated wiring technique."
  [:enum :planned :in-progress :in-test :ready])

(def HandlerEntry
  "A single command, query, subscriber, or middleware declaration."
  [:map {:closed false}
   [:description {:optional true} :string]
   [:status ActiveStatus]
   [:handled-by :keyword]
   [:payload {:optional true} :any]
   [:emits {:optional true} [:vector :keyword]]
   [:read-only {:optional true} :boolean]
   [:pattern {:optional true} :keyword]
   [:phase {:optional true} [:enum :inbound :outbound]]
   [:priority {:optional true} :int]])

(def Level
  "A level within a context manifest: :commands, :queries, :subscribers, or :middleware.
  Each level has :models — a map of declaration-name → HandlerEntry."
  [:map {:closed false}
   [:models [:map-of :keyword HandlerEntry]]])

(def Context
  "A bounded-context manifest. :domain qualifies the command keywords.
  :levels carries the four declaration kinds."
  [:map {:closed false}
   [:domain :keyword]
   [:description {:optional true} :string]
   [:levels [:map-of
             [:enum :commands :queries :subscribers :middleware]
             Level]]])

(def Composition
  "An application composition: which contexts to lease, which ports to bind."
  [:map {:closed false}
   [:application :keyword]
   [:description {:optional true} :string]
   [:composes
    [:map {:closed false}
     [:leases [:vector :keyword]]
     [:binds {:optional true} [:map-of :keyword :keyword]]]]])

(defn valid? [schema value] (m/validate schema value))
(defn explain [schema value] (m/explain schema value))
