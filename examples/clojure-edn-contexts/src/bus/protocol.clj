(ns bus.protocol
  "A minimal in-process bus protocol. In a real system this might be TCP,
  WebSocket, or a distributed queue. Here it is an atom-backed registry
  — enough to demonstrate the interpreter patterns without external deps.")

(defprotocol IBus
  (register-handler! [bus cmd-kw handler-fn]
    "Register a command handler for cmd-kw. handler-fn takes a command map.")
  (register-query! [bus query-kw query-fn]
    "Register a read-only query handler. query-fn takes a query map and returns data.")
  (subscribe! [bus pattern handler-fn]
    "Subscribe to events matching pattern (a keyword, possibly with wildcards).")
  (register-middleware! [bus phase pattern handler-fn]
    "Register middleware: :inbound runs before handlers, :outbound after.
    pattern nil means every command/event.")
  (dispatch! [bus cmd]
    "Dispatch a command map (must contain :type). Returns the handler's result.")
  (query [bus q]
    "Execute a read-only query map (must contain :type). Returns data.")
  (emit! [bus event]
    "Emit an event to all matching subscribers."))
