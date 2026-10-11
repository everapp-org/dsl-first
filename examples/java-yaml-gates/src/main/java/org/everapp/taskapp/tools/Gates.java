package org.everapp.taskapp.tools;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import org.everapp.taskapp.Interpreter;
import org.everapp.taskapp.Model;

/**
 * The gates: every rule of this project, as a program.
 *
 *     mvn -q compile exec:java@gates     # exit 0 when every gate holds, 1 otherwise
 *
 * Each gate is DECLARED in model/gates.yaml (what it refuses, its kind, the
 * population it must read, a planted failure) and IMPLEMENTED here as one
 * method. The gate `gates-declared` compares the two lists in both directions.
 *
 * A gate method returns a Result:
 *   read   how many things the gate looked at. Fewer than `reads_at_least`
 *          is a failure: a gate that looks at nothing can never be red.
 *   items  one (key, message) per thing the gate refuses.
 *
 * What the runner does with the items depends on the declared kind:
 *   hard     any item is a failure.
 *   ratchet  an item absent from the baseline file is a failure (new debt).
 *            A baseline line that no longer fails is ALSO a failure (stale).
 *            So the baseline only shrinks.
 *   report   the count of items must not exceed the declared ceiling.
 */
public final class Gates {
    private Gates() {}

    public record Item(String key, String message) {}

    public record Result(int read, List<Item> items) {}

    public record Report(List<String> lines, List<String> failures) {}

    @FunctionalInterface
    interface Gate {
        Result check(Context ctx) throws Exception;
    }

    static final Set<String> KINDS = Set.of("hard", "ratchet", "report");
    static final Set<String> REGISTRATION = Set.of("registerHandler", "registerSubscriber", "registerMiddleware");
    static final String SOURCES = "src/main/java";
    static final String LEGAL_REGISTRATION_SITE = SOURCES + "/org/everapp/taskapp/Interpreter.java";

    static final Map<String, Gate> GATES = new TreeMap<>();

    static {
        GATES.put("handler-resolves", Gates::handlerResolves);
        GATES.put("single-registration-site", Gates::singleRegistrationSite);
        GATES.put("event-declared", Gates::eventDeclared);
        GATES.put("wiring-typed", Gates::wiringTyped);
        GATES.put("not-built-stated", Gates::notBuiltStated);
        GATES.put("vocabulary", Gates::vocabulary);
        GATES.put("described", Gates::described);
        GATES.put("not-built-count", Gates::notBuiltCount);
        GATES.put("docs-fresh", Gates::docsFresh);
        GATES.put("gates-declared", Gates::gatesDeclared);
    }

    record Context(Path root, Model model, Map<String, Object> language, Map<String, Object> gates) {
        static Context read(Path root) {
            return new Context(root, Model.load(root),
                    Model.readYaml(root.resolve("model/language.yaml")),
                    Model.map(Model.readYaml(root.resolve("model/gates.yaml")).get("gates")));
        }
    }

    // ------------------------------------------------------------------------
    // The gates
    // ------------------------------------------------------------------------

    /**
     * Model side: the pointer in the file. Code side: the class loader.
     * The two sides have different origins, so this check cannot be a tautology.
     * The lookup is written again here, and not shared with the interpreter, so
     * the gate and the code it checks cannot share one defect. Only the table of
     * shapes is shared. A method with the correct name and the wrong parameters
     * is refused: a check of the name only would let it through to run time.
     */
    static Result handlerResolves(Context ctx) {
        int read = 0;
        List<Item> items = new ArrayList<>();
        for (Model.Operation operation : ctx.model().operations()) {
            if (!Model.BUILT.contains(operation.status())) {
                continue;
            }
            read++;
            String pointer = Model.text(operation.declaration().get("handled_by"));
            Class<?>[] shape = Interpreter.shape(operation.level());
            boolean found = false;
            String[] parts = pointer.split("#", 2);
            if (parts.length == 2) {
                try {
                    Method method = Class.forName(parts[0]).getMethod(parts[1], shape);
                    found = Modifier.isStatic(method.getModifiers());
                } catch (ReflectiveOperationException | LinkageError e) {
                    found = false;
                }
            }
            if (!found) {
                List<String> names = Stream.of(shape).map(Class::getSimpleName).toList();
                items.add(new Item(operation.name(), operation.name() + ": handled_by '" + pointer
                        + "' is not a public static method that takes (" + String.join(", ", names)
                        + "). Write the method, or correct the pointer."));
            }
        }
        return new Result(read, items);
    }

