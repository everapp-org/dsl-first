#!/usr/bin/env python3
"""Parse and validate the EBNF grammars of all five DSL-First specifications.

    pip install lark
    python3 tools/check_all_grammars.py

Optional: validate real-world project `.dsl` files against the official spec
grammars via repeatable `--check-file <dsl>:<path>` flags:

    python3 tools/check_all_grammars.py \\
        --check-file kernel:path/to/kernel.dsl \\
        --check-file behavior:path/to/behavior.dsl \\
        --check-file verification:path/to/verification.dsl \\
        --check-file catalog:path/to/catalog.dsl \\
        --check-file target:path/to/target.dsl

This is a gate on the five methodology specifications:
  - dsl_first_methodology/KERNEL_DSL.md
  - dsl_first_methodology/BEHAVIOR_DSL.md
  - dsl_first_methodology/VERIFICATION_DSL.md
  - dsl_first_methodology/CATALOG_DSL.md
  - dsl_first_methodology/TARGET_DSL.md

For each specification it extracts the ```ebnf``` block, builds a Lark Earley
parser, and requires that:
  - every non-sketch code example in the specification parses;
  - every curated VALID model parses;
  - every curated INVALID model (planted failure) is refused.
"""
from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path

try:
    from lark import Lark
    from lark.exceptions import LarkError
except ImportError:
    sys.exit("This check needs the parser library: pip install lark")

SPEC_DIR = Path(__file__).resolve().parent.parent / "dsl_first_methodology"


@dataclass(frozen=True)
class SpecSuite:
    key: str
    filename: str
    example_fences: tuple[str, ...]
    min_examples: int
    wrap: dict[str, str]
    valid: dict[str, str]
    invalid: dict[str, str]


