"""Hand-written bodies of the task commands. The model decides that they exist,
what they receive and what they may emit. This file decides what they do."""


def create(state, payload, emit):
    tasks = state.setdefault("tasks", [])
    task = {"task_id": len(tasks) + 1, "title": payload["title"], "done": False}
    tasks.append(task)
    emit("task.created", {"task_id": task["task_id"], "title": task["title"]})
    return {"task_id": task["task_id"]}


def complete(state, payload, emit):
    for task in state.get("tasks", []):
        if task["task_id"] == payload["task_id"]:
            task["done"] = True
            emit("task.completed", {"task_id": task["task_id"]})
            return {"task_id": task["task_id"], "done": True}
    raise LookupError(f"no task {payload['task_id']}")


def count_completed(state, event, payload):
    state["completed_count"] = state.get("completed_count", 0) + 1
