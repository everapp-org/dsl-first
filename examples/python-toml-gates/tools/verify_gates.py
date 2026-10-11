#!/usr/bin/env python3
"""Prove that every gate FIRES.

    python3 tools/verify_gates.py

A gate that nobody has watched fail is not yet a rule: it can be green because
it looks at nothing. For each planted failure in model/gates.toml this script
copies the project, puts the defect back, runs the gates on the copy, and
requires exit 1 with a message that holds the expected text.
"""
from __future__ import annotations

import shutil
import subprocess
import sys
import tempfile
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
IGNORE = shutil.ignore_patterns("__pycache__", "*.pyc")


def validate(root: Path):
    done = subprocess.run([sys.executable, "-B", str(root / "tools" / "validate.py")],
                          capture_output=True, text=True, cwd=root)
    return done.returncode, done.stdout + done.stderr


def main() -> int:
    code, output = validate(ROOT)
    if code != 0:
        print(output)
        print("The clean project must be green before a planted failure means anything.")
        return 1
    with (ROOT / "model" / "gates.toml").open("rb") as handle:
        gates = tomllib.load(handle)["gates"]
    bad = 0
    total = 0
    for name, declaration in sorted(gates.items()):
        for case in declaration.get("planted", []):
            total += 1
            with tempfile.TemporaryDirectory() as temporary:
                copy = Path(temporary) / "project"
                shutil.copytree(ROOT, copy, ignore=IGNORE)
                target = copy / case["file"]
                text = target.read_text(encoding="utf-8")
                if case["find"] not in text:
                    print(f"BROKEN {name}: the text to replace is not in {case['file']}")
                    bad += 1
                    continue
                target.write_text(text.replace(case["find"], case["replace"], 1), encoding="utf-8")
                code, output = validate(copy)
            wanted = f"[{name}]"
            hit = [line for line in output.splitlines() if wanted in line and case["expect"] in line]
            if code == 1 and hit:
                print(f"fires  {name:<26} {case['expect'][:60]}")
            else:
                bad += 1
                print(f"SILENT {name}: expected exit 1 and '{case['expect']}', got exit {code}")
                print("       " + "\n       ".join(output.splitlines()[-6:]))
    print(f"\n{total} planted failures, {total - bad} fire, {bad} do not")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