SUITES: tuple[SpecSuite, ...] = (
    SpecSuite(
        key="kernel",
        filename="KERNEL_DSL.md",
        example_fences=("kotlin", "dsl"),
        min_examples=8,
        wrap={
            "domain": "{}",
            "model": "domain D {{ level l {{ {} }} }}",
            "service": "domain D {{ level l {{ {} }} }}",
            "transitions": "domain D {{ level l {{ model M {{ {} }} }} }}",
            "constraints": "domain D {{ {} }}",
            "wiring": "domain D {{ {} }}",
        },
        valid={
            "v1.2 with // comments and T? optional types": """
                // Kernel model with comments and optional type shorthand
                domain Orders {
                    level domain {
                        model Order {
                            fields {
                                id: OrderId // primary key
                                coupon: CouponCode?
                                notes: Optional<String>
                            }
                            states { Draft, Paid }
                            transitions {
                                Draft -> Paid on pay(amount)
                                    if amount > 0
                                    emits OrderPaid
                            }
                        }
                    }
                    level application {
                        service OrderService {
                            operation createOrder(coupon: CouponCode?) returns (order_id: OrderId) status ready
                            operation refundOrder(order_id: OrderId)
                                status not-built reason "Refund workflow deferred."
                        }
                    }
                    wiring refundCreated {
                        from OrderService.createOrder.order_id
                        to   OrderService.refundOrder.order_id
                    }
                }""",
        },
        invalid={
            "status outside closed set":
                "domain D { level l { service S { operation a status planned } } }",
            "status before returns":
                "domain D { level l { service S { operation a status ready returns (x: Int) } } }",
            "parameter type with space":
                "domain D { level l { service S { operation a(x: Big Int) } } }",
            "wiring inside level":
                "domain D { level l { wiring w { from S.a.b to S.c.d } } }",
        },
    ),
    SpecSuite(
        key="behavior",
        filename="BEHAVIOR_DSL.md",
        example_fences=("dsl",),
        min_examples=2,
        wrap={"behavior": "{}"},
        valid={
            "pipeline with when guard, parallelForEach, and // comments": """
                // Procedure with conditional guard and concurrency
                behavior BatchIndex {
                    description "Index items conditionally."
                    participants {
                        coordinator: required
                        workers: 1..*
                    }
                    execution {
                        items = loadItems(source)
                        when dryRunEnabled {
                            preview(items)
                        }
                        parallelForEach item in items {
                            indexItem(item)
                        }
                    }
                    errorHandling {
                        onStepFailure: SKIP_AND_CONTINUE
                    }
                    concurrency {
                        maxParallel: workers.count()
                    }
                }""",
        },
        invalid={
            "empty execution block":
                "behavior Empty { execution { } }",
            "invalid multiplicity missing upper bound":
                "behavior Bad { participants { w: 1.. } execution { run() } }",
        },
    ),
    SpecSuite(
        key="verification",
        filename="VERIFICATION_DSL.md",
        example_fences=("dsl",),
        min_examples=2,
        wrap={
            "verification": "{}",
            "check": "verification V {{ {} }}",
        },
        valid={
            "decimal literals for latency/ratio/score alongside integer minor units": """
                // Decimal literals for latency, ratio, and score assertions
                verification MetricsAndLatency {
                    description "Check latency and confidence thresholds."

                    check LatencyWithinThreshold {
                        given  probe  = configureProbe(0.25)
                        when   result = measureLatency(probe, 1.5)
                        expect result.p95Seconds == 0.42
                               result.score == 0.985
                               result.priceCents == 125
                               result.samples[0].ok == true
                    }

                    check NestedCallArgument {
                        given  session = openSession("default")
                        when   reply   = dispatch(session, setBudget(100))
                        expect reply.accepted == true
                    }
                }""",
        },
        invalid={
            "missing expect block":
                "verification V { check C { when r = run() } }",
            "decimal index inside array path":
                "verification V { check C { when r = run() expect r.items[1.5] == true } }",
            "control flow inside check":
                "verification V { check C { foreach x in xs { run(x) } when r = done() expect r.ok == true } }",
        },
    ),
    SpecSuite(
        key="catalog",
        filename="CATALOG_DSL.md",
        example_fences=("dsl",),
        min_examples=1,
        wrap={
            "catalog": "{}",
            "bind": "catalog C {{ {} }}",
            "format": "catalog C {{ {} }}",
        },
        valid={
            "catalog with bind, format, nested entries, and resolve": """
                // Full catalog feature exercise
                catalog SuiteConfig {
                    description "Environment bindings and screenshot name format."

                    target linux_chrome "Linux Chrome" {
                        os "Linux"
                        headless true
                        retries 2
                    }

                    bind environment on tag {
                        "@qa_env"  -> env qa, version QA
                        "@uat_env" -> env uat, version UAT
                    }

                    format ScreenshotName {
                        pattern "<tcid>_<runid>_<stepid>.PNG"
                        pattern-baseline "<tcid>_<stepid>_Expected.PNG"
                    }

                    resolve exact, below, fallback en
                }""",
        },
        invalid={
            "empty catalog block":
                "catalog Empty { }",
            "bind row missing arrow":
                'catalog C { bind env on tag { "@qa" env qa } }',
        },
    ),
    SpecSuite(
        key="target",
        filename="TARGET_DSL.md",
        example_fences=("dsl",),
        min_examples=2,
        wrap={"target": "{}"},
        valid={
            "rust target profile with CONCEPT-rust-v1 realizations": """
                // Rust target profile sanity check
                target rust_stable {
                    description "Idiomatic Rust + Cargo Test"
                    language    rust "1.96"
                    runner      cargo_test "1.96"
                    driver      http "std"
                    catalogs    prog, rust, qa
                    tooling {
                        linter    clippy "-D warnings"
                        formatter rustfmt "--check"
                    }
                    constructs {
                        realize CONCEPT-prog-v1-001-01-immutable-data-carrier with CONCEPT-rust-v1-001-01-derive-structs
                        prefer  CONCEPT-rust-v1-009-01-thiserror-domain-errors
                        avoid   CONCEPT-rust-v1-030-01-unwrap-in-library-code
                    }
                    budget {
                        max_method_lines 50
                    }
                }""",
        },
        invalid={
            "malformed concept id":
                'target bad { language rust "1.96" constructs { prefer not-a-concept-id } }',
            "realize missing with keyword":
                'target bad { language rust "1.96" constructs { realize CONCEPT-prog-v1-001-01-a CONCEPT-rust-v1-001-01-b } }',
            "non-integer budget value":
                'target bad { language rust "1.96" budget { max_method_lines "fifty" } }',
        },
    ),
)


def ebnf_to_lark(text: str, spec_name: str) -> str:
    """Extract the first ```ebnf``` block from a spec and adapt it to Lark syntax."""
    block = re.search(r"```ebnf\n(.*?)```", text, re.S)
    if not block:
        raise ValueError(f"{spec_name} has no ```ebnf block")

    cleaned_lines: list[str] = []
    for line in block.group(1).splitlines():
        # Strip standard ISO EBNF (* ... *) comments.
        line = re.sub(r"\(\*.*?\*\)", "", line)
        # Strip inline // comments in EBNF prose, preserving the COMMENT rule itself.
        if not re.match(r"^\s*COMMENT\s*[:=]", line):
            line = re.sub(r"(?<!['\"/])//.*$", "", line)
        cleaned_lines.append(line.rstrip())

    cleaned = "\n".join(cleaned_lines)
    # Join indented continuation lines onto the preceding rule.
    joined = re.sub(r"\n[ \t]+(?=\S)", " ", cleaned)
    # Convert `rule = body` to Lark's `rule: body`.
    return re.sub(r"(?m)^([A-Za-z_]+)(\s*)=", r"\1\2:", joined)


