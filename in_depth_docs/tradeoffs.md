# When NOT to use DSL First

> **Origin**: Part of the [DSL-First Methodology](https://github.com/everapp-org/dsl-first) open-source project.  
> **License**: Apache 2.0 — see [LICENSE](../LICENSE)

DSL-First is not a silver bullet. It has a high initial investment and learning curve.

## ❌ Poor Fits

1. **Simple CRUD Apps**: If your application is mostly data operations with few domain rules, the overhead of a DSL pipeline is not worth it.
2. **Prototypes**: If you are throwing code away, exploring a problem space, or navigating highly uncertain requirements, DSL-First is too rigid.
3. **Unique Snowflakes**: If every domain object is entirely different and there are no patterns to extract, a generator will provide little value.

## ✅ Good Fits

1. **Domain-Driven Systems**: Complex domain models with many state machines and business rules.
2. **Long-Lived Projects**: Multi-year timelines with multiple teams and high maintenance burden.
3. **Regulated Industries**: When you need formal specifications and auditable documentation.
4. **AI-Assisted Development**: When you want to leverage LLMs to rapidly iterate over designs safely.

## The Trade-offs

The size of these costs depends heavily on the **binding** (see the guide's *[Two Bindings](../dsl_first_methodology/DSL_FIRST_METHODOLOGY_GUIDE.md#25-two-bindings-grammar-hosted-and-data-hosted)*). Grammar-hosted (Java, Go, C#) carries the full weight below; data-hosted (Clojure and other homoiconic hosts) avoids much of it — the model is plain data, so there is no parser and often no generation step.

1. **Initial Investment**: Grammar-hosted, expect a couple of weeks to design the syntax, build the parser (e.g. ANTLR), and build the code/test/doc generators. Data-hosted is much lighter — the model is data validated by a schema, with no parser and usually no generator — but you still invest in the schema and the interpretation functions.
2. **Learning Curve**: The team must learn the language, when to change the model vs. extend the runtime, and code-generation (or, data-hosted, interpretation) concepts.
3. **Derivation Maintenance**: The generators — or, data-hosted, the schema and interpreter — are themselves code that must be maintained, tested, and documented.
4. **Limited Flexibility**: Grammar-hosted generated code is rigid; you add custom behavior through extension points (inheritance, composition). Data-hosted interpretation is more malleable — behavior is ordinary functions over the data — at the cost of pushing more logic into runtime rather than compiled output.
