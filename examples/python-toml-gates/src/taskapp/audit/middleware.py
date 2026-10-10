"""Hand-written body of the audit context."""


def record(state, operation, payload):
    state.setdefault("audit", []).append({"operation": operation})


def entries(state, payload, emit):
    return list(state.get("audit", []))
