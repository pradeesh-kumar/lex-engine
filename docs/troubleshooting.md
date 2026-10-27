# Troubleshooting

## `bin/lexengine` does not exist

The distribution is produced during Maven's `package` phase. `compile` and `test` generate example sources and compile classes, but do not package the CLI.

```sh
mvn package
# or
./scripts/build-cli.sh
```

Check that the build reached `lexengine-cli` successfully. When using `-pl`, select that module with `-am` to include its dependencies. A custom `lexengine.distributionDirectory` changes the output location.

## Java version or compiler errors

Messages such as `invalid target release: 21` indicate that Maven is running with an older JDK:

```sh
java -version
javac -version
mvn -version
```

Set `JAVA_HOME` to a JDK 21+ and rebuild. A Java runtime is sufficient to run the packaged CLI and compiled examples, while building the project or compiling newly generated Java requires a JDK.

## Dependencies cannot be resolved

Maven downloads plugins and libraries on the first build. An offline build works only after those artifacts have been cached.

Run without `-o` when setting up a fresh checkout. If your environment uses a Maven mirror, repository credentials or a proxy, configure Maven accordingly and check the failing artifact in the Maven output.

## A generated example class cannot be found

The example consumers reference generated classes. Build them with Maven so `generate-sources` runs before `compile`:

```sh
mvn -pl lexengine-examples -am clean test
```

The files should appear under `lexengine-examples/target/generated-sources/lexengine/`. In an IDE, reimport the Maven project after adding an example; Build Helper registers this generated source root.

Changes to a generated file are overwritten by the next generation step. Update the source specification under `src/main/resources/` instead.

## The launcher works only from one folder

Keep `lexengine` or `lexengine.cmd` beside `lex-engine.jar`. The bundle can be moved, but the launcher needs its adjacent JAR.

The launchers retain the current working directory. A relative grammar path is relative to the directory from which you invoked the command, even when the launcher itself is elsewhere. Use an absolute grammar path when appropriate and quote paths containing spaces.

## `LL(1) conflict`

Two alternatives select the same nonterminal/lookahead table entry. A common prefix is one cause:

```text
S -> a A | a B
```

Factor the grammar, use distinct initial tokens, or choose LR(1) if the grammar satisfies that algorithm's requirements. Left-recursion elimination does not perform general left factoring. See [parser algorithms](parsers.md).

## LR shift/reduce or reduce/reduce conflicts

The diagnostic identifies the state and lookahead. A shift/reduce conflict often means precedence or associativity has not been expressed in productions. A reduce/reduce conflict means more than one completed production can reduce on the same lookahead.

Use distinct precedence nonterminals such as `EXPR`, `TERM` and `FACTOR`, or remove ambiguous alternatives. Lex-Engine does not silently resolve conflicts by rule order.

## `No production for nonterminal ...`

An uppercase identifier is treated as a nonterminal. Add its production or quote it if you intend an uppercase terminal name. Token definitions alone do not create grammar productions.

Check naming and primes carefully: `EXPR` and `EXPR'` are different symbols.

## Unterminated grammar input or unexpected delimiters

Combined `.lg` rules require a final semicolon and balanced groups:

```text
ROOT -> '(' EXPR ')' $Grouped;
```

Literal punctuation is quoted. `(EXPR)` is an EBNF group; `'(' EXPR ')'` matches parentheses in the source language. The legacy format has different punctuation rules and one production per line.

## Empty-input or repetition errors

Use `ε` as a complete alternative:

```text
OPTION -> ident | ε;
```

A lexer rule that matches no characters cannot emit a progressing token. Likewise, an EBNF repeated expression must consume input, so `(ε)*` and repetitions of nullable groups are rejected.

## `No token matches at character ...`

A combined text lexer has no matching rule at that offset. Check the token patterns, punctuation and whitespace rules.

```text
plus: "\+"
TOKEN_IGNORE: "[ \t\r\n]+"
```

Whitespace is not skipped automatically. Combined token regexes use Java `Pattern`; standalone DFA `.spec` rules use the smaller regex language in the [lexer guide](lexer.md).

## A token is classified unexpectedly

The text lexer chooses the longest match across rules and uses declaration order for ties. Put a keyword before a general identifier when both can match the keyword text.

The parser classifies a token by grammar terminal name first, then by token value. A token named `ident` with value `if` still matches `ident` first if that name is a grammar terminal. Quoted grammar terminals do not create a value-only matching mode.

## The final token or trailing input is rejected

Successful parsing requires complete input consumption. `1 2` is not a single arithmetic expression in the arithmetic example, and `[1,]` has a comma with no following number.

Check the generated `ParserException` token index and expected terminals. A lexical error points to a character offset instead. Tokens supplied by an external lexer must have names and values compatible with the grammar; do not append a user EOF token.

## `Token` cannot be found while compiling a DFA scanner

Java actions in `.spec` files refer to a companion token class that you provide. Generate the scanner into the same package as that class, or use qualified names and a matching configured return type.

The [number-scanner walkthrough](lexer.md#a-complete-number-scanner) includes complete `Token.java` and consumer sources. The [legacy example](examples.md#5-standalone-dfa-lexer-and-legacy-parser) shows a token record that also implements a generated parser's token contract.

## Custom metadata or templates do not compile

Generation validates the grammar and emits source; it does not type-check custom Java members. Compile with `javac` or through Maven to see Java diagnostics.

Use a plain public class skeleton, keep generated runtime placeholders in custom templates, and avoid member/type names that collide with the generated API. See [grammar metadata](grammar.md#java-metadata) and [custom templates](generated-api.md#custom-parser-templates).

## Tests pass but the installed binary is old

Rebuild with `mvn package` or the builder script, then run the freshly generated `bin/lexengine --version`. If you previously copied a distribution elsewhere, copy the updated JAR and launchers there together.

`mvn verify` exercises the packaged executable and a relocated copy of its launcher. It is the recommended verification command before sharing a distribution.
