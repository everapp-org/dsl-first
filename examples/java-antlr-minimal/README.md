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
2. **Parse.** `MainGenerator` reads `domain.dsl` and walks the parse tree for the domain name, its states, and its transitions.
3. **Generate.** It uses JavaPoet to emit two files into `target/generated-sources/dsl/org/everapp/generated/`: the `AgentState` enum and a stateful `Agent` class with one guarded method per transition.

You wrote `domain.dsl`; both `.java` files were *derived* — never hand-written, and regenerated whenever the DSL changes.

## What you get

The 8-line model above expands into two compilable source files. You can read them
right here — [`generated-preview/`](generated-preview/) holds committed copies so you
don't need a toolchain — but the build is what actually produces them.

`AgentState.java` — derived from the `states` block:

```java
public enum AgentState { OFF, IDLE, WORKING }
```

`Agent.java` — derived from the `transitions` block. Each transition becomes a
**guarded** method: it can only fire from its declared source state, so illegal
moves throw instead of silently corrupting state. None of this was hand-written.

```java
public final class Agent {
  private AgentState state = AgentState.OFF;

  public AgentState state() { return state; }

  public void activate() {
    require(state == AgentState.OFF, "activate");
    state = AgentState.IDLE;
  }
  public void assignTask() {
    require(state == AgentState.IDLE, "assignTask");
    state = AgentState.WORKING;
  }
  public void completeTask() {
    require(state == AgentState.WORKING, "completeTask");
    state = AgentState.IDLE;
  }

  private void require(boolean allowed, String action) {
    if (!allowed)
      throw new IllegalStateException("Cannot " + action + " from state " + state);
  }
}
```

That is the methodology's payoff in miniature: a few lines of model, expanded by a
*deterministic* generator (cheap CPU) into correct-by-construction code — no drift,
no hallucination. Add a transition to `domain.dsl`, rerun, and a new guarded method
appears; remove a state and references to it vanish.

## Run it

```bash
mvn antlr4:antlr4      # generate the parser from MyDSL.g4
mvn compile            # compile parser + generator
mvn exec:java -Dexec.mainClass=org.everapp.generator.MainGenerator
# => ✅ Generated AgentState.java and Agent.java in target/generated-sources/dsl
```

The real output lands in `target/` (git-ignored). The committed copies in
[`generated-preview/`](generated-preview/) are there purely so the output is readable
on GitHub without building.

(In a real project the generator runs automatically in the `generate-sources` build phase — see §8 of the [Methodology Guide](../../dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md).)
