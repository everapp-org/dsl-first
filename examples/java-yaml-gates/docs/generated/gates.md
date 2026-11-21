<!-- GENERATED from model/ by tools/GenerateDocs.java. Do not edit. -->

# What the build refuses

| gate | kind | refuses | limit |
|---|---|---|---|
| `described` | ratchet | An operation or an event with no description. | baseline `model/baselines/described.txt`, it only shrinks |
| `docs-fresh` | hard | A derived document that differs from what the model gives now. |  |
| `event-declared` | hard | An event that an operation emits or hears and that no context declares. |  |
| `gates-declared` | hard | A gate that is implemented and not declared, declared and not implemented, or declared in part. |  |
| `handler-resolves` | hard | An operation that is built and whose handler is not a real method of the correct shape. |  |
| `not-built-count` | report | More operations that are declared and not built than the ceiling allows. | ceiling 1 |
| `not-built-stated` | hard | A status outside the closed set, or a not-built operation with no reason. |  |
| `single-registration-site` | hard | A call that registers a handler, a subscriber or a middleware outside the interpreter. |  |
| `vocabulary` | hard | A refused word used as an identifier in a model. The message names the approved word. |  |
| `wiring-typed` | hard | A wiring whose two ends are not both declared, or do not agree on the type. |  |
