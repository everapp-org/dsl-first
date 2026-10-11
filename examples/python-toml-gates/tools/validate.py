#!/usr/bin/env python3
"""The gates: every rule of this project, as a program.

    python3 tools/validate.py      # exit 0 when every gate holds, 1 otherwise

Each gate is DECLARED in model/gates.toml (what it refuses, its kind, the
population it must read, a planted failure) and IMPLEMENTED here as one
function. The gate `gates-declared` compares the two lists in both directions.

A gate function returns (read, items):
  read   how many things the gate looked at. Fewer than `reads_at_least`
         is a failure: a gate that looks at nothing can never be red.
  items  one (key, message) per thing the gate refuses.

What the runner does with the items depends on the declared kind:
  hard     any item is a failure.
  ratchet  an item absent from the baseline file is a failure (new debt).
           A baseline line that no longer fails is ALSO a failure (stale).
           So the baseline only shrinks.
  report   the count of items must not exceed the declared ceiling.
"""
from __future__ import annotations

import ast
import importlib
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "src"))
sys.path.insert(0, str(ROOT / "tools"))

import generate_docs  # noqa: E402
from taskapp import model as model_reader  # noqa: E402

KINDS = {"hard", "ratchet", "report"}
REGISTRATION = {"register_handler", "register_subscriber", "register_middleware"}
LEGAL_REGISTRATION_SITE = "src/taskapp/interpreter.py"
GATES = {}


def gate(name):
    def remember(function):
        GATES[name] = function
        return function
    return remember


class Context:
    def __init__(self, root: Path):
        self.root = root
        self.model = model_reader.load(root)
        self.language = model_reader.read_toml(root / "model" / "language.toml")
        self.gates = model_reader.read_toml(root / "model" / "gates.toml")["gates"]


# --------------------------------------------------------------------------
# The gates
# --------------------------------------------------------------------------

@gate("handler-resolves")
def handler_resolves(ctx):
    """Model side: the pointer in the file. Code side: the import system.
    The two sides have different origins, so this check cannot be a tautology."""
    read, items = 0, []
    for name, _, declaration in model_reader.operations(ctx.model):
        if declaration.get("status") not in model_reader.BUILT:
            continue
        read += 1
        pointer = declaration.get("handled_by", "")
        module_name, _, function_name = pointer.partition(":")
        try:
            target = getattr(importlib.import_module(module_name), function_name)
        except (ImportError, AttributeError, ValueError):
            target = None
        if not callable(target):
            items.append((name, f"{name}: handled_by '{pointer}' is not a function. "
                                f"Write the function, or correct the pointer."))
    return read, items


@gate("single-registration-site")
def single_registration_site(ctx):
    read, items = 0, []
    for path in sorted((ctx.root / "src").rglob("*.py")):
        read += 1
        relative = path.relative_to(ctx.root).as_posix()
        if relative == LEGAL_REGISTRATION_SITE:
            continue
        for node in ast.walk(ast.parse(path.read_text(encoding="utf-8"))):
            if (isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute)
                    and node.func.attr in REGISTRATION):
                items.append((f"{relative}:{node.lineno}",
                              f"{relative}:{node.lineno} calls {node.func.attr}. "
                              f"Declare the operation in model/contexts/ and let the interpreter wire it."))
    return read, items


@gate("event-declared")
def event_declared(ctx):
    declared = model_reader.events(ctx.model)
    read, items = 0, []
    for name, level, declaration in model_reader.operations(ctx.model):
        named = list(declaration.get("emits", []))
        if level == "subscribers":
            named.append(declaration.get("pattern", ""))
        for event in named:
            read += 1
            if event not in declared:
                items.append((f"{name}->{event}",
                              f"{name} names the event '{event}', and no context declares it. "
                              f"Add [events.\"{event}\"] with its payload."))
    return read, items


@gate("wiring-typed")
def wiring_typed(ctx):
    catalogue = {name: d for name, _, d in model_reader.operations(ctx.model, ("commands", "queries"))}
    read, items = 0, []
    for wiring in ctx.model["app"].get("wirings", []):
        read += 1
        label = wiring["name"]
        source, target = catalogue.get(wiring["from"]), catalogue.get(wiring["to"])
        if source is None or target is None:
            items.append((label, f"wiring {label}: '{wiring['from']}' and '{wiring['to']}' "
                                 f"must both be declared operations."))
            continue
        returned = source.get("result_item", source.get("result"))
        if returned is None:
            items.append((label, f"wiring {label}: '{wiring['from']}' declares no result. "
                                 f"A source must say what it returns."))
            continue
        gives = returned.get(wiring["field"])
        takes = target.get("payload", {}).get(wiring["payload_field"])
        if gives is None or takes is None or gives != takes:
            items.append((label, f"wiring {label}: {wiring['from']}.{wiring['field']} is {gives}, "
                                 f"and {wiring['to']}.{wiring['payload_field']} is {takes}. "
                                 f"Make the two declarations agree."))
    return read, items


@gate("not-built-stated")
def not_built_stated(ctx):
    read, items = 0, []
    for name, _, declaration in model_reader.operations(ctx.model):
        read += 1
        status = declaration.get("status")
        if status not in model_reader.STATUSES:
            items.append((name, f"{name}: status '{status}' is not one of {sorted(model_reader.STATUSES)}."))
        elif status == "not-built" and (not declaration.get("reason") or "handled_by" in declaration):
            items.append((name, f"{name}: a not-built operation gives a reason and names no handler."))
    return read, items


