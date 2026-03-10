# DSL-First Development: A Practitioner's Guide

> **A methodology for building maintainable software through declarative domain modeling and generative meta-programming.**

**Version**: 1.0  
**Target Audience**: Senior developers, architects, tech leads  
**Prerequisites**: Familiarity with DDD, code generation concepts, parser basics

> **Origin**: Part of the [DSL-First Methodology](https://github.com/everapp-org/dsl-first) open-source project.  
> **License**: Apache 2.0 — see [LICENSE](LICENSE)

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Core Concepts](#2-core-concepts)
3. [The Methodology](#3-the-methodology)
4. [DSL Design Patterns](#4-dsl-design-patterns)
5. [Generator Architecture](#5-generator-architecture)
6. [Implementation Workflow](#6-implementation-workflow)
7. [Testing Strategy](#7-testing-strategy)
8. [Build Integration](#8-build-integration)
9. [Evolution & Versioning](#9-evolution--versioning)
10. [Anti-Patterns](#10-anti-patterns)
11. [Case Study: jCrew](#11-case-study-jcrew)
12. [Quick Reference](#12-quick-reference)

---

## 1. Introduction

### 1.1 What is DSL-First Development?

**DSL-First Development** is a software development methodology where:

1. **Domain knowledge is captured in Domain-Specific Languages (DSLs)** before implementation
2. **Code is generated** from DSL definitions via meta-programs (generators)
3. **The DSL serves as the Single Source of Truth** for models, behavior, and documentation

This inverts the traditional approach where code is written first and documentation (if any) follows.

```
Traditional:  Requirements → Code → Documentation → Drift
DSL-First:    Requirements → DSL → Generated Code + Documentation (always in sync)
```

### 1.2 When to Use DSL-First

**Good fit when:**
- Domain has repetitive structures (entities, state machines, configurations)
- Multiple artifacts must stay synchronized (code, tests, docs, schemas)
- Domain experts need to participate in modeling
- System will evolve significantly over time
- Team values consistency over flexibility

**Poor fit when:**
- One-off prototypes or throwaway code
- Highly dynamic domains with no stable structure
- Team lacks meta-programming experience
- Build time is extremely constrained

### 1.3 Benefits

| Benefit | Description |
|---------|-------------|
| **Consistency** | All artifacts generated from same source |
| **Correctness** | Generator encodes patterns once, applied everywhere |
| **Velocity** | Add new domain concepts in minutes, not hours |
| **Documentation** | DSL *is* documentation; always current |
| **Refactoring Safety** | Change DSL, regenerate; compiler catches errors |
| **Onboarding** | New developers read DSL to understand domain |

---

## 2. Core Concepts

### 2.1 Terminology

| Term | Definition | Example |
|------|------------|---------|
| **DSL** | Domain-Specific Language - a constrained language for a specific problem domain | SQL for databases, jCrew DSL for agent modeling |
| **Meta-program** | A program that operates on programs (generates, transforms, analyzes) | Code generators, compilers, linters |
| **Generator** | A meta-program that produces source code from DSL input | JavaPoet-based class generator |
| **Grammar** | Formal specification of DSL syntax | ANTLR .g4 file, PEG grammar |
| **Parser** | Transforms DSL text into structured representation (AST/parse tree) | ANTLR-generated parser |
| **Semantic Model** | In-memory representation of DSL concepts (domain model of the DSL itself) | `DomainDef`, `ModelDef`, `TransitionDef` classes |
| **Extractor/Visitor** | Traverses parse tree to build semantic model | ANTLR visitor pattern implementation |
| **Emitter** | Produces output artifacts from semantic model | Template engine, code builder (JavaPoet) |

### 2.2 Architecture Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                         DSL FILES                               │
│  (domain.dsl, providers.dsl, config.dsl)                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      GRAMMAR LAYER                              │
│  Parser Generator (ANTLR, PEG.js, TreeSitter)                  │
│  Output: Parser + Lexer                                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    EXTRACTION LAYER                             │
│  Visitor/Extractor classes                                      │
│  Output: Semantic Model (in-memory domain representation)       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    GENERATION LAYER                             │
│  Multiple generators per semantic model                         │
│  Output: Source code, schemas, documentation                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    GENERATED ARTIFACTS                          │
│  - Source files (Java, TypeScript, Python, etc.)               │
│  - Test files                                                   │
│  - JSON/YAML schemas                                            │
│  - Documentation (Markdown, HTML)                               │
│  - Diagrams (PlantUML, Mermaid)                                │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 DDD Alignment

DSL-First Development complements Domain-Driven Design:

| DDD Concept | DSL-First Mapping |
|-------------|-------------------|
| **Ubiquitous Language** | DSL vocabulary = ubiquitous language |
| **Bounded Context** | Each DSL file = bounded context |
| **Aggregate** | DSL `model` or `entity` block |
| **Value Object** | DSL `value` or embedded type |
| **Domain Event** | DSL `emits` clause in transitions |
| **Repository** | Generated from DSL aggregate definitions |
| **Specification** | DSL validation rules |

---

## 3. The Methodology

### 3.1 The DSL-First Workflow

```
┌──────────────────────────────────────────────────────────────────┐
│  PHASE 1: DOMAIN ANALYSIS                                        │
│  ─────────────────────────                                       │
│  • Identify core domain concepts (nouns → entities)              │
│  • Identify behaviors (verbs → commands, events)                 │
│  • Identify states and transitions                               │
│  • Identify constraints and invariants                           │
│  • Output: Domain vocabulary list                                │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  PHASE 2: DSL DESIGN                                             │
│  ───────────────────                                             │
│  • Design grammar constructs for each concept                    │
│  • Balance expressiveness vs. simplicity                         │
│  • Validate with domain experts (can they read it?)              │
│  • Output: Grammar specification (.g4, .peg, etc.)               │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  PHASE 3: SEMANTIC MODEL                                         │
│  ───────────────────────                                         │
│  • Define classes representing DSL concepts                      │
│  • These are the "domain model of the DSL"                       │
│  • Implement extractor/visitor to populate model                 │
│  • Output: Semantic model classes + extractor                    │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  PHASE 4: GENERATOR IMPLEMENTATION                               │
│  ─────────────────────────────────                               │
│  • One generator per artifact type                               │
│  • Generator reads semantic model, emits artifacts               │
│  • Encode patterns and best practices in generators              │
│  • Output: Generator classes                                     │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  PHASE 5: INTEGRATION                                            │
│  ────────────────────                                            │
│  • Wire generators into build system                             │
│  • Define output locations for generated files                   │
│  • Configure IDE to recognize generated sources                  │
│  • Output: Working build pipeline                                │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  PHASE 6: ITERATION                                              │
│  ──────────────────                                              │
│  • Write DSL definitions for domain                              │
│  • Generate artifacts                                            │
│  • Write manual runtime code that uses generated code            │
│  • Evolve DSL and generators as domain understanding deepens     │
│  • Output: Working system                                        │
└──────────────────────────────────────────────────────────────────┘
```

### 3.2 The "Generate, Don't Write" Principle

**Rule**: If you're writing the same pattern more than twice, it should be generated.

**Examples of patterns to generate:**
- Entity classes with ID, fields, getters/setters
- State machine transition methods with validation
- Event emission on state changes
- Repository interfaces and implementations
- Configuration POJOs with validation
- JSON Schema from type definitions
- API clients from OpenAPI specs
- Test scaffolding for all transitions

### 3.3 The Separation Principle

**Rule**: Cleanly separate generated code from hand-written code.

**Strategies:**

| Strategy | Description | Pros | Cons |
|----------|-------------|------|------|
| **Separate directories** | `src/main/java` vs `src/generated/java` | Clear separation | IDE may need config |
| **Separate packages** | `com.example.domain` vs `com.example.domain.generated` | Same source root | Package clutter |
| **Partial classes** | Generated base class + manual subclass | Extend without touching generated | Inheritance hierarchy |
| **Composition** | Generated components composed in manual classes | Flexible | More wiring code |

**Recommended**: Separate directories with generated code in source control (for review and debugging).

### 3.4 What to Generate vs What to Hand-Write

A common question: *"If this is DSL-First, why isn't everything generated?"*

**Answer**: DSL-First generates **repetitive domain patterns**, not infrastructure or unique logic.

#### Generate These (Derived from DSL)

| Category | Examples | Why Generate? |
|----------|----------|---------------|
| **Domain Models** | Entity classes, value objects | Consistent structure, state management |
| **State Enums** | `OrderState`, `AgentState` | Single source of truth for states |
| **Domain Events** | `OrderSubmitted`, `TaskCompleted` | Tied to state transitions in DSL |
| **Transition Methods** | `submit()`, `cancel()`, `approve()` | Encode guards, events, actions from DSL |
| **Schemas** | JSON Schema, OpenAPI | Derived from DSL type definitions |
| **Test Scaffolding** | Transition tests, validation tests | Generated from DSL transitions |
| **Documentation** | State diagrams, field docs | Always in sync with DSL |
| **Configuration POJOs** | `CrewConfig`, `AgentConfig` | From config DSL definitions |

#### Hand-Write These (Infrastructure & Unique Logic)

| Category | Examples | Why Hand-Write? |
|----------|----------|-----------------|
| **Protocol Adapters** | `McpToolAdapter`, REST controllers | Translate between protocols—unique per integration |
| **External Clients** | HTTP clients, database drivers | Infrastructure, not domain |
| **Complex Business Logic** | Custom validation, algorithms | Too complex for DSL; referenced by name |
| **Build/Tooling** | Maven plugins, CLI wrappers | One-time setup |
| **Framework Integrations** | Spring config, dependency injection | Framework-specific glue code |

#### The Naming Convention

```
Generated files:
├── src/main/java/generated/         # Clearly marked directory
│   └── org/example/domain/
│       └── Order.java               # Header: "// GENERATED - DO NOT EDIT"

Hand-written files:
├── src/main/java/                   # Standard source
│   └── org/example/
│       ├── adapter/                 # Protocol adapters
│       │   └── McpToolAdapter.java  # Header: "// HAND-WRITTEN - Infrastructure"
│       └── impl/                    # Business logic implementations
│           └── OrderValidator.java
```

#### jCrew Example

| Source DSL | Generated | Hand-Written |
|------------|-----------|--------------|
| `jcrew.dsl` | `Agent.java`, `AgentState.java`, `AgentCreated.java` | `AgentOrchestrator.java` |
| `jcrew-mcp.dsl` | `file_read.schema.json`, `McpToolConfig.java` | `McpToolAdapter.java`, `McpToolRegistry.java` |
| `jcrew-providers.dsl` | `OpenAIGpt4oProfile.java`, `ProviderRegistry.java` | `OpenAIClient.java` |
| `jcrew-config.dsl` | `CrewConfig.java`, `crew-config.schema.json` | YAML loader, CLI |

**Key insight**: The `McpToolAdapter` is infrastructure that bridges jCrew's tool system to the MCP protocol. It's hand-written because:
1. It implements a specific protocol (JSON-RPC 2.0)
2. It's a one-time integration, not a repeating pattern
3. The DSL defines *what* tools exist; the adapter defines *how* to expose them

---

## 4. DSL Design Patterns

### 4.1 Structural Patterns

#### 4.1.1 Hierarchical Scoping

Organize DSL with nested scopes that inherit context:

```dsl
domain ecommerce {                    # Top-level scope
    description "E-commerce domain"
    
    level orders {                    # Second-level scope
        
        model Order {                 # Entity scope
            fields { ... }
            states { ... }
            transitions { ... }
        }
    }
}
```

**Benefits:**
- Natural grouping of related concepts
- Scope provides context for references
- Maps well to bounded contexts

#### 4.1.2 Reference by Name

Use named references instead of embedding definitions:

```dsl
# Good: Reference by name
model Order {
    fields {
        customer: CustomerId          # Reference to another entity
        items: list OrderItem         # Reference to value object
    }
}

# Bad: Embedded definition
model Order {
    fields {
        customer: { id: String, name: String }   # Embedded - harder to reuse
    }
}
```

#### 4.1.3 Modifier Lists

Use modifier keywords for optional properties:

```dsl
fields {
    name: string required "Customer name"
    email: string required unique "Email address"
    phone: string optional "Phone number"
    createdAt: timestamp immutable "Creation timestamp"
}
```

### 4.2 Behavioral Patterns

#### 4.2.1 State Machine Definition

Define states, transitions, guards, and actions:

```dsl
model Order {
    states {
        DRAFT, SUBMITTED, APPROVED, SHIPPED, DELIVERED, CANCELLED
    }
    
    transitions {
        DRAFT -> SUBMITTED on submit {
            guard hasItems
            emits OrderSubmitted
            actions validateInventory, calculateTotal
        }
        
        SUBMITTED -> APPROVED on approve {
            guard isValidPayment
            emits OrderApproved
            actions reserveInventory
        }
        
        * -> CANCELLED on cancel {         # From any state
            guard isCancellable
            emits OrderCancelled
            actions releaseInventory, refundPayment
        }
    }
}
```

#### 4.2.2 Validation Rules

Express business rules declaratively:

```dsl
config OrderConfig {
    fields {
        minOrderAmount: number default 10.00 "Minimum order amount"
        maxItems: integer default 100 "Maximum items per order"
    }
    
    validation {
        "minOrderAmount must be positive"
        "maxItems must be between 1 and 1000"
    }
}
```

### 4.3 Extensibility Patterns

#### 4.3.1 Annotations/Decorators

Add metadata without changing core structure:

```dsl
@aggregate
@event_sourced
model Order {
    @indexed
    fields {
        @encrypted
        customerEmail: string
        
        @audit_log
        status: OrderStatus
    }
}
```

#### 4.3.2 Extension Points

Allow custom behavior injection:

```dsl
model Order {
    transitions {
        SUBMITTED -> APPROVED on approve {
            actions {
                reserveInventory           # Built-in action
                @custom notifyWarehouse    # Custom action - implemented manually
            }
        }
    }
}
```

The generator creates a stub; developers implement the `@custom` actions.

---

## 5. Generator Architecture

### 5.1 Generator Types

| Type | Input | Output | Example |
|------|-------|--------|---------|
| **Model Generator** | Entity definitions | Domain model classes | `Agent.java` |
| **State Generator** | State definitions | Enum classes | `AgentState.java` |
| **Event Generator** | Event definitions | Event record classes | `AgentActivated.java` |
| **Test Generator** | Transition definitions | Unit test classes | `AgentStateTest.java` |
| **Schema Generator** | Type definitions | JSON Schema files | `agent-schema.json` |
| **Doc Generator** | All definitions | Documentation | `domain-model.md` |
| **Diagram Generator** | State machines | PlantUML/Mermaid | `agent-states.puml` |

### 5.2 Generator Structure

```
Generator
├── Input: Semantic Model (parsed DSL)
├── Configuration
│   ├── Output directory
│   ├── Package/namespace
│   ├── Naming conventions
│   └── Feature flags
├── Templates/Builders
│   ├── Class structure templates
│   ├── Method body templates
│   └── Documentation templates
└── Output: Generated artifacts
```

### 5.3 Implementation Approaches

#### 5.3.1 Template-Based Generation

Use a template engine (Velocity, Freemarker, Handlebars, Jinja2):

```velocity
## Java class template
package ${package};

public class ${className} {
    #foreach($field in $fields)
    private ${field.type} ${field.name};
    #end
    
    #foreach($field in $fields)
    public ${field.type} get${field.capitalizedName}() {
        return this.${field.name};
    }
    #end
}
```

**Pros:** Familiar, visual, easy to modify  
**Cons:** String-based, no type safety, harder to test

#### 5.3.2 Builder-Based Generation

Use a code builder library (JavaPoet, KotlinPoet, ts-morph):

```java
// JavaPoet example
TypeSpec classSpec = TypeSpec.classBuilder(className)
    .addModifiers(Modifier.PUBLIC)
    .addFields(fields.stream()
        .map(f -> FieldSpec.builder(f.type(), f.name(), Modifier.PRIVATE).build())
        .toList())
    .addMethods(fields.stream()
        .map(this::generateGetter)
        .toList())
    .build();
```

**Pros:** Type-safe, refactorable, testable  
**Cons:** More verbose, steeper learning curve

#### 5.3.3 Hybrid Approach

Use builders for structure, templates for bodies:

```java
MethodSpec method = MethodSpec.methodBuilder("validate")
    .addModifiers(Modifier.PUBLIC)
    .returns(TypeName.VOID)
    .addCode(templateEngine.render("validation-body.vm", model))
    .build();
```

### 5.4 Generator Testing

**Test generators, not generated code.**

```java
@Test
void shouldGenerateFieldWithCorrectType() {
    // Given
    FieldDef field = new FieldDef("email", "string", true);
    ModelDef model = new ModelDef("User", List.of(field));
    
    // When
    TypeSpec generated = generator.generate(model);
    
    // Then
    FieldSpec emailField = generated.fieldSpecs.get(0);
    assertEquals("email", emailField.name);
    assertEquals(ClassName.get(String.class), emailField.type);
}
```

---

## 6. Implementation Workflow

### 6.1 Adding a New Domain Concept

**Scenario**: Add a new entity type to an existing DSL.

```
Step 1: Update DSL file
────────────────────────
# domain.dsl
model Payment {
    fields {
        id: PaymentId
        orderId: OrderId
        amount: Money
        status: PaymentStatus
    }
    states { PENDING, AUTHORIZED, CAPTURED, REFUNDED, FAILED }
    transitions { ... }
}

Step 2: Run generator
────────────────────────
$ mvn compile  # or: npm run generate

Step 3: Implement custom actions (if any)
────────────────────────
// Manual implementation of @custom actions
public class PaymentActions {
    public void processWithStripe(Payment payment) { ... }
}

Step 4: Write integration code
────────────────────────
// Manual code that uses generated classes
public class PaymentService {
    public void processPayment(PaymentId id) {
        Payment payment = repository.findById(id);
        payment.authorize(stripeToken);  // Generated method
    }
}
```

### 6.2 Modifying Existing Concept

**Scenario**: Add a new field to an existing entity.

```
Step 1: Update DSL
────────────────────────
model Order {
    fields {
        ...
        discountCode: string optional "Applied discount code"  # NEW
    }
}

Step 2: Regenerate
────────────────────────
$ mvn compile

Step 3: Compile and fix
────────────────────────
# Compiler will flag any code that needs updating
# (e.g., builder calls missing the new field)
```

### 6.3 Adding a New Generator

**Scenario**: Generate OpenAPI specs from existing DSL.

```
Step 1: Create semantic model adapter (if needed)
────────────────────────
// Convert DSL model to OpenAPI concepts
public class OpenApiAdapter {
    public OpenApiSpec fromDomain(DomainModel domain) { ... }
}

Step 2: Implement generator
────────────────────────
public class OpenApiGenerator {
    public void generate(DomainModel model, Path outputDir) {
        OpenApiSpec spec = adapter.fromDomain(model);
        writeYaml(spec, outputDir.resolve("openapi.yaml"));
    }
}

Step 3: Wire into build
────────────────────────
// Add to orchestrator
orchestrator.addGenerator(new OpenApiGenerator(config));
```

---

## 7. Testing Strategy

### 7.1 Testing Pyramid for DSL-First

```
                    ┌─────────────┐
                   │  E2E Tests  │  Manual: Test full system
                  │             │  behavior
                 └─────────────┘
                ┌───────────────────┐
               │ Integration Tests │  Generated: Test generated
              │                   │  code compiles & runs
             └───────────────────┘
            ┌─────────────────────────┐
           │    Generator Tests      │  Manual: Test generators
          │                         │  produce correct output
         └─────────────────────────┘
        ┌───────────────────────────────┐
       │     Grammar/Parser Tests      │  Manual: Test DSL parses
      │                               │  correctly
     └───────────────────────────────┘
    ┌─────────────────────────────────────┐
   │      Semantic Model Tests           │  Manual: Test model
  │                                     │  construction
 └─────────────────────────────────────┘
```

### 7.2 What to Generate vs. Write

| Test Type | Generate? | Rationale |
|-----------|-----------|-----------|
| State transition tests | ✅ Yes | Derived directly from DSL |
| Event emission tests | ✅ Yes | Derived from `emits` clauses |
| Validation tests | ✅ Yes | Derived from validation rules |
| Guard condition tests | ❌ No | Guards may have complex logic |
| Integration tests | ❌ No | Depend on external systems |
| Performance tests | ❌ No | Require manual tuning |

### 7.3 Generated Test Example

From DSL:
```dsl
transitions {
    IDLE -> WORKING on assignTask(task: TaskId) {
        guard hasCapacity
        emits AgentAssigned
    }
}
```

Generated test:
```java
@Test
@DisplayName("Agent: IDLE -> WORKING on assignTask")
void testAssignTaskTransition() {
    // Arrange
    Agent agent = createAgentInState(AgentState.IDLE);
    TaskId taskId = new TaskId("task-123");
    
    // Act
    agent.assignTask(taskId);
    
    // Assert
    assertEquals(AgentState.WORKING, agent.getState());
    assertEventEmitted(agent, AgentAssigned.class);
}

@Test
@DisplayName("Agent: assignTask fails from WORKING state")
void testAssignTaskInvalidState() {
    // Arrange
    Agent agent = createAgentInState(AgentState.WORKING);
    
    // Act & Assert
    assertThrows(IllegalStateException.class, 
        () -> agent.assignTask(new TaskId("task-456")));
}
```

---

## 8. Build Integration

### 8.1 Build Phase Placement

```
┌─────────────────────────────────────────────────────────────────┐
│                       BUILD PIPELINE                            │
├─────────────────────────────────────────────────────────────────┤
│  1. Clean                                                       │
│  2. Generate sources (DSL → code)          ◄── GENERATORS RUN  │
│  3. Compile generated + manual sources                         │
│  4. Run unit tests                                              │
│  5. Package                                                     │
│  6. Integration tests                                           │
└─────────────────────────────────────────────────────────────────┘
```

### 8.2 Maven Integration (Java)

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>generate-from-dsl</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>java</goal>
            </goals>
            <configuration>
                <mainClass>com.example.codegen.DslCodeGenerator</mainClass>
                <arguments>
                    <argument>${project.basedir}/src/main/dsl</argument>
                    <argument>${project.build.directory}/generated-sources/dsl</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>

<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>add-generated-sources</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>add-source</goal>
            </goals>
            <configuration>
                <sources>
                    <source>${project.build.directory}/generated-sources/dsl</source>
                </sources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 8.3 npm/Node.js Integration

```json
{
  "scripts": {
    "generate": "ts-node scripts/generate.ts",
    "prebuild": "npm run generate",
    "build": "tsc",
    "test": "jest"
  }
}
```

### 8.4 Incremental Generation

**Problem**: Regenerating everything on every build is slow.

**Solutions:**

1. **Checksum-based**: Hash DSL files, skip if unchanged
2. **Timestamp-based**: Compare DSL mtime vs generated mtime
3. **Dependency tracking**: Track which DSL constructs affect which outputs

```java
public class IncrementalGenerator {
    public void generate(Path dslFile, Path outputDir) {
        String currentHash = hash(dslFile);
        String cachedHash = readCachedHash(dslFile);
        
        if (currentHash.equals(cachedHash)) {
            log.info("DSL unchanged, skipping generation");
            return;
        }
        
        doGenerate(dslFile, outputDir);
        writeCachedHash(dslFile, currentHash);
    }
}
```

---

## 9. Evolution & Versioning

### 9.1 DSL Versioning Strategy

**Embed version in DSL:**
```dsl
@version "2.0"
domain ecommerce {
    ...
}
```

**Version the grammar:**
```
grammars/
├── DomainDSL_v1.g4
├── DomainDSL_v2.g4
└── DomainDSL.g4 → symlink to current
```

### 9.2 Backward Compatibility

| Change Type | Impact | Strategy |
|-------------|--------|----------|
| Add optional field | None | Safe to add |
| Add required field | Breaking | Provide default or migration |
| Rename field | Breaking | Support alias during transition |
| Remove field | Breaking | Deprecate first, remove later |
| Add new state | Minor | Generated code handles gracefully |
| Remove state | Breaking | Requires data migration |
| Change transition | Major | May affect business logic |

### 9.3 Migration Pattern

```dsl
# Version 1
model Order {
    fields {
        customerName: string required
    }
}

# Version 2 - Split name into first/last
model Order {
    fields {
        @deprecated("Use firstName and lastName")
        customerName: string optional
        
        firstName: string required
        lastName: string required
    }
    
    @migration "v1-to-v2" {
        "Split customerName into firstName and lastName"
    }
}
```

Generator produces migration code:
```java
public class OrderMigration_v1_to_v2 {
    public Order migrate(Order_v1 old) {
        String[] parts = old.getCustomerName().split(" ", 2);
        return Order.builder()
            .firstName(parts[0])
            .lastName(parts.length > 1 ? parts[1] : "")
            .build();
    }
}
```

---

## 10. Anti-Patterns

### 10.1 The Kitchen Sink DSL

**Problem**: DSL tries to express everything, becomes as complex as general-purpose language.

```dsl
# Bad: Too much logic in DSL
model Order {
    transitions {
        SUBMITTED -> APPROVED on approve {
            guard {
                if (items.size() > 100) {
                    return customer.tier == "PREMIUM";
                }
                return items.all(i -> inventory.check(i.productId) > i.quantity);
            }
        }
    }
}
```

**Solution**: Keep DSL declarative. Complex logic belongs in hand-written code.

```dsl
# Good: Declarative guard reference
transitions {
    SUBMITTED -> APPROVED on approve {
        guard canApproveOrder    # Implementation in code
    }
}
```

### 10.2 Generate and Modify

**Problem**: Developers modify generated files, changes lost on regeneration.

**Solution**: 
- Mark generated files clearly: `// GENERATED - DO NOT EDIT`
- Use extension points (subclassing, composition, `@custom` actions)
- Configure IDE to warn on editing generated files

### 10.3 DSL Drift

**Problem**: DSL definitions diverge from actual system behavior.

**Solution**:
- Generated code is the ONLY implementation (no parallel hand-written version)
- CI validates DSL parses and generates successfully
- Runtime uses generated classes directly

### 10.4 Over-Generation

**Problem**: Generating code that's simpler to write by hand.

```dsl
# Bad: Generating trivial utility
utility StringUtils {
    method capitalize(s: string): string
    method trim(s: string): string
}
```

**Solution**: Only generate code with patterns that benefit from consistency.

### 10.5 Tight Coupling to Generator

**Problem**: Generated code depends on generator-specific runtime library.

```java
// Bad: Requires generator's runtime
public class Order extends GeneratedEntityBase<OrderState> { ... }
```

**Solution**: Generate standalone code with no special dependencies.

---

## 11. Case Study: jCrew

### 11.1 Project Overview

**jCrew** is a multi-agent orchestration framework using DSL-First Development.

**DSL Files:**
| DSL | Purpose | Generated Artifacts |
|-----|---------|---------------------|
| `jcrew.dsl` | Domain models (Agent, Crew, Task, Tool) | Model classes, states, events, tests |
| `jcrew-providers.dsl` | LLM provider definitions | Provider profiles, registry, enums |
| `jcrew-mcp.dsl` | MCP tool definitions | Tool schemas, server configs, JSON schemas |
| `jcrew-config.dsl` | Configuration schemas | Config POJOs, validators, JSON Schema |

### 11.2 Grammar Example (ANTLR)

```antlr
// KernelDSL.g4 - Core grammar for domain models
grammar KernelDSL;

domain
    : 'domain' NAME '{' domainBody+ '}'
    ;

domainBody
    : description
    | level
    ;

level
    : 'level' NAME '{' levelBody+ '}'
    ;

levelBody
    : modelDef
    | valueDef
    ;

modelDef
    : 'model' NAME '{' modelBody+ '}'
    ;

modelBody
    : fieldsDef
    | statesDef
    | transitionsDef
    ;

transitionsDef
    : 'transitions' '{' transitionDef+ '}'
    ;

transitionDef
    : fromState '->' toState 'on' triggerName ('(' params ')')? '{' transitionBody* '}'
    ;

transitionBody
    : 'guard' NAME
    | 'emits' NAME
    | 'actions' NAME (',' NAME)*
    ;

// Lexer rules
NAME: [a-zA-Z_][a-zA-Z0-9_]*;
STRING: '"' (~["\r\n])* '"';
WS: [ \t\r\n]+ -> skip;
COMMENT: '//' ~[\r\n]* -> skip;
```

### 11.3 Semantic Model Example

```java
// Semantic model - domain model of the DSL itself
public record DomainModel(
    String name,
    String description,
    List<LevelDef> levels
) {}

public record LevelDef(
    String name,
    List<ModelDef> models,
    List<ValueDef> values
) {}

public record ModelDef(
    String name,
    String description,
    List<FieldDef> fields,
    List<String> states,
    List<TransitionDef> transitions
) {}

public record TransitionDef(
    String fromState,
    String toState,
    String trigger,
    List<ParameterDef> parameters,
    String guard,
    String event,
    List<String> actions
) {}
```

### 11.4 Extractor Example

```java
// ANTLR visitor that builds semantic model
public class DomainModelExtractor extends KernelDSLBaseVisitor<Object> {
    private DomainModel.Builder domainBuilder;
    private ModelDef.Builder currentModel;
    
    @Override
    public Object visitDomain(KernelDSLParser.DomainContext ctx) {
        domainBuilder = DomainModel.builder()
            .name(ctx.NAME().getText());
        
        visitChildren(ctx);
        return domainBuilder.build();
    }
    
    @Override
    public Object visitTransitionDef(KernelDSLParser.TransitionDefContext ctx) {
        TransitionDef transition = TransitionDef.builder()
            .fromState(ctx.fromState().getText())
            .toState(ctx.toState().getText())
            .trigger(ctx.triggerName().getText())
            .guard(extractGuard(ctx))
            .event(extractEvent(ctx))
            .actions(extractActions(ctx))
            .build();
        
        currentModel.addTransition(transition);
        return transition;
    }
}
```

### 11.5 Generator Example

```java
// JavaPoet-based generator for domain model classes
public class ModelClassGenerator {
    
    public TypeSpec generate(ModelDef model) {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(model.name())
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(generatedAnnotation());
        
        // Add fields
        for (FieldDef field : model.fields()) {
            classBuilder.addField(generateField(field));
        }
        
        // Add state field
        classBuilder.addField(FieldSpec.builder(
            ClassName.get(statePackage, model.name() + "State"),
            "state",
            Modifier.PRIVATE
        ).build());
        
        // Add transition methods
        for (TransitionDef transition : model.transitions()) {
            classBuilder.addMethod(generateTransitionMethod(model, transition));
        }
        
        return classBuilder.build();
    }
    
    private MethodSpec generateTransitionMethod(ModelDef model, TransitionDef t) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(t.trigger())
            .addModifiers(Modifier.PUBLIC);
        
        // Add parameters
        for (ParameterDef param : t.parameters()) {
            method.addParameter(mapType(param.type()), param.name());
        }
        
        // Add state validation
        method.addStatement("assertState($T.$L)", 
            stateClass(model), t.fromState());
        
        // Add state change
        method.addStatement("this.state = $T.$L",
            stateClass(model), t.toState());
        
        // Add event emission
        if (t.event() != null) {
            method.addStatement("emit(new $T(this.id))", 
                eventClass(t.event()));
        }
        
        // Add action calls
        for (String action : t.actions()) {
            method.addStatement("$L()", action);
        }
        
        return method.build();
    }
}
```

### 11.6 Generated Code Example

**Input DSL:**
```dsl
model Agent {
    fields {
        id: AgentId
        name: String
        role: String
    }
    
    states {
        OFF, IDLE, WORKING, PAUSED, TERMINATED
    }
    
    transitions {
        OFF -> IDLE on activate {
            emits AgentActivated
            actions initializeLLMConnection, loadTools
        }
        
        IDLE -> WORKING on assignTask(taskId: TaskId) {
            guard hasCapacity
            emits AgentAssigned
        }
    }
}
```

**Generated Java:**
```java
// GENERATED CODE - DO NOT EDIT
package org.everapp.jcrew.domain.model;

@Generated("jcrew-codegen")
public class Agent {
    private AgentId id;
    private String name;
    private String role;
    private AgentState state;
    private final List<Object> emittedEvents = new ArrayList<>();
    
    public Agent(AgentId id, String name, String role) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.state = AgentState.OFF;
    }
    
    /**
     * Transition: OFF -> IDLE
     */
    public void activate() {
        assertState(AgentState.OFF);
        this.state = AgentState.IDLE;
        emit(new AgentActivated(this.id));
        initializeLLMConnection();
        loadTools();
    }
    
    /**
     * Transition: IDLE -> WORKING
     */
    public void assignTask(TaskId taskId) {
        assertState(AgentState.IDLE);
        if (!hasCapacity()) {
            throw new GuardFailedException("hasCapacity");
        }
        this.state = AgentState.WORKING;
        emit(new AgentAssigned(this.id, taskId));
    }
    
    private void assertState(AgentState expected) {
        if (this.state != expected) {
            throw new IllegalStateException(
                "Expected " + expected + " but was " + this.state);
        }
    }
    
    private void emit(Object event) {
        this.emittedEvents.add(event);
    }
    
    // Action stubs - implement in subclass or composition
    protected void initializeLLMConnection() { }
    protected void loadTools() { }
    protected boolean hasCapacity() { return true; }
}
```

### 11.7 Project Structure

```
jcrew/
├── jcrew-dsl/                          # DSL module
│   └── src/main/
│       ├── antlr4/.../dsl/
│       │   ├── KernelDSL.g4            # Core domain grammar
│       │   ├── ProviderDSL.g4          # Provider definitions
│       │   ├── McpDSL.g4               # MCP tool definitions
│       │   └── ConfigDSL.g4            # Configuration schemas
│       └── resources/
│           ├── jcrew.dsl               # Domain models
│           ├── jcrew-providers.dsl     # Provider definitions
│           ├── jcrew-mcp.dsl           # MCP definitions
│           └── jcrew-config.dsl        # Config schemas
│
├── jcrew-codegen/                      # Generator module
│   └── src/main/java/.../codegen/
│       ├── model/                      # Semantic models
│       │   ├── DomainModel.java
│       │   ├── ProviderModel.java
│       │   ├── McpModel.java
│       │   └── ConfigModel.java
│       ├── extractor/                  # DSL → Semantic model
│       │   ├── DomainModelExtractor.java
│       │   ├── ProviderModelExtractor.java
│       │   ├── McpModelExtractor.java
│       │   └── ConfigModelExtractor.java
│       ├── generator/                  # Semantic model → Code
│       │   ├── ModelClassGenerator.java
│       │   ├── StateEnumGenerator.java
│       │   ├── EventClassGenerator.java
│       │   ├── ProviderProfileGenerator.java
│       │   ├── McpSchemaGenerator.java
│       │   └── ConfigClassGenerator.java
│       └── DslCodeGenerator.java       # Orchestrator
│
├── jcrew-core/                         # Runtime module
│   └── src/main/java/.../
│       ├── domain/                     # GENERATED models
│       │   ├── model/
│       │   ├── event/
│       │   ├── state/
│       │   └── valueobject/
│       ├── service/                    # MANUAL services
│       ├── runtime/                    # MANUAL runtime
│       └── mcp/                        # MANUAL + GENERATED
│
└── jcrew-specs/                        # Specifications
    └── dsl/
        ├── kernel-dsl-spec.md
        └── dsl-grammar-reference.md
```

---

## 12. Quick Reference

### 12.1 DSL-First Checklist

**Before Starting:**
- [ ] Identify core domain concepts
- [ ] List repetitive patterns suitable for generation
- [ ] Choose parser technology (ANTLR, PEG, TreeSitter)
- [ ] Choose code generation approach (templates vs builders)
- [ ] Define output directory structure

**For Each DSL:**
- [ ] Design grammar (start simple, evolve)
- [ ] Define semantic model classes
- [ ] Implement extractor/visitor
- [ ] Implement generators
- [ ] Write generator tests
- [ ] Wire into build system
- [ ] Document DSL syntax

**Ongoing:**
- [ ] DSL is single source of truth
- [ ] Generated code is never manually edited
- [ ] All changes flow through DSL
- [ ] Version DSL for breaking changes

### 12.2 Technology Options

| Language | Parser | Code Builder |
|----------|--------|--------------|
| Java | ANTLR 4, JavaCC | JavaPoet, Roaster |
| Kotlin | ANTLR 4 | KotlinPoet |
| TypeScript | PEG.js, Chevrotain | ts-morph, TypeScript AST |
| Python | ANTLR 4, PLY, Lark | Jinja2, ast module |
| Go | ANTLR 4, Participle | jennifer, text/template |
| Rust | pest, nom | quote, proc-macro |
| C# | ANTLR 4, Parlot | Roslyn, T4 |

### 12.3 Common Patterns Summary

| Pattern | Use When | Example |
|---------|----------|---------|
| State Machine | Entity has lifecycle states | Order, Agent, Task |
| Configuration Schema | Need validated config POJOs | CrewConfig, AgentConfig |
| API Schema | Generate from/to API specs | OpenAPI, GraphQL |
| Event Sourcing | Need event definitions | DomainEvent types |
| Repository | Need data access layer | AgentRepository |
| Validation Rules | Business rules in DSL | "amount must be positive" |

### 12.4 Glossary

| Term | Definition |
|------|------------|
| **AST** | Abstract Syntax Tree - structured representation of parsed code |
| **Emitter** | Component that outputs generated artifacts |
| **Extractor** | Component that builds semantic model from parse tree |
| **Grammar** | Formal specification of DSL syntax rules |
| **Meta-program** | Program that operates on other programs |
| **Parse Tree** | Concrete syntax tree from parser |
| **Semantic Model** | In-memory representation of DSL domain concepts |
| **Single Source of Truth** | One authoritative definition for derived artifacts |
| **Visitor Pattern** | Design pattern for traversing tree structures |

---

## Appendix A: Further Reading

**Books:**
- *Domain-Specific Languages* - Martin Fowler
- *Language Implementation Patterns* - Terence Parr
- *Domain-Driven Design* - Eric Evans

**Tools:**
- [ANTLR 4](https://www.antlr.org/) - Parser generator
- [JavaPoet](https://github.com/square/javapoet) - Java code generation
- [ts-morph](https://ts-morph.com/) - TypeScript AST manipulation

**Specifications:**
- [JSON Schema](https://json-schema.org/) - Schema definition standard
- [OpenAPI](https://www.openapis.org/) - API specification standard

---

## Appendix B: Template Starter

Use this template to start a new DSL-First project:

```
my-project/
├── dsl/
│   ├── grammar/
│   │   └── MyDSL.g4
│   └── definitions/
│       └── domain.dsl
├── codegen/
│   ├── model/
│   │   └── SemanticModel.java
│   ├── extractor/
│   │   └── DslExtractor.java
│   ├── generator/
│   │   └── CodeGenerator.java
│   └── Main.java
├── core/
│   ├── generated/           # Generated code goes here
│   └── runtime/             # Manual runtime code
└── build.gradle / pom.xml
```

---

**Document Version**: 1.0  
**Last Updated**: 2026-01-29  
**License**: Apache 2.0 — https://github.com/everapp-org/dsl-first/blob/main/LICENSE
