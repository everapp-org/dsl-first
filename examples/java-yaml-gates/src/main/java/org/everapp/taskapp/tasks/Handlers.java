package org.everapp.taskapp.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.everapp.taskapp.Bus;

/**
 * Hand-written bodies of the task commands. The model decides that they exist,
 * what they receive and what they may emit. This file decides what they do.
 */
public final class Handlers {
    private Handlers() {}

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> tasks(Map<String, Object> state) {
        return (List<Map<String, Object>>) state.computeIfAbsent("tasks", key -> new ArrayList<Map<String, Object>>());
    }

    public static Object create(Map<String, Object> state, Map<String, Object> payload, Bus.Emit emit) {
        List<Map<String, Object>> tasks = tasks(state);
        Map<String, Object> task = new HashMap<>();
        task.put("task_id", tasks.size() + 1);
        task.put("title", payload.get("title"));
        task.put("done", false);
        tasks.add(task);
        emit.emit("task.created", Map.of("task_id", task.get("task_id"), "title", task.get("title")));
        return Map.of("task_id", task.get("task_id"));
    }

    public static Object complete(Map<String, Object> state, Map<String, Object> payload, Bus.Emit emit) {
        for (Map<String, Object> task : tasks(state)) {
            if (task.get("task_id").equals(payload.get("task_id"))) {
                task.put("done", true);
                emit.emit("task.completed", Map.of("task_id", task.get("task_id")));
                return Map.of("task_id", task.get("task_id"), "done", true);
            }
        }
        throw new NoSuchElementException("no task " + payload.get("task_id"));
    }

    public static void countCompleted(Map<String, Object> state, String event, Map<String, Object> payload) {
        state.merge("completed_count", 1, (old, one) -> (Integer) old + 1);
    }
}
