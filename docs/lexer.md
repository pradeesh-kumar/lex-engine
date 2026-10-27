# Lexer generation

Lex-Engine offers two ways to provide tokens to a parser:

| Facility | Input | Execution model | Typical use |
| --- | --- | --- | --- |
| Combined grammar text lexer | Token rules inside `.lg`. | Java regex matching over a source `String`. | A self-contained generated parser with a text constructor. |
| Standalone DFA scanner | Regex/action rules in `.spec`. | A minimized DFA reading from a `Reader`. | A separate scanner with custom token factories and a parser adapter. |

The [grammar guide](grammar.md) describes combined token rules. This guide focuses on the standalone DFA scanner.

## Lexical specification

Each non-comment line contains a quoted regex and a Java action:

```text
"[a-zA-Z_][a-zA-Z0-9_]*" { return Token.identifier(value); }
"[0-9]+" { return Token.number(value); }
"\+" { return Token.symbol("+", value); }
"[ \t\r\n]+" { /* skip whitespace */ }
```

Blank lines and lines starting with `#` are ignored. Actions are Java code inserted into the generated scanner. The current lexeme is available through `value` or `value()`.

An action that returns a token emits it. An action that completes without returning causes the scanner to continue, which is how whitespace is skipped. Keep each rule on one line.

## A complete number scanner

Create a specification:

```sh
mkdir -p target/lexer-demo
cat > target/lexer-demo/numbers.spec <<'SPEC'
"[0-9]+" { return Token.number(value); }
"[ \t\r\n]+" { /* skip whitespace */ }
SPEC
```

Generate the scanner:

```sh
./bin/lexengine lexer \
  --lex-spec-file target/lexer-demo/numbers.spec \
  --lexer-class NumberLexer \
  --lexer-package demo \
  --output-directory target/lexer-demo
```

Create the companion token class as `target/lexer-demo/Token.java`:

```java
package demo;

public record Token(String name, String value) {
  public static Token number(String value) {
    return new Token("num", value);
  }
}
```

Create `target/lexer-demo/Scan.java`:

```java
package demo;

import java.io.StringReader;

public class Scan {
  public static void main(String[] args) {
    var lexer = new NumberLexer(new StringReader("12 345\n6"));
    while (lexer.hasNext()) {
      Token token = lexer.nextToken();
      System.out.println(token.name() + "=" + token.value());
    }
  }
}
```

Compile and run:

```sh
javac -encoding UTF-8 -d target/lexer-demo/classes \
  target/lexer-demo/NumberLexer.java \
  target/lexer-demo/Token.java \
  target/lexer-demo/Scan.java
java -cp target/lexer-demo/classes demo.Scan
```

Expected output:

```text
num=12
num=345
num=6
```

The compiled scanner and token class do not require Lex-Engine at runtime.

## DFA regex features

| Construct | Meaning |
| --- | --- |
| `abc` | Literal sequence. |
| `[a-zA-Z_]` | Explicit character class and ranges. |
| `(a\|b)` | Group and alternation. |
| `*` | Zero or more occurrences of the preceding expression. |
| `+` | One or more occurrences. |
| `?` | Zero or one occurrence. |
| `.` | A wildcard over the collected lexer alphabet; it adds printable ASCII 32–126 to that alphabet. |
| `\n`, `\r`, `\t`, `\b`, `\f` | Supported control-character escapes. |
| `\+`, `\*`, `\(`, `\)` | Escaped literal metacharacters. |

Escape metacharacters when you intend to recognize punctuation. The DFA regex language does not implement the full Java `Pattern` language: bounded counts such as `{2,4}`, lookarounds, backreferences and Java shorthand classes such as `\d` are not supported. Use `[0-9]` in `.spec` files. Combined `.lg` token rules can use Java regex syntax.

The DFA's input alphabet is built from the characters and ranges in its rules. Inverted character classes operate over that finite alphabet. Explicit positive classes, as used by the examples, make the intended alphabet clear. The generated scanner consumes Java `char` values, rather than providing a Unicode-code-point scanning API.

## Matching and priority

The scanner follows DFA transitions and remembers accepting states so that it can return the accepted lexeme and retain lookahead for the next token. When several regex rules accept the same input, the earlier declared rule has priority.

Empty-matching lexical rules are rejected by `LexerGenerator`. A scanner token must consume characters to allow progress.

The alphabet is partitioned into disjoint inclusive ranges. A balanced index supports point lookup in logarithmic time and complete-partition queries in `O(log n + k)`. DFA minimization refines states by their token actions and transition signatures so states with different future behavior remain distinct.

## Generated scanner API

With default method naming and the supplied template:

```java
new NumberLexer(Reader input)
new NumberLexer(String sourcePath)

boolean hasNext()
Token peek()
Token nextToken()
String value()
int currentRow()
int currentCol()
```

The `String` constructor is a file path; use `StringReader` for source text. `peek()` makes the next token available without consuming it. `hasNext()` also buffers a token. `nextToken()` returns that token or scans the next one; at end of input it returns `null`.

The buffer supports lookahead, rollback and lexemes spanning read boundaries. Its reader is closed when EOF is reached. Applications that stop scanning early should manage the supplied reader's lifetime themselves.

Unrecognized input raises the generated `LexerException`. Row/column accessors report scanner positions; error messages can also identify invalid characters or unresolved symbols.

## Programmatic generation

```java
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.lexengine.commons.Options;
import org.lexengine.commons.Resources;
import org.lexengine.lexer.core.LexerGenerator;

var options = Options.builder()
    .lexerClassName("NumberLexer")
    .lexerPackageName("demo")
    .lexerMethodName("nextToken")
    .lexerReturnType("Token")
    .encoding(StandardCharsets.UTF_8)
    .lexSpecFile("numbers.spec")
    .outDir("target/generated-lexer")
    .build();

try (Reader source = Resources.reader(options.lexSpecFile(), options.encoding())) {
  var generated = new LexerGenerator(options, source).generate();
  generated.writeToDirectory(options.outDir());
}
```

Templates and specification locations are opened from the filesystem first and then the classpath, including resources in JARs. The return type and Java actions must agree with the token class available when compiling your generated source.

## Connect a scanner to a parser

A standalone scanner can produce tokens implementing a generated parser's `Lexer.Token` interface. An adapter translates `nextToken()` to the parser's `next()` method.

The example module supplies complete implementations:

- [Token factories](../lexengine-examples/src/main/java/org/lexengine/examples/lexer/Token.java).
- [Lexer adapter](../lexengine-examples/src/main/java/org/lexengine/examples/lexer/ArithmeticLexerAdapter.java).
- [Java consumer](../lexengine-examples/src/main/java/org/lexengine/examples/LexerParserExample.java).
- [Scanner rules](../lexengine-examples/src/main/resources/arithmetic.spec).

```sh
./bin/lexengine examples legacy "12 + count * (7 - 4)"
```

This scanner and its recursive-descent parser are generated during Maven's build, then packaged as compiled example classes.
