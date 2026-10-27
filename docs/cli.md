# CLI reference

The Maven `package` phase creates a unified CLI in `bin/`. The POSIX launcher, Windows launcher and executable JAR expose the same commands:

```sh
./bin/lexengine <command> [options]
java -jar bin/lex-engine.jar <command> [options]
```

On Windows, use `bin\lexengine.cmd`. Paths and input strings containing spaces must be quoted for your shell.

## Top-level commands

| Command | Purpose |
| --- | --- |
| `parser` | Generate a Java parser from `.lg` or `.grammar` input. |
| `lexer` | Generate a standalone DFA scanner from a `.spec` file. |
| `examples` | Run the compiled feature examples. `example` is an alias. |
| `--version` | Display the packaged project version. |
| `--help`, `-h` | Display top-level help. |

Invoking the CLI without arguments displays help. Each generator command accepts `--help`.

## Parser generation

```sh
./bin/lexengine parser \
  --grammar lexengine-examples/src/main/resources/arithmetic.lg \
  --parser-type LR1 \
  --output-directory target/generated
```

| Option | Aliases | Default / behavior |
| --- | --- | --- |
| `--grammar <path>` | `-g`, `-sp` | Required specification file. |
| `--output-directory <path>` | `-d` | Current directory; created if missing. |
| `--parser-type <type>` | `-p` | `RECURSIVE_DESCENT`. Also supports `LL1` and `LR1`, case-insensitively. |
| `--template <path>` | `-t` | Built-in parser source template. A custom value is a filesystem path. |
| `--verbose` | `-v` | Enable generator diagnostics. |
| `--help` | | Display usage. |

The output filename comes from the specification's Java class name. It is written directly inside the requested output directory; the declared package is handled when `javac -d` compiles it. Generation replaces an existing file with the same output filename.

```sh
javac -encoding UTF-8 -d target/generated/classes target/generated/ArithmeticParser.java
```

Choose a parser type using the [algorithm guide](parsers.md). Conflicting table entries cause generation to fail with a diagnostic. There is no precedence-directive or conflict-resolution flag; express precedence in the grammar.

## DFA lexer generation

```sh
./bin/lexengine lexer \
  --lex-spec-file lexengine-examples/src/main/resources/arithmetic.spec \
  --lexer-class ArithmeticLexer \
  --lexer-package org.lexengine.examples.lexer \
  --output-directory target/generated-lexer
```

| Option | Aliases | Default / behavior |
| --- | --- | --- |
| `--lex-spec-file <path>` | `-sp` | Bundled `lexer-spec.spec` resource if omitted. |
| `--output-directory <path>` | `-d` | Current directory. |
| `--lexer-class <name>` | `-lc` | `Lexer`. |
| `--lexer-package <package>` | `-lp` | `org.lexengine.generator`. |
| `--lexer-method <name>` | `-lm` | `nextToken`. |
| `--scanner-template <path-or-resource>` | `-st`, `-sc` | Bundled `scanner-class.tpl`. |
| `--verbose` | `-v` | Enable diagnostics. |
| `--help` | | Display usage. |

Running `lexengine lexer` without options displays help. A specification or template path is opened from the filesystem first, then from the classpath. The generator emits Java actions exactly as specified, so supply the companion `Token` implementation referenced by your actions when compiling the output.

The Java API also supports configuring the return type and encoding through `Options.Builder`; those settings are described in the [lexer guide](lexer.md).

## Compiled examples

```sh
./bin/lexengine examples --list
./bin/lexengine examples --all
./bin/lexengine examples arithmetic "1 + 2 * 3"
./bin/lexengine examples list "[]"
./bin/lexengine examples config --input-file settings.conf
./bin/lexengine examples json --input-file document.json
./bin/lexengine examples legacy "12 + count * 3"
./bin/lexengine examples lr1 "a c d"
```

Running `examples`, `examples --help`, or `examples --list` displays the catalog. `examples --all` runs the built-in sample for every demo. An example name without input uses its built-in sample.

A named example accepts either one input string or `--input-file <path>`. File contents are read as UTF-8. Each successful run prints `Accepted <name> using <algorithm>` followed by its parse tree.

Example parser algorithms are fixed at build time: arithmetic/JSON/canonical use LR(1), list uses LL(1), and configuration/legacy use recursive descent. Use `parser --parser-type` to generate a different implementation for your own grammar.

## Exit codes and output

- `0`: help/version, successful generation, or accepted example input.
- `1`: unknown commands/options, missing arguments, I/O failures, grammar conflicts, or rejected example input.

Command errors are written to standard error. Generated-file messages and example trees are written to standard output; generator diagnostics may also appear there when enabled. Use the exit code for automation instead of parsing a tree or diagnostic line.

```sh
./bin/lexengine examples list "[1,]" >target/example-output.txt 2>target/example-error.txt
```

The input above is rejected because the grammar requires a number after a comma.

## Shell quoting

POSIX shells can pass JSON as a single argument using outer single quotes:

```sh
./bin/lexengine examples json '{"name":"Lex-Engine","enabled":true}'
```

For Windows Command Prompt, PowerShell, or complex multiline data, the file form is easier to reproduce:

```bat
bin\lexengine.cmd examples json --input-file lexengine-examples\src\main\resources\samples\document.json
```

The launchers pass arguments through and locate the JAR relative to the launcher. The POSIX launcher requires Java 21+ and can discover a compatible installed JDK on macOS when the selected runtime is missing or too old. On other systems, configure `JAVA_HOME` or `PATH` with Java 21+. They do not change the caller's working directory.

## Existing Java entry points

The standalone entry points remain available when using module JARs or class directories:

- `org.lexengine.parser.Main`
- `org.lexengine.lexer.Main`
- `org.lexengine.examples.Main`

Their `run(String[], PrintStream, PrintStream)` methods return a status for callers embedding a command. Their `main` methods translate a failure status into the process exit code.
