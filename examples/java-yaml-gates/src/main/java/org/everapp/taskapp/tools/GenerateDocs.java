package org.everapp.taskapp.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.everapp.taskapp.Model;

/**
 * Derive the documentation from the model.
 *
 *     mvn -q compile exec:java@generate-docs      # write docs/generated/
 *
 * The files in docs/generated/ are never edited by hand. The gate `docs-fresh`
 * derives them again and fails the build when a committed file differs.
 *
 * Every key is read through the readers of Model, which give an empty value
 * for a missing key. A model with a missing key must reach the gate that names
 * the defect. It must not stop here with an exception.
 */
public final class GenerateDocs {
    private GenerateDocs() {}

    static final String HEADER = "<!-- GENERATED from model/ by tools/GenerateDocs.java. Do not edit. -->\n\n";

    private static String or(Object value, String fallback) {
        String text = Model.text(value);
        return text.isEmpty() ? fallback : text;
    }

    private static String shape(Object fields) {
        Map<String, Object> declared = Model.map(fields);
        if (declared.isEmpty()) {
            return "nothing";
        }
        return declared.entrySet().stream()
                .map(field -> "`" + field.getKey() + "`: " + field.getValue())
                .collect(Collectors.joining(", "));
    }

    static String reference(Model model) {
        StringBuilder out = new StringBuilder(HEADER);
        out.append("# ").append(Model.text(model.app.get("application"))).append(" — reference\n\n");
        out.append(Model.text(model.app.get("description"))).append("\n");
        for (String contextName : Model.strings(model.app.get("contexts"))) {
            Map<String, Object> context = model.contexts.getOrDefault(contextName, Map.of());
            out.append("\n## Context `").append(contextName).append("`\n\n")
                    .append(Model.text(context.get("description"))).append("\n");
            for (String level : List.of("commands", "queries")) {
                String word = level.equals("commands") ? "command" : "query";
                new TreeMap<>(Model.map(context.get(level))).forEach((name, value) -> {
                    Map<String, Object> declaration = Model.map(value);
                    String status = Model.text(declaration.get("status"));
                    out.append("\n### ").append(word).append(" `").append(contextName).append('.').append(name).append("`\n\n");
                    out.append(or(declaration.get("description"), "(no description)")).append("\n\n");
                    out.append("- status: **").append(status).append("**\n");
                    if (status.equals("not-built")) {
                        out.append("- reason: ").append(or(declaration.get("reason"), "(no reason)")).append("\n");
                    }
                    out.append("- receives: ").append(shape(declaration.get("payload"))).append("\n");
                    if (declaration.containsKey("result")) {
                        out.append("- returns: ").append(shape(declaration.get("result"))).append("\n");
                    }
                    if (declaration.containsKey("result_item")) {
                        out.append("- returns a list, each row: ").append(shape(declaration.get("result_item"))).append("\n");
                    }
                    List<String> emits = Model.strings(declaration.get("emits"));
                    if (!emits.isEmpty()) {
                        out.append("- emits: ")
                                .append(emits.stream().map(e -> "`" + e + "`").collect(Collectors.joining(", ")))
                                .append("\n");
                    }
                });
            }
            new TreeMap<>(Model.map(context.get("events"))).forEach((name, value) -> {
                Map<String, Object> declaration = Model.map(value);
                out.append("\n### event `").append(name).append("`\n\n")
                        .append(or(declaration.get("description"), "(no description)")).append("\n\n");
                out.append("- carries: ").append(shape(declaration.get("payload"))).append("\n");
            });
        }
        out.append("\n## What is declared and not built\n\n");
        List<Model.Operation> absent = model.operations().stream()
                .filter(operation -> operation.status().equals("not-built")).toList();
        for (Model.Operation operation : absent) {
            out.append("- `").append(operation.name()).append("` — ")
                    .append(or(operation.declaration().get("reason"), "(no reason)")).append("\n");
        }
        if (absent.isEmpty()) {
            out.append("Nothing.\n");
        }
        out.append("\n## Wirings\n\n| wiring | from | field | to | payload field |\n|---|---|---|---|---|\n");
        for (Map<String, Object> wiring : model.wirings()) {
            out.append("| ").append(Model.text(wiring.get("name")))
                    .append(" | `").append(Model.text(wiring.get("from")))
                    .append("` | `").append(Model.text(wiring.get("field")))
                    .append("` | `").append(Model.text(wiring.get("to")))
                    .append("` | `").append(Model.text(wiring.get("payload_field"))).append("` |\n");
        }
        return out.toString();
    }

    static String glossary(Map<String, Object> language) {
        StringBuilder out = new StringBuilder(HEADER);
        out.append("# Glossary\n\n| term | kind | definition | do not write |\n|---|---|---|---|\n");
        new TreeMap<>(Model.map(language.get("terms"))).forEach((term, value) -> {
            Map<String, Object> entry = Model.map(value);
            out.append("| **").append(term).append("** | ").append(Model.text(entry.get("kind")))
                    .append(" | ").append(Model.text(entry.get("definition")))
                    .append(" | ").append(String.join(", ", Model.strings(entry.get("refuses")))).append(" |\n");
        });
        return out.toString();
    }

    static String gatesPage(Map<String, Object> gates) {
        StringBuilder out = new StringBuilder(HEADER);
        out.append("# What the build refuses\n\n| gate | kind | refuses | limit |\n|---|---|---|---|\n");
        new TreeMap<>(Model.map(gates.get("gates"))).forEach((name, value) -> {
            Map<String, Object> gate = Model.map(value);
            String kind = Model.text(gate.get("kind"));
            String limit = "";
            if (kind.equals("ratchet")) {
                limit = "baseline `" + Model.text(gate.get("baseline")) + "`, it only shrinks";
            }
            if (kind.equals("report")) {
                limit = "ceiling " + Model.text(gate.get("ceiling"));
            }
            out.append("| `").append(name).append("` | ").append(kind).append(" | ")
                    .append(Model.text(gate.get("refuses"))).append(" | ").append(limit).append(" |\n");
        });
        return out.toString();
    }

    /** Return {path relative to root: text} for every derived document. */
    public static Map<String, String> render(Path root) {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("docs/generated/reference.md", reference(Model.load(root)));
        out.put("docs/generated/glossary.md", glossary(Model.readYaml(root.resolve("model/language.yaml"))));
        out.put("docs/generated/gates.md", gatesPage(Model.readYaml(root.resolve("model/gates.yaml"))));
        return out;
    }

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : "");
        for (Map.Entry<String, String> document : render(root).entrySet()) {
            Path path = root.resolve(document.getKey());
            Files.createDirectories(path.getParent());
            Files.writeString(path, document.getValue(), StandardCharsets.UTF_8);
            System.out.println("wrote " + document.getKey());
        }
    }
}