def identifiers(ctx):
    for context_name, context in ctx.model["contexts"].items():
        yield context_name, f"context {context_name}"
        for level in (*model_reader.LEVELS, "events"):
            for name, declaration in context.get(level, {}).items():
                where = f"{context_name}.{name}"
                yield name, where
                for key in ("payload", "result", "result_item"):
                    for field in declaration.get(key, {}):
                        yield field, f"{where} field {field}"
    for wiring in ctx.model["app"].get("wirings", []):
        yield wiring["name"], f"wiring {wiring['name']}"


@gate("vocabulary")
def vocabulary(ctx):
    approved = {refused: term
                for term, entry in ctx.language["terms"].items()
                for refused in entry["refuses"]}
    read, items = 0, []
    for identifier, where in identifiers(ctx):
        read += 1
        for part in re.split(r"[._-]", identifier):
            if part in approved:
                items.append((where, f"{where}: the word '{part}' is refused. "
                                     f"Write '{approved[part]}' (model/language.toml)."))
    return read, items


@gate("described")
def described(ctx):
    read, items = 0, []
    named = [(name, d) for name, _, d in model_reader.operations(ctx.model)]
    named += list(model_reader.events(ctx.model).items())
    for name, declaration in named:
        read += 1
        if not declaration.get("description", "").strip():
            items.append((name, f"{name} has no description."))
    return read, items


@gate("not-built-count")
def not_built_count(ctx):
    all_operations = list(model_reader.operations(ctx.model))
    items = [(name, f"{name} is declared and not built.")
             for name, _, declaration in all_operations if declaration.get("status") == "not-built"]
    return len(all_operations), items


@gate("docs-fresh")
def docs_fresh(ctx):
    read, items = 0, []
    for relative, text in generate_docs.render(ctx.root).items():
        read += 1
        path = ctx.root / relative
        if not path.exists() or path.read_text(encoding="utf-8") != text:
            items.append((relative, f"{relative} differs from the model. "
                                    f"Run: python3 tools/generate_docs.py"))
    return read, items


@gate("gates-declared")
def gates_declared(ctx):
    """The rules about the rules. Declared gates and implemented gates must be
    the same set, and each declaration must be complete."""
    declared, implemented = set(ctx.gates), set(GATES)
    items = [(name, f"gate '{name}' is declared in model/gates.toml and no function implements it.")
             for name in sorted(declared - implemented)]
    items += [(name, f"gate '{name}' is implemented and model/gates.toml does not declare it.")
              for name in sorted(implemented - declared)]
    for name, declaration in sorted(ctx.gates.items()):
        kind = declaration.get("kind")
        if kind not in KINDS:
            items.append((name, f"gate '{name}': kind '{kind}' is not one of {sorted(KINDS)}."))
        if kind == "ratchet" and "baseline" not in declaration:
            items.append((name, f"gate '{name}': a ratchet names its baseline file."))
        if kind == "report" and "ceiling" not in declaration:
            items.append((name, f"gate '{name}': a report with no ceiling can grow with no failure. "
                                f"Give it a ceiling."))
        if not declaration.get("planted"):
            items.append((name, f"gate '{name}': no planted failure. "
                                f"A gate that nobody saw fail is not yet a rule."))
        if "reads_at_least" not in declaration or not declaration.get("refuses"):
            items.append((name, f"gate '{name}': 'refuses' and 'reads_at_least' are required."))
    return len(declared | implemented), items


# --------------------------------------------------------------------------
# The runner
# --------------------------------------------------------------------------

def read_baseline(path: Path) -> set:
    if not path.exists():
        return set()
    lines = (line.strip() for line in path.read_text(encoding="utf-8").splitlines())
    return {line for line in lines if line and not line.startswith("#")}


def run(root: Path):
    ctx = Context(root)
    failures, lines = [], []
    for name in sorted(GATES):
        declaration = ctx.gates.get(name, {"kind": "hard", "reads_at_least": 1})
        kind = declaration.get("kind", "hard")
        read, items = GATES[name](ctx)
        found = []
        if read < declaration.get("reads_at_least", 1):
            found.append(f"read {read}, and it must read at least {declaration['reads_at_least']}. "
                         f"A gate that reads nothing cannot fail.")
        if kind == "ratchet":
            baseline = read_baseline(root / declaration.get("baseline", ""))
            failing = {key for key, _ in items}
            found += [message + " This is new. Correct it." for key, message in items if key not in baseline]
            found += [f"'{key}' is in {declaration['baseline']} and no longer fails. Delete the line."
                      for key in sorted(baseline - failing)]
            note = f"{len(failing & baseline)} in the baseline"
        elif kind == "report":
            ceiling = declaration.get("ceiling", 0)
            if len(items) > ceiling:
                found += [message for _, message in items]
                found.append(f"count {len(items)} is above the ceiling {ceiling}.")
            note = f"count {len(items)}, ceiling {ceiling}"
        else:
            found += [message for _, message in items]
            note = ""
        lines.append(f"{'FAIL' if found else 'ok  '}  {name:<26} {kind:<8} read {read:<3} {note}")
        failures += [f"[{name}] {message}" for message in found]
    return lines, failures


if __name__ == "__main__":
    report, problems = run(ROOT)
    print("\n".join(report))
    for problem in problems:
        print("FAIL " + problem)
    print(f"\n{len(GATES)} gates, {len(problems)} failure(s)")
    sys.exit(1 if problems else 0)
