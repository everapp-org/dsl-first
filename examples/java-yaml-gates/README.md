# DSL-First in Java: a YAML model, and gates that can fail

A runnable data-hosted pipeline in Java, with **one run-time dependency** (SnakeYAML, to read the model). The model is YAML. An interpreter wires a small task application from the model by reflection. Ten **gates** hold the model and the hand-written code together, and a harness proves that each gate can fail.

This is the Java twin of [`python-toml-gates`](../python-toml-gates/README.md), and the companion of the [Gates Guide](../../dsl_first_methodology/DSL_FIRST_GATES_GUIDE.md). The application, the model and the ten gates are the same. Only the carrier (YAML for TOML) and the host mechanisms differ.

## Run it

You need a JDK 17 or later, and Maven.

```bash
cd examples/java-yaml-gates

mvn -q verify                              # compile, then 8 tests: the application, the gates, the planted failures
mvn -q compile exec:java@gates             # the gates: exit 0 when every rule holds
mvn -q compile exec:java@verify-gates      # put 15 defects back; each gate must fire
mvn -q compile exec:java@generate-docs     # derive docs/generated/ from the model
```

Expected from the gates:

```
ok    described                  ratchet  read 9   1 in the baseline
ok    docs-fresh                 hard     read 3
ok    event-declared             hard     read 3
ok    gates-declared             hard     read 10
ok    handler-resolves           hard     read 6
ok    not-built-count            report   read 7   count 1, ceiling 1
ok    not-built-stated           hard     read 7
ok    single-registration-site   hard     read 9
ok    vocabulary                 hard     read 25
ok    wiring-typed               hard     read 1

10 gates, 0 failure(s)
```

`mvn verify` is the one command for a pre-commit hook and for CI: the tests run the gates and the planted failures.

## How this relates to the other examples

| | `java-antlr-minimal` | `python-toml-gates` | this example |
|---|---|---|---|
| Binding | grammar-hosted | data-hosted | data-hosted |
| Host language | Java | Python | Java |
| Model carrier | a text file with its own grammar | TOML | YAML |
| Model → code | Java source is generated | `importlib` + `getattr` | `Class.forName` + `getMethod` |
| Checks on the model | the grammar | ten declared gates | the same ten gates |
| Planted failures | — | 14 | 15 |

Java can host both bindings. Use the grammar-hosted one when you want generated types that `javac` checks. Use this one when the model must stay plain data that a second tool, or a second language, can read.

## What is in it

```
model/                         THE MODEL — the single source of truth
  contexts/tasks.yaml            commands, queries, events, a subscriber
  contexts/audit.yaml            a middleware and a query
  app.yaml                       the composition, and the WIRINGS between operations
  language.yaml                  the vocabulary: one thing, one name
  gates.yaml                     every gate, declared, with its planted failures
  baselines/described.txt        the frozen debt of one ratchet. It only shrinks
src/main/java/org/everapp/taskapp/
  Model.java                     read the model
  Bus.java                       the one seam; refuses what the model does not declare
  Interpreter.java               wires the application. The ONE legal registration site
  tasks/ audit/                  hand-written bodies
  tools/Gates.java               the gates
  tools/VerifyGates.java         the planted-failure harness
  tools/GenerateDocs.java        the deriver for docs/generated/
docs/generated/                DERIVED — never edited by hand
src/test/java/.../AppTest.java the application, the conformance test, and both tools
```

## How each technique is done in Java

| technique | in this example |
|---|---|
| read the model | SnakeYAML with `SafeConstructor`, into plain maps and lists |
| a pointer resolves | `handled_by: org.everapp.taskapp.tasks.Handlers#create` → `Class.forName` + `getMethod` with the parameter types of the level |
| one registration site | the parser of the JDK compiler (`com.sun.source`, `JavacTask.parse`) reads each source file. No compile, no extra library |
| wiring types agree | a plain comparison of the two declared types |
| derived files are fresh | derive again in memory, compare with the committed file |
| planted failures | copy the project to a temporary directory, edit one file, run the gates on the copy in the same JVM |
| runs on every change | `mvn verify` |

## The points that are specific to Java

### 1. The pointer gate checks the shape, not only the name

In Python, `handler-resolves` asks if the pointer is a callable. In Java, reflection gives more. The gate asks for a public static method with the parameter types of its level: `(Map, Map, Emit)` for a command or a query, `(Map, String, Map)` for a subscriber or a middleware.

So this example has one planted failure more than the Python one. It points a query at a real method that has the shape of a middleware:

```
FAIL [handler-resolves] tasks.list: handled_by 'org.everapp.taskapp.audit.Middleware#record'
     is not a public static method that takes (Map, Map, Emit).
     Write the method, or correct the pointer.
```

A gate that checks the name only would be green, and the defect would arrive at run time.

### 2. The gate that reads source does not compile it

`single-registration-site` must find a call to `registerHandler` in any file that is not the interpreter. A text search would also find the name in a comment or in a string. This gate uses the parser of the compiler that is in every JDK, and walks the tree for method calls and method references:

```
FAIL [single-registration-site] src/main/java/org/everapp/taskapp/tasks/Queries.java:13
     calls registerHandler. Declare the operation in model/contexts/ and let the interpreter wire it.
```

It only parses. So it needs no class path, and it reads a planted source file that was never compiled.

### 3. What the planted failures do not do

The harness runs the gates on a copy of the project **in the same JVM**. It does not compile the copy. Two effects follow, and you must know them:

- `handler-resolves` resolves a pointer against the classes of the running build, not against the sources of the copy. Its planted failures edit the model, so this is correct for them.
- No planted failure edits `Gates.java`. The two directions of `gates-declared` are both proven by an edit of `model/gates.yaml`: one declares a gate that no method implements, the other removes the declaration of a gate that a method implements.

The Python example runs each planted failure in a new process, so it does not have these two limits.

### 4. A gate that stops does not hide the others

The runner catches an exception of one gate and reports it as a failure of that gate. A model file that cannot be parsed is one failure with a name:

```
FAIL [model] The model cannot be read: while parsing a flow sequence ...
```

The readers in `Model.java` give an empty value for a missing key. So a model with a missing key reaches the gate that names the defect, and does not stop in the generator.

## Everything else is the same

The other points are those of the Python example, with the same model and the same messages. Refer to its README for them:

- an edge of the model is declared (`wirings:` in `model/app.yaml`), so a gate can check it
- three kinds of gate: hard, ratchet (`described`, with `model/baselines/described.txt`), and report with a ceiling (`not-built-count`)
- a floor `reads_at_least` on each gate, so a gate cannot go green because its population went away
- "not built" is a declaration: `tasks.archive` is refused with its reason, and `tasks.delete` is refused as not declared
- the vocabulary is enforced, and the glossary is derived from it

## Derivation and conformance, both

- **Derived**: the wiring of every handler, the payload checks of the bus, the list of events an operation may emit, and the three documents. None has a hand-written twin.
- **Conformed**: the handler bodies are written by hand. `handler-resolves` pins each pointer to a real method, and `AppTest` compares the declared operations with the ones the running bus holds, as an equality.

## Use it in your project

Copy `tools/Gates.java`, `tools/VerifyGates.java` and `model/gates.yaml`, then replace the gate methods with your own rules. Keep the runner, the three kinds, the floor and the harness as they are.

If your project already uses Jackson, replace the 3 lines of `Model.readYaml` with a Jackson reader. JSON is then also possible. Nothing else in the example knows the format.

Measured with Temurin 21 and Maven 3.8.7, compiled with `--release 17`. It was not run on a JDK 17.
