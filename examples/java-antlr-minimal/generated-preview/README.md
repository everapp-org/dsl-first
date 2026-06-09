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

## Keeping the snapshot honest (manual workflow)

There is no automation watching this folder — it stays correct only if you run the
check yourself. [`../verify-generated-preview.sh`](../verify-generated-preview.sh)
regenerates from `domain.dsl` and diffs against these copies.

```bash
cd ..                              # the example root (has pom.xml)

./verify-generated-preview.sh      # verify: prints a diff and exits 1 on drift
./verify-generated-preview.sh --update   # accept current output as the new snapshot
```

The discipline: **whenever you change `domain.dsl`, the grammar, or the generator,
run `--update` and commit the refreshed `generated-preview/` in the same change.**
Run the plain (no-flag) form before committing to confirm there's nothing stale —
a non-zero exit means the snapshot no longer matches what the build produces.
