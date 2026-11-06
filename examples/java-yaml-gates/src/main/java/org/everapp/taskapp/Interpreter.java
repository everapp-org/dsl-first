package org.everapp.taskapp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The interpreter: it wires an application from its model.
 *
 * This is the ONE file that may call registerHandler, registerSubscriber or
 * registerMiddleware. The gate `single-registration-site` refuses such a call
 * in any other file. So a capability can join the application only through
 * the model.
 */
public final class Interpreter {
    private Interpreter() {}

    /** The parameter types that a handler of each level must have. */
    public static Class<?>[] shape(String level) {
        return level.equals("commands") || level.equals("queries")
                ? new Class<?>[] {Map.class, Map.class, Bus.Emit.class}
                : new Class<?>[] {Map.class, String.class, Map.class};
    }

    /** Turn "package.Class#method" into the method. */
    static Method resolve(String pointer, String level) {
        String[] parts = pointer.split("#", 2);
        try {
            return Class.forName(parts[0]).getMethod(parts[1], shape(level));
        } catch (ReflectiveOperationException | ArrayIndexOutOfBoundsException e) {
            throw new IllegalStateException("handled_by '" + pointer + "' does not resolve", e);
        }
    }

    private static Object call(Method method, Object... arguments) {
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw new IllegalStateException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Commands and queries of the contexts that the application composes. */
    static Map<String, Map<String, Object>> catalogue(Model model) {
        Set<String> composed = new HashSet<>(Model.strings(model.app.get("contexts")));
        Map<String, Map<String, Object>> out = new TreeMap<>();
        for (Model.Operation operation : model.operations(List.of("commands", "queries"))) {
            if (composed.contains(operation.context())) {
                out.put(operation.name(), operation.declaration());
            }
        }
        return out;
    }

    public static Bus start(Path root) {
        Model model = Model.load(root);
        Bus bus = new Bus(catalogue(model), model.events());
        Set<String> composed = new HashSet<>(Model.strings(model.app.get("contexts")));
        for (Model.Operation operation : model.operations()) {
            if (!composed.contains(operation.context())) {
                continue;
            }
            if (!Model.BUILT.contains(operation.status())) {
                continue; // declared, and not wired
            }
            Method method = resolve(Model.text(operation.declaration().get("handled_by")), operation.level());
            switch (operation.level()) {
                case "commands", "queries" ->
                        bus.registerHandler(operation.name(), (state, payload, emit) -> call(method, state, payload, emit));
                case "subscribers" ->
                        bus.registerSubscriber(Model.text(operation.declaration().get("pattern")),
                                (state, event, payload) -> call(method, state, event, payload));
                case "middleware" ->
                        bus.registerMiddleware((state, name, payload) -> call(method, state, name, payload));
                default -> throw new IllegalStateException("unknown level " + operation.level());
            }
        }
        return bus;
    }
}
