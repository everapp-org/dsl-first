"""The one seam that every command, query and event crosses.

The bus knows the catalogue, which the interpreter derives from the model.
It refuses an operation that the model does not declare, a payload that has
the wrong shape, and an event that the operation did not declare.
"""
from __future__ import annotations

TYPES = {"str": str, "int": int, "bool": bool}


class Refused(Exception):
    """The bus refused a request. The message names what was refused."""


class NotBuilt(Refused):
    """The operation is declared as not built. The message gives the reason."""


def check_shape(what: str, declared: dict, value: dict) -> None:
    if set(value) != set(declared):
        raise Refused(f"{what}: fields {sorted(value)} given, {sorted(declared)} declared")
    for field, type_name in declared.items():
        python_type = TYPES[type_name]
        item = value[field]
        if not isinstance(item, python_type) or (python_type is int and isinstance(item, bool)):
            raise Refused(f"{what}: field '{field}' must be {type_name}, got {type(item).__name__}")


class Bus:
    def __init__(self, catalogue: dict, event_catalogue: dict):
        self._catalogue = catalogue
        self._events = event_catalogue
        self._handlers = {}
        self._subscribers = []
        self._middleware = []
        self.state = {}

    # -- registration: called by the interpreter and by nothing else ---------

    def register_handler(self, name, function):
        self._handlers[name] = function

    def register_subscriber(self, pattern, function):
        self._subscribers.append((pattern, function))

    def register_middleware(self, function):
        self._middleware.append(function)

    def registered(self) -> set:
        return set(self._handlers)

    # -- use ------------------------------------------------------------------

    def dispatch(self, name: str, payload: dict | None = None):
        payload = payload or {}
        declaration = self._catalogue.get(name)
        if declaration is None:
            raise Refused(f"'{name}' is not a declared operation")
        if declaration["status"] == "not-built":
            raise NotBuilt(f"'{name}' is not built yet: {declaration['reason']}")
        check_shape(f"payload of {name}", declaration.get("payload", {}), payload)
        for middleware in self._middleware:
            middleware(self.state, name, payload)
        allowed = set(declaration.get("emits", []))

        def emit(event: str, event_payload: dict):
            if event not in allowed:
                raise Refused(f"'{name}' emitted '{event}', which it does not declare")
            check_shape(f"payload of {event}", self._events[event]["payload"], event_payload)
            for pattern, subscriber in self._subscribers:
                if pattern == event:
                    subscriber(self.state, event, event_payload)

        return self._handlers[name](self.state, payload, emit)
