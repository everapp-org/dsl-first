#!/usr/bin/env python3
"""Parse the grammar of KERNEL_DSL.md, so the specification cannot drift from itself.

    pip install lark
    python3 tools/check_kernel_grammar.py

This is a gate on the specification (Gates Guide, "a rule is a program").
It reads the EBNF block of dsl_first_methodology/KERNEL_DSL.md, builds a parser
from it, and requires that:

  - every example in the specification parses. A fragment, such as one
    model or one service, is put inside a small domain first;
  - each VALID model below parses. One of them is a v1.1 model, so the claim
    "every v1.1 model is a valid v1.2 model" has a check;
  - each INVALID model below is REFUSED. These are the planted failures: a
    grammar that accepts everything would parse the valid models too.

It checks the grammar only. The well-formedness rules of section 6 (a status
has a reason, the two ends of a wiring agree) need a model reader, and the
runnable examples implement them as gates.

This tool is for people who change the specification. Nobody needs it to USE
the methodology.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

try:
    from lark import Lark
    from lark.exceptions import LarkError
except ImportError:
    sys.exit("This check needs the parser library: pip install lark")

SPEC = Path(__file__).resolve().parent.parent / "dsl_first_methodology" / "KERNEL_DSL.md"

VALID = {
    "a v1.1 model, unchanged": '''
        domain Billing {
            description "Invoices."
            level domain {
                model Invoice {
                    description "One invoice."
                    fields {
                        id: InvoiceId
                        lines: List<Line, Tax>
                    }
                    states { Sent Paid Error }
                    transitions {
                        Sent -> Paid on receivePayment(amount)
                            if amount > 0
                            emits InvoicePaid, LedgerUpdated
                            do updateLedger
                        Sent -> Error on receivePayment(amount)
                            if amount <= 0
                            else Sent
                    }
                    invariants { total >= 0 }
                }
                service Billing {
                    description "Operations."
                    operation send(invoiceId)
                    operation close
                }
            }
            constraints {
                forbid imports from package billing.ui.*
            }
        }''',
    "v1.2: typed parameters, a result, each status, a wiring": '''
        domain Tasks {
            level application {
                service TaskService {
                    operation listTasks returns many (task_id: Int, title: String)
                    operation createTask(title: String) returns (task_id: Int) status ready
                    operation completeTask(task_id: Int) returns (task_id: Int, done: Bool)
                        status in-progress
                    operation archiveTask(task_id: Int)
                        status not-built reason "No decision yet."
                    operation mixed(a, b: List<Int>, c: String[])
                }
            }
            wiring completeFromList {
                description "The list row gives the id."
                from TaskService.listTasks.task_id
                to   TaskService.completeTask.task_id
            }
            wiring second { from A.b.c to D.e.f }
        }''',
}

INVALID = {
    "a status outside the closed set":
        'domain D { level l { service S { operation a status planned } } }',
    "a reason with no status":
        'domain D { level l { service S { operation a reason "why" } } }',
    "returns with no fields":
        'domain D { level l { service S { operation a returns () } } }',
    "a wiring endpoint of two parts":
        'domain D { wiring w { from S.a to S.b.c } }',
    "a wiring with no `to`":
        'domain D { wiring w { from S.a.b } }',
    "a wiring inside a level":
        'domain D { level l { wiring w { from S.a.b to S.c.d } } }',
    "a parameter type with a space":
        'domain D { level l { service S { operation a(x: Big Int) } } }',
}


def grammar_of(text: str) -> str:
    block = re.search(r"```ebnf\n(.*?)```", text, re.S)
    if not block:
        sys.exit("KERNEL_DSL.md has no ```ebnf block")
    # The specification writes `name = body`, and continues a long rule on the
    # next line with an indent. The parser library writes `name: body` on one line.
    joined = re.sub(r"\n[ \t]+(?=\S)", " ", block.group(1))
    return re.sub(r"(?m)^([A-Za-z_]+)(\s*)=", r"\1\2:", joined)


WRAP = {
    "domain": "{}",
    "model": "domain D {{ level l {{ {} }} }}",
    "service": "domain D {{ level l {{ {} }} }}",
    "transitions": "domain D {{ level l {{ model M {{ {} }} }} }}",
    "constraints": "domain D {{ {} }}",
}


def models_in(text: str) -> dict:
    """Every example of the specification. A fragment (a model, a service, ...)
    is put inside the smallest domain that can hold it. A block that holds
    `...` is a sketch, not a model, and is not read."""
    found = {}
    for number, block in enumerate(re.findall(r"```kotlin\n(.*?)```", text, re.S), 1):
        first = block.split(None, 1)[0]
        if "..." in block or first not in WRAP:
            continue
        found[f"specification example {number} ({first})"] = WRAP[first].format(block)
    return found


def main() -> int:
    text = SPEC.read_text(encoding="utf-8")
    parser = Lark(grammar_of(text), start="start", parser="earley")
    from_spec = models_in(text)
    if len(from_spec) < 8:
        print(f"FAIL read {len(from_spec)} examples in the specification, and it must read at least 8")
        return 1
    bad = 0
    for name, model in {**from_spec, **VALID}.items():
        try:
            parser.parse(model)
            print(f"parses   {name}")
        except LarkError as error:
            bad += 1
            print(f"FAIL     {name} must parse: {str(error).splitlines()[0]}")
    for name, model in INVALID.items():
        try:
            parser.parse(model)
            bad += 1
            print(f"FAIL     {name} must be refused, and the grammar accepts it")
        except LarkError:
            print(f"refused  {name}")
    print(f"\n{len(from_spec) + len(VALID)} valid models, {len(INVALID)} invalid models, {bad} failure(s)")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
