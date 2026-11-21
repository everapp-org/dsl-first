package org.everapp.taskapp.tasks;

import java.util.HashMap;
import java.util.Map;

import org.everapp.taskapp.Bus;

/** Hand-written bodies of the task queries. */
public final class Queries {
    private Queries() {}

    public static Object listAll(Map<String, Object> state, Map<String, Object> payload, Bus.Emit emit) {
        return Handlers.tasks(state).stream().<Map<String, Object>>map(HashMap::new).toList();
    }
}
