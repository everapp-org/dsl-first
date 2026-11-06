"""Read the model. Every other module gets the model from here.

The model is plain data in TOML files. There is no grammar and no parser of
our own: `tomllib` is in the standard library since Python 3.11.
"""
from __future__ import annotations

import tomllib
from pathlib import Path

LEVELS = ("commands", "queries", "subscribers", "middleware")
BUILT = {"ready", "in-progress"}
STATUSES = BUILT | {"not-built"}


def read_toml(path: Path) -> dict:
    with path.open("rb") as handle:
        return tomllib.load(handle)


def load(root: Path) -> dict:
    """Return {"app": ..., "contexts": {name: ...}} for the application at root."""
    model_dir = Path(root) / "model"
    app = read_toml(model_dir / "app.toml")
    contexts = {}
    for path in sorted((model_dir / "contexts").glob("*.toml")):
        context = read_toml(path)
        contexts[context["context"]] = context
    return {"app": app, "contexts": contexts}


def operations(model: dict, levels=LEVELS):
    """Yield (full name, level, declaration) for each declared operation."""
    for context_name, context in sorted(model["contexts"].items()):
        for level in levels:
            for name, declaration in sorted(context.get(level, {}).items()):
                yield f"{context_name}.{name}", level, declaration


def events(model: dict) -> dict:
    declared = {}
    for context in model["contexts"].values():
        declared.update(context.get("events", {}))
    return declared