    /**
     * Reads the SOURCE with the parser of the JDK compiler. It does not compile,
     * so it needs no class path, and a comment or a string that holds the name of
     * a registration method is not a call.
     */
    static Result singleRegistrationSite(Context ctx) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(ctx.root().resolve(SOURCES))) {
            files = walk.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
        List<Item> items = new ArrayList<>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new Result(0, List.of(new Item("jdk", "No Java compiler is available. Run the gates on a JDK.")));
        }
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            for (Path path : files) {
                String relative = ctx.root().relativize(path).toString().replace('\\', '/');
                if (relative.equals(LEGAL_REGISTRATION_SITE)) {
                    continue;
                }
                JavacTask task = (JavacTask) compiler.getTask(
                        null, manager, diagnostic -> {}, null, null, manager.getJavaFileObjects(path));
                SourcePositions positions = Trees.instance(task).getSourcePositions();
                for (CompilationUnitTree unit : task.parse()) {
                    new TreeScanner<Void, Void>() {
                        void found(Tree node, String name) {
                            if (REGISTRATION.contains(name)) {
                                long line = unit.getLineMap().getLineNumber(positions.getStartPosition(unit, node));
                                String where = relative + ":" + line;
                                items.add(new Item(where, where + " calls " + name
                                        + ". Declare the operation in model/contexts/ and let the interpreter wire it."));
                            }
                        }

                        @Override
                        public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                            ExpressionTree select = node.getMethodSelect();
                            if (select instanceof MemberSelectTree member) {
                                found(node, member.getIdentifier().toString());
                            } else if (select instanceof IdentifierTree identifier) {
                                found(node, identifier.getName().toString());
                            }
                            return super.visitMethodInvocation(node, unused);
                        }

                        @Override
                        public Void visitMemberReference(MemberReferenceTree node, Void unused) {
                            found(node, node.getName().toString());
                            return super.visitMemberReference(node, unused);
                        }
                    }.scan(unit, null);
                }
            }
        }
        return new Result(files.size(), items);
    }

    static Result eventDeclared(Context ctx) {
        Set<String> declared = ctx.model().events().keySet();
        int read = 0;
        List<Item> items = new ArrayList<>();
        for (Model.Operation operation : ctx.model().operations()) {
            List<String> named = new ArrayList<>(Model.strings(operation.declaration().get("emits")));
            if (operation.level().equals("subscribers")) {
                named.add(Model.text(operation.declaration().get("pattern")));
            }
            for (String event : named) {
                read++;
                if (!declared.contains(event)) {
                    items.add(new Item(operation.name() + "->" + event, operation.name() + " names the event '"
                            + event + "', and no context declares it. Add it under `events:` with its payload."));
                }
            }
        }
        return new Result(read, items);
    }

    static Result wiringTyped(Context ctx) {
        Map<String, Map<String, Object>> catalogue = new HashMap<>();
        for (Model.Operation operation : ctx.model().operations(List.of("commands", "queries"))) {
            catalogue.put(operation.name(), operation.declaration());
        }
        int read = 0;
        List<Item> items = new ArrayList<>();
        for (Map<String, Object> wiring : ctx.model().wirings()) {
            read++;
            String label = Model.text(wiring.get("name"));
            String from = Model.text(wiring.get("from"));
            String to = Model.text(wiring.get("to"));
            Map<String, Object> source = catalogue.get(from);
            Map<String, Object> target = catalogue.get(to);
            if (source == null || target == null) {
                items.add(new Item(label, "wiring " + label + ": '" + from + "' and '" + to
                        + "' must both be declared operations."));
                continue;
            }
            Object returned = source.containsKey("result_item") ? source.get("result_item") : source.get("result");
            if (returned == null) {
                items.add(new Item(label, "wiring " + label + ": '" + from
                        + "' declares no result. A source must say what it returns."));
                continue;
            }
            Object gives = Model.map(returned).get(Model.text(wiring.get("field")));
            Object takes = Model.map(target.get("payload")).get(Model.text(wiring.get("payload_field")));
            if (gives == null || takes == null || !gives.equals(takes)) {
                items.add(new Item(label, "wiring " + label + ": " + from + "." + wiring.get("field") + " is " + gives
                        + ", and " + to + "." + wiring.get("payload_field") + " is " + takes
                        + ". Make the two declarations agree."));
            }
        }
        return new Result(read, items);
    }

    static Result notBuiltStated(Context ctx) {
        int read = 0;
        List<Item> items = new ArrayList<>();
        for (Model.Operation operation : ctx.model().operations()) {
            read++;
            String status = operation.status();
            Map<String, Object> declaration = operation.declaration();
            if (!Model.STATUSES.contains(status)) {
                items.add(new Item(operation.name(), operation.name() + ": status '" + status + "' is not one of "
                        + new TreeSet<>(Model.STATUSES) + "."));
            } else if (status.equals("not-built")
                    && (Model.text(declaration.get("reason")).isEmpty() || declaration.containsKey("handled_by"))) {
                items.add(new Item(operation.name(), operation.name()
                        + ": a not-built operation gives a reason and names no handler."));
            }
        }
        return new Result(read, items);
    }

    /** Each identifier of the model, with the place where it is written. */
    private static List<String[]> identifiers(Context ctx) {
        List<String[]> out = new ArrayList<>();
        List<String> levels = new ArrayList<>(Model.LEVELS);
        levels.add("events");
        ctx.model().contexts.forEach((contextName, context) -> {
            out.add(new String[] {contextName, "context " + contextName});
            for (String level : levels) {
                Model.map(context.get(level)).forEach((name, declaration) -> {
                    String where = contextName + "." + name;
                    out.add(new String[] {name, where});
                    for (String key : List.of("payload", "result", "result_item")) {
                        for (String field : Model.map(Model.map(declaration).get(key)).keySet()) {
                            out.add(new String[] {field, where + " field " + field});
                        }
                    }
                });
            }
        });
        for (Map<String, Object> wiring : ctx.model().wirings()) {
            String name = Model.text(wiring.get("name"));
            out.add(new String[] {name, "wiring " + name});
        }
        return out;
    }

    static Result vocabulary(Context ctx) {
        Map<String, String> approved = new HashMap<>();
        Model.map(ctx.language().get("terms")).forEach((term, entry) ->
                Model.strings(Model.map(entry).get("refuses")).forEach(refused -> approved.put(refused, term)));
        int read = 0;
        List<Item> items = new ArrayList<>();
        for (String[] identifier : identifiers(ctx)) {
            read++;
            for (String part : identifier[0].split("[._-]")) {
                if (approved.containsKey(part)) {
                    items.add(new Item(identifier[1], identifier[1] + ": the word '" + part + "' is refused. Write '"
                            + approved.get(part) + "' (model/language.yaml)."));
                }
            }
        }
        return new Result(read, items);
    }

    static Result described(Context ctx) {
        Map<String, Map<String, Object>> named = new TreeMap<>();
        ctx.model().operations().forEach(operation -> named.put(operation.name(), operation.declaration()));
        named.putAll(ctx.model().events());
        List<Item> items = new ArrayList<>();
        named.forEach((name, declaration) -> {
            if (Model.text(declaration.get("description")).isBlank()) {
                items.add(new Item(name, name + " has no description."));
            }
        });
        return new Result(named.size(), items);
    }

    static Result notBuiltCount(Context ctx) {
        List<Model.Operation> all = ctx.model().operations();
        List<Item> items = all.stream()
                .filter(operation -> operation.status().equals("not-built"))
                .map(operation -> new Item(operation.name(), operation.name() + " is declared and not built."))
                .toList();
        return new Result(all.size(), items);
    }

    static Result docsFresh(Context ctx) throws IOException {
        int read = 0;
        List<Item> items = new ArrayList<>();
        for (Map.Entry<String, String> document : GenerateDocs.render(ctx.root()).entrySet()) {
            read++;
            Path path = ctx.root().resolve(document.getKey());
            if (!Files.exists(path) || !Files.readString(path, StandardCharsets.UTF_8).equals(document.getValue())) {
                items.add(new Item(document.getKey(), document.getKey()
                        + " differs from the model. Run: mvn -q compile exec:java@generate-docs"));
            }
        }
        return new Result(read, items);
    }

    /**
     * The rules about the rules. Declared gates and implemented gates must be
     * the same set, and each declaration must be complete.
     */
    static Result gatesDeclared(Context ctx) {
        Set<String> declared = new TreeSet<>(ctx.gates().keySet());
        Set<String> implemented = GATES.keySet();
        List<Item> items = new ArrayList<>();
        for (String name : declared) {
            if (!implemented.contains(name)) {
                items.add(new Item(name, "gate '" + name
                        + "' is declared in model/gates.yaml and no method implements it."));
            }
        }
        for (String name : implemented) {
            if (!declared.contains(name)) {
                items.add(new Item(name, "gate '" + name
                        + "' is implemented and model/gates.yaml does not declare it."));
            }
        }
        for (String name : declared) {
            Map<String, Object> declaration = Model.map(ctx.gates().get(name));
            String kind = Model.text(declaration.get("kind"));
            if (!KINDS.contains(kind)) {
                items.add(new Item(name, "gate '" + name + "': kind '" + kind + "' is not one of "
                        + new TreeSet<>(KINDS) + "."));
            }
            if (kind.equals("ratchet") && !declaration.containsKey("baseline")) {
                items.add(new Item(name, "gate '" + name + "': a ratchet names its baseline file."));
            }
            if (kind.equals("report") && !(declaration.get("ceiling") instanceof Integer)) {
                items.add(new Item(name, "gate '" + name
                        + "': a report with no ceiling can grow with no failure. Give it a ceiling."));
            }
            if (Model.list(declaration.get("planted")).isEmpty()) {
                items.add(new Item(name, "gate '" + name
                        + "': no planted failure. A gate that nobody saw fail is not yet a rule."));
            }
            if (!(declaration.get("reads_at_least") instanceof Integer)
                    || Model.text(declaration.get("refuses")).isEmpty()) {
                items.add(new Item(name, "gate '" + name + "': 'refuses' and 'reads_at_least' are required."));
            }
        }
        Set<String> all = new TreeSet<>(declared);
        all.addAll(implemented);
        return new Result(all.size(), items);
    }

    // ------------------------------------------------------------------------
    // The runner
    // ------------------------------------------------------------------------

    static Set<String> readBaseline(Path path) throws IOException {
        Set<String> lines = new TreeSet<>();
        if (Files.isRegularFile(path)) {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (!line.isBlank() && !line.strip().startsWith("#")) {
                    lines.add(line.strip());
                }
            }
        }
        return lines;
    }

    private static int number(Object value, int fallback) {
        return value instanceof Integer given ? given : fallback;
    }

    public static Report run(Path root) {
        List<String> lines = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        Context ctx;
        try {
            ctx = Context.read(root);
        } catch (RuntimeException e) {
            // A model that cannot be read is one failure with a name, not a stack trace.
            failures.add("[model] The model cannot be read: " + e.getMessage());
            return new Report(lines, failures);
        }
        for (Map.Entry<String, Gate> gate : GATES.entrySet()) {
            String name = gate.getKey();
            Map<String, Object> declaration = Model.map(ctx.gates().get(name));
            String kind = declaration.containsKey("kind") ? Model.text(declaration.get("kind")) : "hard";
            int floor = number(declaration.get("reads_at_least"), 1);
            List<String> found = new ArrayList<>();
            String note = "";
            int read = 0;
            try {
                Result result = gate.getValue().check(ctx);
                read = result.read();
                List<Item> items = result.items();
                if (read < floor) {
                    found.add("read " + read + ", and it must read at least " + floor
                            + ". A gate that reads nothing cannot fail.");
                }
                if (kind.equals("ratchet")) {
                    String file = Model.text(declaration.get("baseline"));
                    Set<String> baseline = readBaseline(root.resolve(file));
                    Set<String> failing = new TreeSet<>();
                    items.forEach(item -> failing.add(item.key()));
                    for (Item item : items) {
                        if (!baseline.contains(item.key())) {
                            found.add(item.message() + " This is new. Correct it.");
                        }
                    }
                    int kept = 0;
                    for (String key : baseline) {
                        if (failing.contains(key)) {
                            kept++;
                        } else {
                            found.add("'" + key + "' is in " + file + " and no longer fails. Delete the line.");
                        }
                    }
                    note = kept + " in the baseline";
                } else if (kind.equals("report")) {
                    int ceiling = number(declaration.get("ceiling"), 0);
                    if (items.size() > ceiling) {
                        items.forEach(item -> found.add(item.message()));
                        found.add("count " + items.size() + " is above the ceiling " + ceiling + ".");
                    }
                    note = "count " + items.size() + ", ceiling " + ceiling;
                } else {
                    items.forEach(item -> found.add(item.message()));
                }
            } catch (Exception e) {
                // One gate that stops must not hide the others.
                found.add("the gate stopped: " + e);
            }
            lines.add(String.format("%s  %-26s %-8s read %-3d %s",
                    found.isEmpty() ? "ok  " : "FAIL", name, kind, read, note).stripTrailing());
            found.forEach(message -> failures.add("[" + name + "] " + message));
        }
        return new Report(lines, failures);
    }

    public static void main(String[] args) {
        Report report = run(Path.of(args.length > 0 ? args[0] : ""));
        report.lines().forEach(System.out::println);
        report.failures().forEach(failure -> System.out.println("FAIL " + failure));
        System.out.println("\n" + GATES.size() + " gates, " + report.failures().size() + " failure(s)");
        if (!report.failures().isEmpty()) {
            System.exit(1);
        }
    }
}
