"""Drive the application through the same seam its users use: the bus.

    python3 -m unittest discover -s tests -v
"""
import subprocess
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "src"))

from taskapp import interpreter, model as model_reader  # noqa: E402
from taskapp.bus import NotBuilt, Refused  # noqa: E402


class TheApplication(unittest.TestCase):
    def setUp(self):
        self.bus = interpreter.start(ROOT)

    def test_create_list_complete(self):
        created = self.bus.dispatch("tasks.create", {"title": "write the model"})
        rows = self.bus.dispatch("tasks.list")
        self.assertEqual([{"task_id": 1, "title": "write the model", "done": False}], rows)
        # The declared wiring: the id of a list row is the payload of complete.
        done = self.bus.dispatch("tasks.complete", {"task_id": rows[0]["task_id"]})
        self.assertEqual({"task_id": created["task_id"], "done": True}, done)
        self.assertEqual(1, self.bus.state["completed_count"])

    def test_the_audit_middleware_records_each_command(self):
        self.bus.dispatch("tasks.create", {"title": "a"})
        self.assertEqual([{"operation": "tasks.create"}, {"operation": "audit.entries"}],
                         self.bus.dispatch("audit.entries"))

    def test_an_undeclared_operation_is_refused_by_name(self):
        with self.assertRaisesRegex(Refused, "'tasks.delete' is not a declared operation"):
            self.bus.dispatch("tasks.delete", {"task_id": 1})

    def test_a_wrong_payload_is_refused(self):
        with self.assertRaisesRegex(Refused, "field 'task_id' must be int, got str"):
            self.bus.dispatch("tasks.complete", {"task_id": "1"})

    def test_a_not_built_operation_answers_with_its_reason(self):
        with self.assertRaisesRegex(NotBuilt, "No decision yet"):
            self.bus.dispatch("tasks.archive", {"task_id": 1})


class Conformance(unittest.TestCase):
    """The model and the running application, compared in BOTH directions.
    One side is read from the files. The other is read from the live bus."""

    def test_the_wired_operations_equal_the_declared_built_ones(self):
        model = model_reader.load(ROOT)
        declared = {name for name, _, declaration
                    in model_reader.operations(model, ("commands", "queries"))
                    if declaration["status"] in model_reader.BUILT}
        self.assertEqual(declared, interpreter.start(ROOT).registered())


class TheGates(unittest.TestCase):
    def run_tool(self, name):
        return subprocess.run([sys.executable, "-B", str(ROOT / "tools" / name)],
                              capture_output=True, text=True, cwd=ROOT)

    def test_every_gate_holds(self):
        done = self.run_tool("validate.py")
        self.assertEqual(0, done.returncode, done.stdout)

    def test_every_gate_fires_on_its_planted_failure(self):
        done = self.run_tool("verify_gates.py")
        self.assertEqual(0, done.returncode, done.stdout)


if __name__ == "__main__":
    unittest.main()
