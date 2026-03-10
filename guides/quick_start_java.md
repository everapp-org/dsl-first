# DSL First: Java ANTLR Guide

This guide covers building a DSL parser and code generator in Java using ANTLR and JavaPoet.

## 1. Setup Maven

```xml
<dependencies>
    <dependency>
        <groupId>org.antlr</groupId>
        <artifactId>antlr4-runtime</artifactId>
        <version>4.13.1</version>
    </dependency>
    <dependency>
        <groupId>com.squareup</groupId>
        <artifactId>javapoet</artifactId>
        <version>1.13.0</version>
    </dependency>
</dependencies>
```

## 2. Generate Parser (ANTLR)

First, define your grammar (`MyDomain.g4`):

```antlr
grammar MyDomain;
domainModel: 'domain' ID '{' ... '}';
```

Then generate the parser:

```bash
antlr4 MyDomain.g4
javac MyDomain*.java
```

## 3. Parse AST

```java
CharStream input = CharStreams.fromFileName("domain.dsl");
MyDSLLexer lexer = new MyDSLLexer(input);
MyDSLParser parser = new MyDSLParser(new CommonTokenStream(lexer));

// Traverse tree
DomainContext domain = parser.domain();
String name = domain.ID().getText();
```

## 4. Generate Java Sources

Use JavaPoet to write the syntax tree into structured Java code:

```java
TypeSpec stateEnum = TypeSpec.enumBuilder(name + "State")
    .addModifiers(Modifier.PUBLIC)
    .addEnumConstant("PENDING")
    .addEnumConstant("ACTIVE")
    .build();

JavaFile.builder("com.example", stateEnum).build().writeTo(outputDir);
```
