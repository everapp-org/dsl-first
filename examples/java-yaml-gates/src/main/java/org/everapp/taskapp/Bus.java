package org.everapp.taskapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The one seam that every command, query and event crosses.
 *
 * The bus knows the catalogue, which the interpreter derives from the model.
 * It refuses an operation that the model does not declare, a payload that has
 * the wrong shape, and an event that the operation did not declare.
 */
public final class Bus {
    private static final Map<String, Class<?>> TYPES =
            Map.of("str", String.class, "int", Integer.class, "bool", Boolean.class);

    /** The bus refused a request. The message names what was refused. */
    public static class Refused extends RuntimeException {
        public Refused(String message) {
            super(message);
        }
    }

    /** The operation is declared as not built. The message gives the reason. */
    public static final class NotBuilt extends Refused {
        public NotBuilt(String message) {
            super(message);
        }
    }

    @FunctionalInterface
    public interface Emit {
        void emit(String event, Map<String, Object> payload);
    }

    @FunctionalInterface
    public interface Handler {
        Object handle(Map<String, Object> state, Map<String, Object> payload, Emit emit);
    }

    @FunctionalInterface
    public interface Subscriber {
        void on(Map<String, Object> state, String event, Map<String, Object> payload);
    }

    @FunctionalInterface
    public interface Middleware {
        void before(Map<String, Object> state, String operation, Map<String, Object> payload);
    }

    private record Subscription(String pattern, Subscriber subscriber) {}

    private final Map<String, Map<String, Object>> catalogue;
    private final Map<String, Map<String, Object>> events;
    private final Map<String, Handler> handlers = new HashMap<>();
    private final List<Subscription> subscribers = new ArrayList<>();
    private final List<Middleware> middleware = new ArrayList<>();
    public final Map<String, Object> state = new HashMap<>();

    public Bus(Map<String, Map<String, Object>> catalogue, Map<String, Map<String, Object>> events) {
        this.catalogue = catalogue;
        this.events = events;
    }

    // -- registration: called by the interpreter and by nothing else ---------

    public void registerHandler(String name, Handler handler) {
        handlers.put(name, handler);
    }

    public void registerSubscriber(String pattern, Subscriber subscriber) {
        subscribers.add(new Subscription(pattern, subscriber));
    }

    public void registerMiddleware(Middleware function) {
        middleware.add(function);
    }

    public Set<String> registered() {
        return new HashSet<>(handlers.keySet());
    }

    // -- use ------------------------------------------------------------------

    public Object dispatch(String name) {
        return dispatch(name, Map.of());
    }

    public Object dispatch(String name, Map<String, Object> payload) {
        Map<String, Object> declaration = catalogue.get(name);
        if (declaration == null) {
            throw new Refused("'" + name + "' is not a declared operation");
        }
        if ("not-built".equals(declaration.get("status"))) {
            throw new NotBuilt("'" + name + "' is not built yet: " + declaration.get("reason"));
        }
        checkShape("payload of " + name, Model.map(declaration.get("payload")), payload);
        for (Middleware function : middleware) {
            function.before(state, name, payload);
        }
        Set<String> allowed = new HashSet<>(Model.strings(declaration.get("emits")));
        Emit emit = (event, eventPayload) -> {
            if (!allowed.contains(event)) {
                throw new Refused("'" + name + "' emitted '" + event + "', which it does not declare");
            }
            checkShape("payload of " + event, Model.map(events.get(event).get("payload")), eventPayload);
            for (Subscription subscription : subscribers) {
                if (subscription.pattern().equals(event)) {
                    subscription.subscriber().on(state, event, eventPayload);
                }
            }
        };
        return handlers.get(name).handle(state, payload, emit);
    }

    private static void checkShape(String what, Map<String, Object> declared, Map<String, Object> value) {
        if (!value.keySet().equals(declared.keySet())) {
            throw new Refused(what + ": fields " + new TreeSet<>(value.keySet()) + " given, "
                    + new TreeSet<>(declared.keySet()) + " declared");
        }
        declared.forEach((field, typeName) -> {
            Class<?> type = TYPES.get(String.valueOf(typeName));
            Object item = value.get(field);
            if (type == null || !type.isInstance(item)) {
                throw new Refused(what + ": field '" + field + "' must be " + typeName + ", got "
                        + (item == null ? "null" : item.getClass().getSimpleName()));
            }
        });
    }
}
