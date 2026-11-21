(ns compose
  "The application-composition interpreter.

  An application is a value (resources/instances/<app>.edn) naming the
  platform contexts it leases and the ports it binds. This namespace is
  the form's schema + loader + interpreter — the read side and the boot
  side. The same interpreter boots any composition; it names no application.

  This is the composition / leasing technique: the manifest interpreter
  (contexts.embed) installs individual contexts; the composition interpreter
  decides WHICH contexts to install. Separating 'what a context declares'
  from 'which contexts an application needs' keeps both as data."
  (:require [clojure.edn     :as edn]
            [clojure.java.io :as io]
            [malli.core      :as m]
            [malli.error     :as me]
            [contexts.schema :as schema]
            [contexts.embed  :as contexts]))

(def ^:private validator (m/validator schema/Composition))
(def ^:private explainer (m/explainer schema/Composition))

(defn valid?
  "True when the composition satisfies the form schema."
  [composition]
  (validator composition))

(defn explain
  "A humanized explanation of why a composition is invalid, or nil when valid."
  [composition]
  (when-not (validator composition)
    (me/humanize (explainer composition))))

(defn load-composition
  "Reads a composition value from the classpath, e.g. \"instances/task-app.edn\".
  Throws when the resource is absent or the composition is invalid."
  [resource-path]
  (let [url (io/resource resource-path)]
    (when-not url
      (throw (ex-info (str "composition not found on classpath: " resource-path)
                      {:resource resource-path})))
    (let [composition (edn/read-string (slurp url))]
      (when-not (valid? composition)
        (throw (ex-info "invalid composition"
                        {:explain (explain composition)})))
      composition)))

(defn- context-manifest-by-domain
  "The parsed platform context manifest whose :domain is domain-key, or nil."
  [domain-key]
  (some (fn [r]
          (when-let [url (io/resource r)]
            (let [ctx (edn/read-string (slurp url))]
              (when (= domain-key (:domain ctx)) ctx))))
        contexts/context-resources))

(defn install-leased-context!
  "Install ONE leased platform context (by :domain keyword) on the bus,
  returning its [[kind kw] ...] registrations."
  [bus domain-key]
  (if-let [ctx (context-manifest-by-domain domain-key)]
    (contexts/install-context! bus ctx)
    (throw (ex-info (str "composition leases an unknown context: " domain-key)
                    {:domain domain-key}))))

(defn interpret!
  "Boot a composition onto the bus: install its leased platform contexts.
  Returns the [[kind kw] ...] registrations. Generic — names no application;
  the same interpreter boots task-app, echo, or any composition value."
  [bus composition]
  (let [c (:composes composition)]
    (vec (mapcat #(install-leased-context! bus %) (:leases c)))))
