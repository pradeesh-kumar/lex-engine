# Lex-Engine documentation

Lex-Engine generates Java scanners and parsers from lexical and grammar specifications. The generators run during development or a Maven build; generated scanners and parsers run independently on Java 21+.

Start by building the project and running its compiled examples:

```sh
mvn clean verify
./bin/lexengine examples --all
```

On Windows, use `bin\lexengine.cmd` for the CLI. Commands in these guides assume the repository root as the current directory unless stated otherwise.

## Guides

| Guide | What it covers |
| --- | --- |
| [Getting started and building](getting-started.md) | JDK/Maven setup, automatic example generation, the CLI builder, Maven phases, and portable distributions. |
| [CLI reference](cli.md) | Parser and lexer generation commands, examples, options, exit codes, and shell quoting. |
| [Grammar specification](grammar.md) | Combined `.lg` files, Java metadata, tokens, EBNF, labels, epsilon, and legacy grammars. |
| [Parser algorithms](parsers.md) | Recursive descent, LL(1), canonical LR(1), FIRST/FOLLOW, recursion elimination, and conflicts. |
| [Lexer generation](lexer.md) | DFA `.spec` files, regex features, Java actions, companion token classes, and scanner APIs. |
| [Generated APIs and parse trees](generated-api.md) | Constructors, immutable nodes, token lists, lexer adapters, programmatic generation, and templates. |
| [Example walkthroughs](examples.md) | Six runnable, typed examples with their sources, inputs, expected behavior, and variations. |
| [Architecture and Maven integration](architecture.md) | Module dependencies, source generation, packaging, the distribution builder, and tests. |
| [Troubleshooting](troubleshooting.md) | Build errors, grammar conflicts, lexical errors, invalid input, and CLI problems. |

## Feature map

| Feature | Where to try it |
| --- | --- |
| Operator precedence and nested expressions | `lexengine examples arithmetic` |
| Optional EBNF groups and repeated lists | `lexengine examples list` |
| Recursive descent, comments and optional semicolons | `lexengine examples config` |
| Recursive JSON, string escapes and exponents | `lexengine examples json` |
| A standalone DFA lexer feeding a legacy parser | `lexengine examples legacy` |
| Canonical LR(1) lookahead and explicit token lists | `lexengine examples lr1` |

All six example parsers and the example DFA lexer are generated and compiled in `lexengine-examples` during Maven's build. The examples do not invoke a Java compiler when they run.

## Source locations

- [Example module](../lexengine-examples/README.md): Java consumers, grammars, lexical specification and sample input files.
- [Parser core](../lexengine-parser/src/main/java/org/lexengine/parser/core): grammar validation, transformations and source emission.
- [Lexer core](../lexengine-lexer/src/main/java/org/lexengine/lexer/core): regex parsing, NFA/DFA construction and source emission.
- [CLI module](../lexengine-cli/pom.xml): executable JAR, launchers and distribution assembly.
- [CLI build script](../scripts/build-cli.sh): builds the CLI and its reactor dependencies.
