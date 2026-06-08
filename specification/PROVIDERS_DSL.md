# Provider Integration: A Kernel DSL Domain Pattern (v1.0)

> **Origin**: Part of the [DSL-First Methodology](https://github.com/everapp-org/dsl-first) open-source project.  
> **License**: Apache 2.0 — see [LICENSE](../LICENSE)

**Version:** 1.0  
**Type:** Domain pattern guide (uses [Kernel DSL v1.1](KERNEL_DSL.md) — no new grammar)  
**Purpose:** Show how to model **platform integration surfaces** as a Kernel DSL domain, with an opinionated three-layer structure that keeps capability intent, protocol binding, and selection policy separate.

---

## 1. This Is a Kernel DSL Domain

Provider integration — binding an application to external APIs, LLM services, or data
platforms — has the same structure as any domain: named entities with fields, optional
lifecycle, configuration blocks. The Kernel DSL handles it without a new grammar.

The value of this pattern guide is not a new metamodel. It is the **three-layer modeling
discipline** that prevents a common failure mode: collapsing capabilities, wire-protocol
details, and operational policy into a single configuration blob.

---

## 2. The Three-Layer Discipline

Every provider integration involves three separable concerns:

| Layer | Name | Concern | Volatility |
|-------|------|---------|------------|
| 1 | **Capability model** | What the provider offers: models, capabilities, context window, cost | Medium — new models added; prices change |
| 2 | **Protocol binding** | How to call it: base URL, auth scheme, endpoint, response mapping | Low — stable once set |
| 3 | **Selection policy** | When to use which model: strategy, constraints, fallback | High — tuned frequently |

Keeping these in separate `model` / `config` blocks makes each concern independently
readable, reviewable, and changeable. It also maps to the classic MDE distinction between
PIM (capability intent) and PSM (platform-specific binding).

---

## 3. Domain Structure in Kernel DSL

```dsl
domain providers {
    description "LLM provider catalog, protocol bindings, and selection policy."

    // --- Layer 1: Capability model (PIM-ish) ---

    level catalog {

        model Provider {
            description "An external API provider."
            fields {
                id:          ProviderId
                displayName: String
            }
        }

        model ModelProfile {
            description "One model tier offered by a provider."
            fields {
                provider:              ProviderId
                modelId:               ModelId
                capabilities:          List<Capability>
                contextWindow:         Integer
                costPer1kInputTokens:  Decimal   // @volatile — update when pricing changes
                costPer1kOutputTokens: Decimal   // @volatile
                requestsPerMinute:     Integer   // @volatile
                tokensPerMinute:       Integer   // @volatile
            }
        }

    }

    // --- Layer 2: Protocol binding (PSM) ---

    level binding {

        model ProtocolBinding {
            description "How to call one provider's API over HTTP."
            fields {
                provider:         ProviderId
                baseUrl:          String
                authScheme:       AuthScheme
                endpoint:         String
                method:           HttpMethod
                responseSelector: String
                selectorLanguage: SelectorLanguage
            }
        }

        model RetryPolicy {
            description "Retry/backoff parameters for transient failures."
            fields {
                provider:          ProviderId
                maxRetries:        Integer
                initialBackoffMs:  Integer
                backoffMultiplier: Decimal
                retryOn:           List<RetryTrigger>
            }
        }

    }

    // --- Layer 3: Selection policy ---

    level policy {

        model SelectionPolicy {
            description "Runtime model-selection strategy and constraints."
            fields {
                strategy:           SelectionStrategy
                fallback:           ModelId
                requireCapability:  Capability
                maxCostPer1kTokens: Decimal
                preferredProviders: List<ProviderId>
            }
        }

    }
}
```

---

## 4. Volatile Facts: Annotation Convention

Cost and rate-limit fields change independently of the rest of the model. Mark them so
readers and generators know to expect churn:

```dsl
costPer1kInputTokens:  Decimal   // @volatile — update when pricing changes
requestsPerMinute:     Integer   // @volatile
```

Generators should propagate this annotation to generated constants:

```java
/** @volatile Pricing changes; update jcrew-providers.dsl when it does. */
public static final double COST_PER_1K_INPUT = 0.005;
```

Separating volatile fields from stable ones also makes `git diff` on the providers DSL
file reveal pricing updates cleanly, without noise from structural changes.

---

## 5. Transformations (M2T)

Because the providers domain is a normal Kernel DSL domain, the standard generators apply
with no special tooling:

| DSL construct | Generated artifact |
|---------------|--------------------|
| `model Provider` | `Provider` value class with ID and display name |
| `model ModelProfile` (per row) | `P_M_Profile` constant class with capability set and cost constants |
| `model ProtocolBinding` | `P_ProtocolConfig` with HTTP binding constants |
| `model RetryPolicy` | `RetryConfig` value object |
| `model SelectionPolicy` | `ModelSelector` class implementing the declared strategy |
| All `ModelProfile` rows together | `ProviderRegistry` with `lookup()` and `findByCriteria()` |

The `ProviderRegistry` and `ModelSelector` are the two artifacts that are worth writing a
dedicated generator for (rather than the generic model generator), because they aggregate
across all declared providers and implement selection logic.

---

## 6. Example: Instance File

```dsl
// jcrew-providers.dsl

domain providers {

    level catalog {

        model Provider {
            fields { id: ProviderId displayName: String }
        }

        model ModelProfile {
            description "gpt-4o"
            fields {
                provider:              OpenAI
                modelId:               gpt-4o
                capabilities:          [function-calling, vision, json-mode, streaming]
                contextWindow:         128000
                costPer1kInputTokens:  0.005    // @volatile
                costPer1kOutputTokens: 0.015    // @volatile
                requestsPerMinute:     10000    // @volatile
                tokensPerMinute:       2000000  // @volatile
            }
        }

        model ModelProfile {
            description "gpt-4o-mini"
            fields {
                provider:              OpenAI
                modelId:               gpt-4o-mini
                capabilities:          [function-calling, json-mode, streaming]
                contextWindow:         128000
                costPer1kInputTokens:  0.00015  // @volatile
                costPer1kOutputTokens: 0.0006   // @volatile
                requestsPerMinute:     30000    // @volatile
                tokensPerMinute:       200000000 // @volatile
            }
        }

    }

    level binding {

        model ProtocolBinding {
            description "OpenAI HTTP binding"
            fields {
                provider:         OpenAI
                baseUrl:          "https://api.openai.com/v1"
                authScheme:       BEARER_TOKEN
                endpoint:         "/chat/completions"
                method:           POST
                responseSelector: "choices[0].message.content"
                selectorLanguage: JSONPATH
            }
        }

        model RetryPolicy {
            fields {
                provider:          OpenAI
                maxRetries:        3
                initialBackoffMs:  1000
                backoffMultiplier: 2.0
                retryOn:           [RATE_LIMIT, TIMEOUT, SERVER_ERROR]
            }
        }

    }

    level policy {

        model SelectionPolicy {
            fields {
                strategy:           COST_OPTIMIZED
                fallback:           gpt-4o-mini
                requireCapability:  function-calling
                maxCostPer1kTokens: 0.02
                preferredProviders: [OpenAI]
            }
        }

    }
}
```

---

## 7. When to Split Into Multiple Files

A single `providers.dsl` works for one provider family. When a project integrates
multiple providers with divergent bindings, split by concern, not by provider:

```
jcrew-providers-catalog.dsl    // all ModelProfile entries, all providers
jcrew-providers-bindings.dsl   // all ProtocolBinding + RetryPolicy entries
jcrew-providers-policy.dsl     // SelectionPolicy (changes most often)
```

This keeps volatile facts (policy, pricing) in files with their own change cadence,
without polluting the stable binding configuration.

---

## 8. Well-Formedness Guidance

Validators for this domain should check:
- Every `ModelProfile` references a `ProviderId` that has a corresponding `ProtocolBinding`.
- The `fallback` model in `SelectionPolicy` references a `modelId` declared in at least one `ModelProfile`.
- `requireCapability` references a capability that at least one declared `ModelProfile` satisfies (warn if not — the constraint would always fail at runtime).
- All `@volatile` fields have a comment; generators should fail loudly if the comment is absent.

---

**Document Version**: 1.0  
**Last Updated**: 2026-06-08  
**License**: Apache 2.0 — https://github.com/everapp-org/dsl-first/blob/main/LICENSE
