# Minimal DSL-First in Java (grammar-hosted)

A complete, runnable grammar-hosted pipeline: a text DSL is parsed by an ANTLR grammar, and a generator emits a Java source file from it. It models an **Agent state machine** (the same domain as [`../clojure-edn-minimal`](../clojure-edn-minimal), so you can compare the two bindings).

This README is for an implementer building the pipeline by hand.

## Files

| File | Role |
|------|------|
| `src/main/resources/domain.dsl` | **The DSL** — the Agent model, in text form |
| `src/main/antlr4/org/everapp/MyDSL.g4` | **The grammar** — ANTLR rules for the DSL's syntax |
| `src/main/java/org/everapp/generator/MainGenerator.java` | **The deriver** — parses the DSL and emits Java via JavaPoet |
| `pom.xml` | Build: ANTLR (parser generation) + JavaPoet |

## The pipeline

The model (`domain.dsl`):

```
domain Agent {
    states { OFF, IDLE, WORKING }
    transitions {
        activate: OFF -> IDLE
        assignTask: IDLE -> WORKING
        completeTask: WORKING -> IDLE
    }
}
```

1. **Grammar → parser.** `MyDSL.g4` defines the syntax; ANTLR turns it into a lexer + parser.
2. **Parse.** `MainGenerator` reads `domain.dsl` and walks the parse tree to find the domain name and its states.
3. **Generate.** It uses JavaPoet to emit an `AgentState` enum to `target/generated-sources/dsl/org/everapp/generated/AgentState.java`.

You wrote `domain.dsl`; `AgentState.java` was *derived* — never hand-written, and regenerated whenever the DSL changes.

## Run it

```bash
mvn antlr4:antlr4      # generate the parser from MyDSL.g4
mvn compile            # compile parser + generator
mvn exec:java -Dexec.mainClass=org.everapp.generator.MainGenerator
# => ✅ Generated AgentState.java in target/generated-sources/dsl
```

(In a real project the generator runs automatically in the `generate-sources` build phase — see §8 of the [Methodology Guide](../../dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md).)
