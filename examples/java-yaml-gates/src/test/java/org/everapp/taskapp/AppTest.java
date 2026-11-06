package org.everapp.taskapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.everapp.taskapp.tools.Gates;
import org.everapp.taskapp.tools.VerifyGates;
import org.junit.jupiter.api.Test;

/**
 * Drive the application through the same seam its users use: the bus.
 * Maven runs the tests with the project directory as the working directory.
 */
class AppTest {
    private static final Path ROOT = Path.of("");

    // -- the application ------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void createListComplete() {
        Bus bus = Interpreter.start(ROOT);
        Map<String, Object> created = (Map<String, Object>) bus.dispatch("tasks.create", Map.of("title", "write the model"));
        List<Map<String, Object>> rows = (List<Map<String, Object>>) bus.dispatch("tasks.list");
        assertEquals(List.of(Map.of("task_id", 1, "title", "write the model", "done", false)), rows);
        // The declared wiring: the id of a list row is the payload of complete.
        Object done = bus.dispatch("tasks.complete", Map.of("task_id", rows.get(0).get("task_id")));
        assertEquals(Map.of("task_id", created.get("task_id"), "done", true), done);
        assertEquals(1, bus.state.get("completed_count"));
    }

    @Test
    void theAuditMiddlewareRecordsEachCommand() {
        Bus bus = Interpreter.start(ROOT);
        bus.dispatch("tasks.create", Map.of("title", "a"));
        assertEquals(List.of(Map.of("operation", "tasks.create"), Map.of("operation", "audit.entries")),
                bus.dispatch("audit.entries"));
    }

    @Test
    void anUndeclaredOperationIsRefusedByName() {
        Bus bus = Interpreter.start(ROOT);
        Bus.Refused refused = assertThrows(Bus.Refused.class, () -> bus.dispatch("tasks.delete", Map.of("task_id", 1)));
        assertEquals("'tasks.delete' is not a declared operation", refused.getMessage());
    }

    @Test
    void aWrongPayloadIsRefused() {
        Bus bus = Interpreter.start(ROOT);
        Bus.Refused refused = assertThrows(Bus.Refused.class, () -> bus.dispatch("tasks.complete", Map.of("task_id", "1")));
        assertTrue(refused.getMessage().contains("field 'task_id' must be int, got String"), refused.getMessage());
    }

    @Test
    void aNotBuiltOperationAnswersWithItsReason() {
        Bus bus = Interpreter.start(ROOT);
        Bus.NotBuilt notBuilt = assertThrows(Bus.NotBuilt.class, () -> bus.dispatch("tasks.archive", Map.of("task_id", 1)));
        assertTrue(notBuilt.getMessage().contains("No decision yet"), notBuilt.getMessage());
    }

    // -- conformance ----------------------------------------------------------

    /**
     * The model and the running application, compared in BOTH directions.
     * One side is read from the files. The other is read from the running bus.
     */
    @Test
    void theWiredOperationsEqualTheDeclaredBuiltOnes() {
        Set<String> declared = Model.load(ROOT).operations(List.of("commands", "queries")).stream()
                .filter(operation -> Model.BUILT.contains(operation.status()))
                .map(Model.Operation::name)
                .collect(Collectors.toSet());
        assertEquals(declared, Interpreter.start(ROOT).registered());
    }

    // -- the gates ------------------------------------------------------------

    @Test
    void everyGateHolds() {
        Gates.Report report = Gates.run(ROOT);
        assertEquals(List.of(), report.failures());
        assertEquals(10, report.lines().size());
    }

    @Test
    void everyGateFiresOnItsPlantedFailure() throws IOException {
        VerifyGates.Outcome outcome = VerifyGates.run(ROOT);
        assertEquals(0, outcome.silent(), String.join("\n", outcome.lines()));
        assertTrue(outcome.total() >= 10, "one planted failure or more for each gate");
    }
}
