package org.everapp.taskapp.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.everapp.taskapp.Bus;

/** Hand-written body of the audit context. */
public final class Middleware {
    private Middleware() {}

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> audit(Map<String, Object> state) {
        return (List<Map<String, Object>>) state.computeIfAbsent("audit", key -> new ArrayList<Map<String, Object>>());
    }

    public static void record(Map<String, Object> state, String operation, Map<String, Object> payload) {
        audit(state).add(Map.of("operation", operation));
    }

    public static Object entries(Map<String, Object> state, Map<String, Object> payload, Bus.Emit emit) {
        return List.copyOf(audit(state));
    }
}
