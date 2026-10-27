# Compiled Lex-Engine examples

This module generates six parsers and one DFA scanner during Maven's `generate-sources` phase, then compiles typed Java examples against them. Running a demo does not generate or compile Java at runtime.

From the repository root:

```sh
mvn clean verify
./bin/lexengine examples --list
./bin/lexengine examples --all
```

| Demo | Parser | Specification | Consumer |
| --- | --- | --- | --- |
| `arithmetic` | LR(1) | [arithmetic.lg](src/main/resources/arithmetic.lg) | [ArithmeticExample.java](src/main/java/org/lexengine/examples/ArithmeticExample.java) |
| `list` | LL(1) | [list.lg](src/main/resources/list.lg) | [ListExample.java](src/main/java/org/lexengine/examples/ListExample.java) |
| `config` | Recursive descent | [configuration.lg](src/main/resources/configuration.lg) | [ConfigurationExample.java](src/main/java/org/lexengine/examples/ConfigurationExample.java) |
| `json` | LR(1) | [json.lg](src/main/resources/json.lg) | [JsonExample.java](src/main/java/org/lexengine/examples/JsonExample.java) |
| `legacy` | Recursive descent + standalone DFA lexer | [legacy-expression.grammar](src/main/resources/legacy-expression.grammar), [arithmetic.spec](src/main/resources/arithmetic.spec) | [LexerParserExample.java](src/main/java/org/lexengine/examples/LexerParserExample.java) |
| `lr1` | Canonical LR(1) | [canonical-lr1.grammar](src/main/resources/canonical-lr1.grammar) | [CanonicalLrExample.java](src/main/java/org/lexengine/examples/CanonicalLrExample.java) |

## Run individual examples

```sh
./bin/lexengine examples arithmetic "1 + 2 * (3 - 4)"
./bin/lexengine examples list "[1, 2, 3]"
./bin/lexengine examples config --input-file lexengine-examples/src/main/resources/samples/settings.conf
./bin/lexengine examples json --input-file lexengine-examples/src/main/resources/samples/document.json
./bin/lexengine examples legacy "12 + count * 3"
./bin/lexengine examples lr1 "a c d"
```

A named demo without input uses its built-in sample. Inputs from files are UTF-8. Invalid input returns a failing exit code and a diagnostic.

## Build just this module and dependencies

```sh
mvn -pl lexengine-examples -am clean test
```

Generated Java lives under `target/generated-sources/lexengine/` within this module; compiled classes are in `target/classes/` and included in its JAR. Source specifications are under `src/main/resources/`; update them and rebuild instead of editing generated files.

The Java consumers expose typed `parse(String)` methods returning their generated parser's `ParseTree`, and `format(String)` methods for display. [TreePrinter](src/main/java/org/lexengine/examples/TreePrinter.java) formats any generated node type through method references.

The [POM](pom.xml) configures parser/scanner generation and registers the generated source root. [ExampleCatalog](src/main/java/org/lexengine/examples/ExampleCatalog.java) registers the runnable demos. [ExamplesTest](src/test/java/org/lexengine/examples/ExamplesTest.java) verifies accepted and rejected inputs, JSON escapes, configuration syntax and the lexer adapter.

See the [full walkthroughs](../docs/examples.md), [generated API guide](../docs/generated-api.md), and [architecture guide](../docs/architecture.md).