def examples_in(text: str, suite: SpecSuite) -> dict[str, str | None]:
    """Collect all non-sketch fenced examples from a specification."""
    found: dict[str, str | None] = {}
    fence_pattern = "|".join(re.escape(f) for f in suite.example_fences)
    pattern = re.compile(rf"```(?:{fence_pattern})\n(.*?)```", re.S)
    for number, block in enumerate(pattern.findall(text), 1):
        if "..." in block:
            continue
        # Strip leading // comment lines to find the first keyword.
        non_comment = [
            ln.strip()
            for ln in block.splitlines()
            if ln.strip() and not ln.strip().startswith("//")
        ]
        if not non_comment:
            continue
        first = non_comment[0].split(None, 1)[0]
        if first not in suite.wrap:
            found[f"spec example {number} ({first})"] = None
            continue
        found[f"spec example {number} ({first})"] = suite.wrap[first].format(block)
    return found


def check_suite(suite: SpecSuite) -> tuple[Lark | None, int, int, int]:
    """Validate one specification; returns (parser, valid_count, invalid_count, failures)."""
    spec_path = SPEC_DIR / suite.filename
    text = spec_path.read_text(encoding="utf-8")
    print(f"=== {suite.filename} ({suite.key}) ===")
    try:
        lark_src = ebnf_to_lark(text, suite.filename)
        parser = Lark(lark_src, start="start", parser="earley")
    except Exception as error:  # noqa: BLE001
        print(f"FAIL     could not compile grammar: {error}")
        return None, 0, 0, 1

    from_spec = examples_in(text, suite)
    bad = 0
    if len(from_spec) < suite.min_examples:
        bad += 1
        print(
            f"FAIL     read {len(from_spec)} example(s) in {suite.filename}, "
            f"expected at least {suite.min_examples}"
        )

    for name, sample in {**from_spec, **suite.valid}.items():
        if sample is None:
            bad += 1
            print(f"FAIL     {name} starts with an unknown keyword; update wrap map")
            continue
        try:
            parser.parse(sample)
            print(f"parses   {name}")
        except LarkError as error:
            bad += 1
            print(f"FAIL     {name} must parse: {str(error).splitlines()[0]}")

    for name, sample in suite.invalid.items():
        try:
            parser.parse(sample)
            bad += 1
            print(f"FAIL     {name} must be refused, and the grammar accepts it")
        except LarkError:
            print(f"refused  {name}")

    valid_total = len(from_spec) + len(suite.valid)
    invalid_total = len(suite.invalid)
    return parser, valid_total, invalid_total, bad


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument(
        "--check-file",
        action="append",
        default=[],
        metavar="DSL:PATH",
        help="Validate an external .dsl file against one of the five spec grammars "
             "(DSL is one of: kernel, behavior, verification, catalog, target).",
    )
    args = ap.parse_args(argv)

    parsers: dict[str, Lark] = {}
    total_valid = 0
    total_invalid = 0
    total_failures = 0

    for suite in SUITES:
        parser, v_cnt, i_cnt, failures = check_suite(suite)
        if parser is not None:
            parsers[suite.key] = parser
        total_valid += v_cnt
        total_invalid += i_cnt
        total_failures += failures
        print()

    if args.check_file:
        print("=== External --check-file checks ===")
        for item in args.check_file:
            if ":" not in item:
                total_failures += 1
                print(f"FAIL     invalid --check-file '{item}' (expected DSL:PATH)")
                continue
            dsl_key, raw_path = item.split(":", 1)
            if dsl_key not in parsers:
                total_failures += 1
                print(
                    f"FAIL     unknown DSL '{dsl_key}' for {raw_path} "
                    f"(expected one of: {', '.join(sorted(parsers))})"
                )
                continue
            file_path = Path(raw_path)
            if not file_path.is_file():
                total_failures += 1
                print(f"FAIL     file not found: {file_path}")
                continue
            try:
                parsers[dsl_key].parse(file_path.read_text(encoding="utf-8"))
                total_valid += 1
                print(f"parses   [{dsl_key}] {file_path}")
            except LarkError as error:
                total_failures += 1
                print(f"FAIL     [{dsl_key}] {file_path}: {str(error).splitlines()[0]}")
        print()

    print(
        f"Summary: {len(SUITES)} specs checked, "
        f"{total_valid} valid models, {total_invalid} invalid models, "
        f"{total_failures} failure(s)"
    )
    return 1 if total_failures else 0


if __name__ == "__main__":
    sys.exit(main())
