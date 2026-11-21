"""Hand-written bodies of the task queries."""


def list_all(state, payload, emit):
    return [dict(task) for task in state.get("tasks", [])]
