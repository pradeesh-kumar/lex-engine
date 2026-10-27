# Grammar specification

Lex-Engine supports a combined `.lg` format containing Java metadata, token definitions and EBNF, and an original `.grammar` format containing metadata properties and production rules.

## A complete combined specification

```text
package example;
public class NumberListParser {}
---
num: "[0-9]+"
Punctuation: { open: "\[", close: "\]", comma: "," }
TOKEN_IGNORE: "[ \t\r\n]+"
---
LIST -> '[' (num (',' num)*)? ']' $NumberList;
```

Exactly two separator lines divide the three sections. A separator is a line containing `---`, optionally surrounded by whitespace or followed by a `#` comment. Grammar rules end with `;` and can span lines.

Generate it with:

```sh
./bin/lexengine parser --grammar language.lg --parser-type LL1 --output-directory target/generated
```

## Java metadata

The first section contains a plain public Java class declaration, optionally `final`, with an optional package, imports and custom members:

```java
package example;

import java.util.Map;

public class SettingsParser {
  public String languageName() { return "settings"; }
}
```

Generated runtime and parser members are inserted at the beginning of that class body. The package, imports, existing members and closing portion of the compilation unit are preserved. Class declarations in Java comments and string literals are ignored when locating the class body.

Use a plain class skeleton: inheritance clauses and generic class headers are not part of the supported metadata syntax. Parser class names must be valid Java identifiers and must not collide with generated runtime types such as `Lexer`, `Token`, `Node`, `ParseTree`, `ParserException`, `ListLexer`, `TextLexer`, `Builder` or `Work`. Custom members must avoid signatures and fields belonging to the generated runtime.

Full-line `#` comments are allowed in metadata. Use normal Java comments within Java code.

## Token definitions

A token has a name and a regex:

```text
ident: "[a-zA-Z_][a-zA-Z0-9_]*"
num: "[0-9]+"
```

A group collects related named tokens:

```text
Operator: {
  plus: "\+",
  minus: "-",
  multiply: "\*",
  divide: "/"
}
```

The token's name is `plus`, `minus`, and so on; the group name organizes definitions. Token names must be unique. Use lowercase names when referencing tokens directly in grammar productions, because uppercase identifiers denote nonterminals.

`TOKEN_IGNORE` rules consume input without emitting a token. Both scalar and grouped forms are supported:

```text
TOKEN_IGNORE: "[ \t\r\n]+"
```

```text
TOKEN_IGNORE: {
  whitespace: "[ \t\r\n]+",
  comment: "#[^\r\n]*"
}
```

A group can also contain unnamed quoted ignore patterns, as in `TOKEN_IGNORE: { "[ \t]+" }`.

### Regex and matching rules

Combined-file token rules use Java's `java.util.regex.Pattern` syntax. Quoted regex strings preserve sequences such as `\d`, `\+`, `\n` and `\t`. Escaped quotes and doubled backslashes are decoded before the pattern is compiled. This is separate from the DFA regex engine used by `.spec` files.

At each input position, the text lexer tries all token rules and chooses the longest match across rules. Declaration order breaks ties. Java regex ordering and greediness apply within each rule; use separate rules or put longer alternatives first for prefix-overlapping alternations.

For example, a keyword before an identifier wins when both match `if`, while the identifier wins for `iffy` because it matches more characters:

```text
keyword: "if"
ident: "[a-z]+"
```

No whitespace is skipped implicitly. Define `TOKEN_IGNORE` patterns for the whitespace and comments your language accepts. Patterns that match empty input are rejected; a context-dependent zero-length match is also rejected at runtime.

## Productions and symbols

```text
EXPR -> TERM (('+' | '-') TERM)* $Expression;
TERM -> FACTOR (('*' | '/') FACTOR)* $Term;
FACTOR -> ident $Identifier
        | num $Number
        | '(' EXPR ')' $Grouped;
```

The left-hand side of the first production is the start symbol. A nonterminal must have a production; undefined references fail validation.

| Form | Meaning |
| --- | --- |
| `EXPR`, `TERM_2`, `EXPR'` | Uppercase nonterminal identifiers, with optional digits, underscores and trailing primes. |
| `num`, `ident` | Terminal token names. |
| `'+'`, `'if'`, `'('` | Quoted terminals, including symbols that otherwise have grammar meaning. |
| `A B C` | Sequence. |
| `A \| B` | Alternatives. |
| `(A \| B)` | Nested EBNF group. |
| `A?` | Zero or one occurrence. |
| `A*` | Zero or more occurrences. |
| `A+` | One or more occurrences. |
| `ε` | Empty alternative. |
| `$Label` | Name attached to that alternative's parse-tree node. |

Quoted terminals can match a token name or its value. The runtime checks token names before falling back to values; quoting does not introduce a separate token-value-only matching mode. This lets a token named `plus` with value `+` satisfy the grammar terminal `'+'`.

The terminal name `$end` is reserved for end of input.

## EBNF lowering

Groups, optional elements and repetition are lowered into ordinary productions with helper nonterminals named `EBNF_*`. For example:

```text
LIST -> '[' (num (',' num)*)? ']' $NumberList;
```

The optional group permits `[]`. The inner repetition permits `[1]`, `[1, 2]` and longer lists. A trailing comma is rejected because every comma must be followed by `num`.

Repeated expressions must consume input. `(ε)*` and `(A?)+` when `A?` can be empty are rejected. Allowing a repeated element to match no input would make progress ambiguous.

Helper nodes appear in the resulting parse tree. See [generated APIs](generated-api.md) for the distinction between original labels and helper structure.

## Epsilon and labels

Epsilon must be the only symbol in its alternative:

```text
OPTION -> ident $Present | ε $Absent;
```

Use `ε` explicitly instead of an empty alternative. A label must be the final item before `|` or `;`:

```text
VALUE -> num $Number | ident $Identifier;
```

Labels are metadata used in `Node.label()`. They do not execute Java actions or create separate Java classes. An unlabeled production uses `DefaultLabel`; terminal leaves have no alternative label.

Repeated definitions append alternatives:

```text
VALUE -> num $Number;
VALUE -> ident $Identifier;
```

## Comments and quoting

In token and grammar sections, `#` starts a comment outside quoted strings. A token pattern `"#"` and a grammar terminal `'#'` retain their hashes. Quoted terminals support escaped quotes and backslashes, plus `\n`, `\r` and `\t`.

Quote punctuation with grammar meaning. For example, use `'(' EXPR ')'` for literal parentheses; use `(EXPR)` for an EBNF group.

## Legacy two-section format

```text
class=LegacyExpressionParser
package=example
---
EXPR -> EXPR + TERM | TERM
TERM -> ident | num | ( EXPR )
```

The metadata properties are `class` and `package`. When omitted, the defaults are `Parser` and `org.lexengine.parser.generated`. The first production is the start symbol. Each production occupies one line; `|` separates alternatives, uppercase identifiers are nonterminals, and `ε` denotes an empty alternative.

Legacy parentheses are literal tokens. This format does not supply automatic text tokenization, EBNF suffix operators or alternative labels. Supply token objects or a generated `Lexer` adapter when using the resulting parser.

The [legacy example](examples.md#5-standalone-dfa-lexer-and-legacy-parser) combines this format with a standalone DFA scanner and shows direct-left-recursion elimination in the generated recursive-descent parser.

## Validation boundaries

Syntax, symbol references, empty-matching lexer rules and nullable EBNF repetitions are validated before source emission. Predictive and LR table conflicts are validated by their generators. Custom Java metadata and custom source templates still need to compile with `javac`; source generation does not type-check custom Java members.
