package org.everapp.taskapp;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Read the model. Every other class gets the model from here.
 *
 * The model is plain data in YAML files. There is no grammar and no parser of
 * our own. The readers below never throw on a missing key: a model with a
 * defect must reach the gate that names the defect.
 */
public final class Model {
    public static final List<String> LEVELS = List.of("commands", "queries", "subscribers", "middleware");
    public static final Set<String> BUILT = Set.of("ready", "in-progress");
    public static final Set<String> STATUSES = Set.of("ready", "in-progress", "not-built");

    /** One declared operation: "context.name", its level, and its declaration. */
    public record Operation(String name, String level, Map<String, Object> declaration) {
        public String context() {
            return name.substring(0, name.indexOf('.'));
        }

        public String status() {
            return text(declaration.get("status"));
        }
    }

    public final Map<String, Object> app;
    public final Map<String, Map<String, Object>> contexts = new TreeMap<>();

    private Model(Map<String, Object> app) {
        this.app = app;
    }

    public static Map<String, Object> readYaml(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return map(new Yaml(new SafeConstructor(new LoaderOptions())).load(reader));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Model load(Path root) {
        Path modelDir = root.resolve("model");
        Model model = new Model(readYaml(modelDir.resolve("app.yaml")));
        try (Stream<Path> files = Files.list(modelDir.resolve("contexts"))) {
            for (Path path : files.filter(p -> p.toString().endsWith(".yaml")).sorted().toList()) {
                Map<String, Object> context = readYaml(path);
                model.contexts.put(text(context.get("context")), context);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return model;
    }

    /** Each declared operation of the given levels, sorted by context, level order, name. */
    public List<Operation> operations(Collection<String> levels) {
        List<Operation> found = new ArrayList<>();
        contexts.forEach((contextName, context) -> {
            for (String level : levels) {
                new TreeMap<>(map(context.get(level))).forEach((name, declaration) ->
                        found.add(new Operation(contextName + "." + name, level, map(declaration))));
            }
        });
        return found;
    }

    public List<Operation> operations() {
        return operations(LEVELS);
    }

    public Map<String, Map<String, Object>> events() {
        Map<String, Map<String, Object>> declared = new TreeMap<>();
        contexts.values().forEach(context ->
                map(context.get("events")).forEach((name, declaration) -> declared.put(name, map(declaration))));
        return declared;
    }

    public List<Map<String, Object>> wirings() {
        return list(app.get("wirings")).stream().map(Model::map).toList();
    }

    // -- readers that give an empty value for a missing or malformed key -----

    @SuppressWarnings("unchecked")
    public static Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> given)) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        ((Map<Object, Object>) given).forEach((key, item) -> out.put(String.valueOf(key), item));
        return out;
    }

    public static List<Object> list(Object value) {
        return value instanceof List<?> given ? new ArrayList<>(given) : List.of();
    }

    public static List<String> strings(Object value) {
        return list(value).stream().map(String::valueOf).toList();
    }

    public static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
