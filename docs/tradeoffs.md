# When NOT to use DSL First

> **Origin**: Part of the [DSL-First Methodology](https://github.com/everapp-org/dsl-first) open-source project.  
> **License**: Apache 2.0 — see [LICENSE](../LICENSE)

DSL-First is not a silver bullet. It has a high initial investment and learning curve.

## ❌ Poor Fits

1. **Simple CRUD Apps**: If your application is mostly data operations with few domain rules, the overhead of building a DSL parser and generator is not worth it.
2. **Prototypes**: If you are throwing code away, exploring a problem space, or navigating highly uncertain requirements, DSL-First is too rigid.
3. **Unique Snowflakes**: If every domain object is entirely different and there are no patterns to extract, a generator will provide little value.

## ✅ Good Fits

1. **Domain-Driven Systems**: Complex domain models with many state machines and business rules.
2. **Long-Lived Projects**: Multi-year timelines with multiple teams and high maintenance burden.
3. **Regulated Industries**: When you need formal specifications and auditable documentation.
4. **AI-Assisted Development**: When you want to leverage LLMs to rapidly iterate over designs safely.

## The Trade-offs

1. **Initial Investment**: Expect 2-4 weeks to design the syntax, build the ANTLR parser, and build the code, test, and doc generators.
2. **Learning Curve**: The team must learn the DSL syntax, when to change the DSL vs extend the generated code, and code generation concepts.
3. **Generator Maintenance**: The generators themselves are code that must be maintained, tested, and documented.
4. **Limited Flexibility**: Generated code is rigid. You must use extension points (inheritance, composition) to add custom behavior.
