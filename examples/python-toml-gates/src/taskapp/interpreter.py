"""The interpreter: it wires an application from its model.

This is the ONE file that may call `register_handler`, `register_subscriber`
or `register_middleware`. The gate `single-registration-site` refuses such a
call in any other file. So a capability can join the application only through
the model.
"""
from __future__ import annotations

import importlib
from pathlib import Path

from taskapp import model as model_reader
from taskapp.bus import Bus


def resolve(pointer: str):
    """Turn 'package.module:function' into the function."""
    module_name, _, function_name = pointer.partition(":")
    return getattr(importlib.import_module(module_name), function_name)


def catalogue(model: dict) -> dict:
    """Commands and queries of the contexts that the application composes."""
    composed = set(model["app"]["contexts"])
    return {
        name: declaration
        for name, level, declaration in model_reader.operations(model, ("commands", "queries"))
        if name.split(".")[0] in composed
    }


def start(root: Path) -> Bus:
    model = model_reader.load(root)
    bus = Bus(catalogue(model), model_reader.events(model))
    composed = set(model["app"]["contexts"])
    for name, level, declaration in model_reader.operations(model):
        if name.split(".")[0] not in composed:
            continue
        if declaration["status"] not in model_reader.BUILT:
            continue  # declared, and not wired
        function = resolve(declaration["handled_by"])
        if level in ("commands", "queries"):
            bus.register_handler(name, function)
        elif level == "subscribers":
            bus.register_subscriber(declaration["pattern"], function)
        elif level == "middleware":
            bus.register_middleware(function)
    return bus
