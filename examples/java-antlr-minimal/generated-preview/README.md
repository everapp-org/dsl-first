# Generated preview

These two files are **committed copies of what the build generates** — checked in
so you can read the output without installing a JDK, Maven, and ANTLR.

| File | Derived from |
|------|--------------|
| `AgentState.java` | the `states { … }` block of [`../src/main/resources/domain.dsl`](../src/main/resources/domain.dsl) |
| `Agent.java` | the `transitions { … }` block — one guarded method per transition |

**The build is the source of truth, not these files.** Running the pipeline writes
the real output to `target/generated-sources/dsl/` (git-ignored) and overwrites
nothing here. If you change `domain.dsl`, regenerate and refresh this folder — never
hand-edit the `.java` files (that is exactly the drift DSL-First exists to prevent).

To keep the snapshot honest, [`../verify-generated-preview.sh`](../verify-generated-preview.sh)
regenerates and diffs against these copies (run it in CI). After an intended change:

```bash
../verify-generated-preview.sh --update   # refresh this folder, then commit
```
