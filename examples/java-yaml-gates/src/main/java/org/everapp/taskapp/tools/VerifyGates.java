package org.everapp.taskapp.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.everapp.taskapp.Model;

/**
 * Prove that every gate FIRES.
 *
 *     mvn -q compile exec:java@verify-gates
 *
 * A gate that nobody has watched fail is not yet a rule: it can be green
 * because it looks at nothing. For each planted failure in model/gates.yaml
 * this class copies the project, puts the defect back, runs the gates on the
 * copy, and requires a failure of the SAME gate that holds the expected text.
 */
public final class VerifyGates {
    private VerifyGates() {}

    public record Outcome(int total, int silent, List<String> lines) {}

    private static void copy(Path from, Path to) throws IOException {
        try (Stream<Path> walk = Files.walk(from)) {
            for (Path source : walk.toList()) {
                Path relative = from.relativize(source);
                String first = relative.getNameCount() > 0 ? relative.getName(0).toString() : "";
                if (first.equals("target") || first.equals(".git")) {
                    continue; // build output is not part of the project
                }
                Path target = to.resolve(relative.toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target);
                }
            }
        }
    }

    private static void delete(Path directory) throws IOException {
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    public static Outcome run(Path root) throws IOException {
        root = root.toAbsolutePath().normalize();
        List<String> lines = new ArrayList<>();
        Gates.Report clean = Gates.run(root);
        if (!clean.failures().isEmpty()) {
            clean.failures().forEach(failure -> lines.add("FAIL " + failure));
            lines.add("The clean project must be green before a planted failure means anything.");
            return new Outcome(0, 1, lines);
        }
        Map<String, Object> gates = Model.map(Model.readYaml(root.resolve("model/gates.yaml")).get("gates"));
        int total = 0;
        int silent = 0;
        for (Map.Entry<String, Object> gate : new TreeMap<>(gates).entrySet()) {
            String name = gate.getKey();
            for (Object planted : Model.list(Model.map(gate.getValue()).get("planted"))) {
                Map<String, Object> defect = Model.map(planted);
                String file = Model.text(defect.get("file"));
                String find = Model.text(defect.get("find"));
                String expect = Model.text(defect.get("expect"));
                total++;
                Path temporary = Files.createTempDirectory("planted-");
                try {
                    Path project = temporary.resolve("project");
                    copy(root, project);
                    Path target = project.resolve(file);
                    String text = Files.readString(target, StandardCharsets.UTF_8);
                    int at = text.indexOf(find);
                    if (at < 0) {
                        lines.add("BROKEN " + name + ": the text to replace is not in " + file);
                        silent++;
                        continue;
                    }
                    Files.writeString(target, text.substring(0, at) + Model.text(defect.get("replace"))
                            + text.substring(at + find.length()), StandardCharsets.UTF_8);
                    List<String> failures = Gates.run(project).failures();
                    boolean fires = failures.stream()
                            .anyMatch(failure -> failure.startsWith("[" + name + "]") && failure.contains(expect));
                    if (fires) {
                        lines.add(String.format("fires  %-26s %s", name, expect.length() > 60 ? expect.substring(0, 60) : expect));
                    } else {
                        silent++;
                        lines.add("SILENT " + name + ": expected a failure with '" + expect + "', got:");
                        failures.forEach(failure -> lines.add("       " + failure));
                    }
                } finally {
                    delete(temporary);
                }
            }
        }
        lines.add("");
        lines.add(total + " planted failures, " + (total - silent) + " fire, " + silent + " do not");
        return new Outcome(total, silent, lines);
    }

    public static void main(String[] args) throws IOException {
        Outcome outcome = run(Path.of(args.length > 0 ? args[0] : ""));
        outcome.lines().forEach(System.out::println);
        if (outcome.silent() > 0) {
            System.exit(1);
        }
    }
}
