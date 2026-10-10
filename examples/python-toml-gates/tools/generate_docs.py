#!/usr/bin/env python3
"""Derive the documentation from the model.

    python3 tools/generate_docs.py          # write docs/generated/

The files in docs/generated/ are never edited by hand. The gate `docs-fresh`
derives them again and fails the build when a committed file differs.

The generator reads every optional key with `.get`. A model with a missing key
must reach the gate that names the defect. It must not stop here in a
traceback: the planted failures of tools/verify_gates.py found that defect in
the first version of this file.
"""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "src"))

from taskapp import model as model_reader  # noqa: E402

HEADER = "<!-- GENERATED from model/ by tools/generate_docs.py. Do not edit. -->\n\n"


def shape(fields: dict) -> str:
    if not fields:
        return "nothing"
    return ", ".join(f"`{name}`: {type_name}" for name, type_name in fields.items())


def reference(model: dict) -> str:
    app = model["app"]
    out = [HEADER, f"# {app['application']} — reference\n\n", f"{app['description']}\n"]
    for context_name in app["contexts"]:
        context = model["contexts"][context_name]
        out.append(f"\n## Context `{context_name}`\n\n{context['description']}\n")
        for level in ("commands", "queries"):
            for name, declaration in sorted(context.get(level, {}).items()):
                out.append(f"\n### {level[:-1] if level == 'commands' else 'query'} `{context_name}.{name}`\n\n")
                out.append(f"{declaration.get('description', '(no description)')}\n\n")
                out.append(f"- status: **{declaration.get('status')}**\n")
                if declaration.get("status") == "not-built":
                    out.append(f"- reason: {declaration.get('reason', '(no reason)')}\n")
                out.append(f"- receives: {shape(declaration.get('payload', {}))}\n")
                if "result" in declaration:
                    out.append(f"- returns: {shape(declaration['result'])}\n")
                if "result_item" in declaration:
                    out.append(f"- returns a list, each row: {shape(declaration['result_item'])}\n")
                if declaration.get("emits"):
                    out.append("- emits: " + ", ".join(f"`{e}`" for e in declaration["emits"]) + "\n")
        for name, declaration in sorted(context.get("events", {}).items()):
            out.append(f"\n### event `{name}`\n\n{declaration.get('description', '(no description)')}\n\n")
            out.append(f"- carries: {shape(declaration.get('payload', {}))}\n")
    out.append("\n## What is declared and not built\n\n")
    absent = [(n, d) for n, _, d in model_reader.operations(model) if d.get("status") == "not-built"]
    for name, declaration in absent:
        out.append(f"- `{name}` — {declaration.get('reason', '(no reason)')}\n")
    if not absent:
        out.append("Nothing.\n")
    out.append("\n## Wirings\n\n| wiring | from | field | to | payload field |\n|---|---|---|---|---|\n")
    for wiring in app.get("wirings", []):
        out.append(
            f"| {wiring['name']} | `{wiring['from']}` | `{wiring['field']}` "
            f"| `{wiring['to']}` | `{wiring['payload_field']}` |\n"
        )
    return "".join(out)


def glossary(language: dict) -> str:
    out = [HEADER, "# Glossary\n\n| term | kind | definition | do not write |\n|---|---|---|---|\n"]
    for term, entry in sorted(language["terms"].items()):
        out.append(
            f"| **{term}** | {entry['kind']} | {entry['definition']} | {', '.join(entry['refuses'])} |\n"
        )
    return "".join(out)


def gates_page(gates: dict) -> str:
    out = [HEADER, "# What the build refuses\n\n| gate | kind | refuses | limit |\n|---|---|---|---|\n"]
    for name, gate in sorted(gates["gates"].items()):
        limit = ""
        if gate.get("kind") == "ratchet":
            limit = f"baseline `{gate.get('baseline')}`, it only shrinks"
        if gate.get("kind") == "report":
            limit = f"ceiling {gate.get('ceiling')}"
        out.append(f"| `{name}` | {gate.get('kind')} | {gate.get('refuses')} | {limit} |\n")
    return "".join(out)


def render(root: Path) -> dict:
    """Return {path relative to root: text} for every derived document."""
    model = model_reader.load(root)
    language = model_reader.read_toml(root / "model" / "language.toml")
    gates = model_reader.read_toml(root / "model" / "gates.toml")
    return {
        "docs/generated/reference.md": reference(model),
        "docs/generated/glossary.md": glossary(language),
        "docs/generated/gates.md": gates_page(gates),
    }


if __name__ == "__main__":
    for relative, text in render(ROOT).items():
        (ROOT / relative).parent.mkdir(parents=True, exist_ok=True)
        (ROOT / relative).write_text(text, encoding="utf-8")
        print(f"wrote {relative}")
