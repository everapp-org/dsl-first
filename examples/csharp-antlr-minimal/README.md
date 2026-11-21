# Minimal DSL-First in C# / .NET (grammar-hosted)

A complete, runnable grammar-hosted pipeline in modern C# / .NET: a text DSL is parsed by an ANTLR 4 grammar, and a generator emits C# source files from it via Roslyn (`Microsoft.CodeAnalysis.CSharp`). It models an **Agent state machine** (the same domain as [`../java-antlr-minimal`](../java-antlr-minimal) and [`../clojure-edn-minimal`](../clojure-edn-minimal), so you can compare the implementations side by side).

This README is for an implementer building the pipeline by hand.

## Files

| File | Role |
|------|------|
| `Resources/domain.dsl` | **The DSL** — the Agent model, in text form |
| `Grammar/MyDSL.g4` | **The grammar** — ANTLR rules for the DSL's syntax |
| `Generator/MainGenerator.cs` | **The deriver** — parses the DSL and emits C# via Roslyn `SyntaxFactory` |
| `DslGenerator.csproj` | Build: .NET 10 SDK + ANTLR (`Antlr4BuildTasks` + `Antlr4.Runtime.Standard`) + Roslyn (`Microsoft.CodeAnalysis.CSharp`) |

## The pipeline

The model (`Resources/domain.dsl`):

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

1. **Grammar → parser.** `MyDSL.g4` defines the syntax; `Antlr4BuildTasks` turns it into a C# lexer + parser automatically during `dotnet build`.
2. **Parse.** `MainGenerator` reads `Resources/domain.dsl` and walks the parse tree for the domain name, its states, and its transitions.
3. **Generate.** It uses Roslyn's `SyntaxFactory` API to emit two formatted C# files into `bin/generated-sources/dsl/Everapp/Generated/`: the `AgentState` enum and a sealed stateful `Agent` class with one guarded method per transition.

You wrote `domain.dsl`; both `.cs` files were *derived* — never hand-written, and regenerated whenever the DSL changes.

## What you get

The 8-line model above expands into two compilable source files. You can read them
right here — [`generated-preview/`](generated-preview/) holds committed copies so you
don't need a toolchain — but the build is what actually produces them.

`AgentState.cs` — derived from the `states` block:

```csharp
public enum AgentState { OFF, IDLE, WORKING }
```

`Agent.cs` — derived from the `transitions` block. Each transition becomes a
**guarded** method: it can only fire from its declared source state, so illegal
moves throw `InvalidOperationException` instead of silently corrupting state. None of this was hand-written.

```csharp
public sealed class Agent
{
    public AgentState State { get; private set; } = AgentState.OFF;

    public void Activate()
    {
        Require(State == AgentState.OFF, "activate");
        State = AgentState.IDLE;
    }
    public void AssignTask()
    {
        Require(State == AgentState.IDLE, "assignTask");
        State = AgentState.WORKING;
    }
    public void CompleteTask()
    {
        Require(State == AgentState.WORKING, "completeTask");
        State = AgentState.IDLE;
    }

    private void Require(bool allowed, string action)
    {
        if (!allowed)
            throw new InvalidOperationException("Cannot " + action + " from state " + State);
    }
}
```

That is the methodology's payoff in miniature: a few lines of model, expanded by a
*deterministic* generator (cheap CPU) into correct-by-construction code — no drift,
no hallucination. Add a transition to `domain.dsl`, rerun, and a new guarded method
appears; remove a state and references to it vanish.

## Run it

```bash
dotnet build   # generate the parser from MyDSL.g4 (via Antlr4BuildTasks) + compile generator
dotnet run     # run MainGenerator
# => ✅ Generated AgentState.cs and Agent.cs in bin/generated-sources/dsl/Everapp/Generated
```

The real output lands in `bin/` (git-ignored). The committed copies in
[`generated-preview/`](generated-preview/) are there purely so the output is readable
on GitHub without building.

If you change `domain.dsl`, the grammar, or the generator, refresh those copies in the
same commit so they don't drift:

```bash
./verify-generated-preview.sh            # diff committed snapshot vs fresh output
./verify-generated-preview.sh --update   # regenerate and refresh generated-preview/
```

See [`generated-preview/README.md`](generated-preview/README.md) for the full workflow.

(In a real project the generator runs automatically as an MSBuild `<Target Name="GenerateFromDsl" BeforeTargets="CoreCompile">` step or as a Roslyn incremental source generator (`IIncrementalGenerator`) — see §8 of the [Methodology Guide](../../dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md).)
