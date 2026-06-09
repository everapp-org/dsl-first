# Core Concepts of DSL First

> **Origin**: Part of the [DSL-First Methodology](https://github.com/everapp-org/dsl-first) open-source project.  
> **License**: Apache 2.0 — see [LICENSE](../LICENSE)

## The DSL-First Process

With DSL-First, the work shifts from writing boilerplate by hand to deriving it from a model that captures the domain (a model written in one of the methodology's languages). The typical process looks like this:

> **Two derivation styles** (see the guide's *[Two Bindings](../dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md#25-two-bindings-grammar-hosted-and-data-hosted)*): a **grammar-hosted** binding parses a text DSL and *generates* code; a **data-hosted** binding (Clojure/EDN) validates the model against a *schema* and *interprets* it — frequently with no code-generation step at all. The walkthrough below shows the grammar-hosted style in Java; the concepts are identical either way.

### 1. Capture the Domain in a Model

The assistant writes a human-readable, machine-parseable **model** — in a domain-specific language — that strictly defines the domain's entities, state machines, guards, and invariants, from your specification or legacy code.

```kotlin
domain Agent {
    states {
        OFF: "Agent defined but not activated"
        IDLE: "Ready to accept tasks"
        WORKING: "Executing a task"
    }

    transitions {
        activate: OFF → IDLE {
            emits: AgentActivated
            actions: [initializeLLM, loadTools]
        }
        assignTask: IDLE → WORKING {
            guard: hasCapability(task)
        }
    }
}
```

### 2. Generate Code

A code generator parses the model and outputs type-safe boilerplate, enums, event classes, and extension points.

```java
// GENERATED CODE - DO NOT EDIT
public class Agent {
    private AgentState state = AgentState.OFF;

    public void activate() {
        if (state != AgentState.OFF) {
            throw new IllegalStateException();
        }

        initializeLLM();        // action
        loadTools();            // action

        state = AgentState.IDLE;
        emit(new AgentActivated(id));
    }

    protected void initializeLLM() {}  // extension point
    protected void loadTools() {}       // extension point
}
```

### 3. Generate Tests

Generators extract transitions and invariants from the model to build out an exhaustive test suite covering 100% of defined state transitions.

```java
@Test
@DisplayName("OFF → IDLE: activate")
void testActivate() {
    Agent agent = new Agent(id, role, goal);
    assertEquals(OFF, agent.getState());

    agent.activate();

    assertEquals(IDLE, agent.getState());
    assertTrue(agent.hasEmittedEvent(AgentActivated.class));
}
```

### 4. Generate Documentation

The generator translates the parsed DSL into visual representations (like Mermaid diagrams) and markdown files that are *guaranteed* to be accurate.

```markdown
### Agent

**Purpose**: Autonomous worker that executes tasks

**States**: OFF | IDLE | WORKING 

**Test Coverage**: ✅ 100% transitions verified
```
