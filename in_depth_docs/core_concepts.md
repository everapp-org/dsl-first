# Core Concepts of DSL First

> **Origin**: Part of the [DSL-First Methodology](https://github.com/everapp-org/dsl-first) open-source project.  
> **License**: Apache 2.0 — see [LICENSE](../LICENSE)

## The DSL-First Process

With DSL-First, the work shifts from writing boilerplate by hand to deriving it from a language that captures the domain. The typical process looks like this:

### 1. Capture the Domain in a DSL

The assistant writes a human-readable, machine-parseable DSL that strictly defines the domain's models, state machines, guards, and invariants — from your specification or legacy code.

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

A code generator parses the DSL and outputs type-safe boilerplate, enums, event classes, and extension points.

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

Generators extract transitions and invariants from the DSL to build out an exhaustive test suite covering 100% of defined state transitions.

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
