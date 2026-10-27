# Example walkthroughs

The `lexengine-examples` module contains specifications, typed Java consumers, input files and tests. Maven generates and compiles the parsers and DFA scanner before compiling those consumers. Running an example uses the compiled classes and does not invoke `javac`.

```sh
mvn clean verify
./bin/lexengine examples --list
./bin/lexengine examples --all
```

Every named example accepts one input string or `--input-file <path>`. Without input it uses its built-in sample. Successful runs print the selected algorithm followed by a labeled tree. Invalid input returns status `1` and a diagnostic on standard error.

| Name | Algorithm | Specification | Typed Java consumer |
| --- | --- | --- | --- |
| `arithmetic` | LR(1) | [arithmetic.lg](../lexengine-examples/src/main/resources/arithmetic.lg) | [ArithmeticExample](../lexengine-examples/src/main/java/org/lexengine/examples/ArithmeticExample.java) |
| `list` | LL(1) | [list.lg](../lexengine-examples/src/main/resources/list.lg) | [ListExample](../lexengine-examples/src/main/java/org/lexengine/examples/ListExample.java) |
| `config` | Recursive descent | [configuration.lg](../lexengine-examples/src/main/resources/configuration.lg) | [ConfigurationExample](../lexengine-examples/src/main/java/org/lexengine/examples/ConfigurationExample.java) |
| `json` | LR(1) | [json.lg](../lexengine-examples/src/main/resources/json.lg) | [JsonExample](../lexengine-examples/src/main/java/org/lexengine/examples/JsonExample.java) |
| `legacy` | Recursive descent + DFA lexer | [legacy-expression.grammar](../lexengine-examples/src/main/resources/legacy-expression.grammar), [arithmetic.spec](../lexengine-examples/src/main/resources/arithmetic.spec) | [LexerParserExample](../lexengine-examples/src/main/java/org/lexengine/examples/LexerParserExample.java) |
| `lr1` | Canonical LR(1) | [canonical-lr1.grammar](../lexengine-examples/src/main/resources/canonical-lr1.grammar) | [CanonicalLrExample](../lexengine-examples/src/main/java/org/lexengine/examples/CanonicalLrExample.java) |

## 1. Arithmetic expressions

```sh
./bin/lexengine examples arithmetic "12 + 3 * (7 - 4)"
./bin/lexengine examples arithmetic --input-file lexengine-examples/src/main/resources/samples/arithmetic.txt
```

The grammar separates additive, multiplicative and factor productions:

```text
EXPR -> TERM (('+' | '-') TERM)* $Expression;
TERM -> FACTOR (('*' | '/') FACTOR)* $Term;
FACTOR -> ident $Identifier | num $Number | '(' EXPR ')' $Grouped;
```

It demonstrates operator precedence, nested EBNF groups, repeated operations, token-name references and literal punctuation. Numeric and identifier leaves preserve their original text. Labels identify expression, term, identifier, number and grouped-expression alternatives.

Use the typed API:

```java
var tree = ArithmeticExample.parse("12 + 3 * (7 - 4)");
System.out.println(tree.root().label()); // Expression
```

Try `1 + )` or `1 2` to see syntax and trailing-input rejection. This example recognizes expression structure; it does not evaluate variable names or arithmetic values. Convert its tree to an AST or evaluator in your application.

## 2. Optional number lists

```sh
./bin/lexengine examples list "[]"
./bin/lexengine examples list "[1]"
./bin/lexengine examples list "[1, 2, 3]"
```

```text
LIST -> '[' (num (',' num)*)? ']' $NumberList;
```

The optional body permits an empty list. The repeated comma/number sequence permits multiple elements. The LL(1) parser uses the closing bracket to select the nullable branch.

```java
var tree = ListExample.parse("[12, 34]");
System.out.println(tree.root().symbol()); // LIST
System.out.println(tree.root().label());  // NumberList
```

`[1,]`, `[1 2]` and a missing closing bracket are rejected. `EBNF_*` nodes in the tree show the helper productions created for the optional group and repetition.

## 3. Configuration assignments

```sh
./bin/lexengine examples config 'threads = 8; enabled = true'
./bin/lexengine examples config --input-file lexengine-examples/src/main/resources/samples/settings.conf
```

```text
CONFIG -> ENTRY* $Configuration;
ENTRY -> name '=' VALUE ';'? $Assignment;
VALUE -> boolean $Boolean | number $Number | string $String | LIST $List;
LIST -> '[' (VALUE (',' VALUE)*)? ']' $List;
```

This recursive-descent example combines:

- Zero or more assignments, including an empty document.
- Optional semicolons.
- Booleans, signed numbers, quoted strings and nested heterogeneous lists.
- Whitespace and `#` comments through grouped `TOKEN_IGNORE` rules.

```java
var tree = ConfigurationExample.parse("a = [true, [1, 2]]; b = \"value\"");
System.out.println(tree.root().label()); // Configuration
```

The configuration string rule accepts simple quoted text without embedded quotes or newlines. The JSON example demonstrates escaped strings. The example recognizes assignment syntax; checking duplicate keys or converting values into application settings is an application step.

Try `threads 8`, `a = [1,]` or an unterminated quoted value to see validation fail.

## 4. Recursive JSON values

```sh
./bin/lexengine examples json '{"items":[1,true,null],"name":"Lex-Engine"}'
./bin/lexengine examples json --input-file lexengine-examples/src/main/resources/samples/document.json
```

This grammar recognizes objects, arrays, strings, numbers, booleans and null. The start symbol is `VALUE`, so primitive top-level values such as `null`, `true` and `-1.25e+3` also work.

```text
VALUE -> OBJECT $Object | ARRAY $Array | string $String | number $Number
       | 'true' $True | 'false' $False | 'null' $Null;
OBJECT -> '{' (MEMBER (',' MEMBER)*)? '}' $Object;
MEMBER -> string ':' VALUE $Member;
ARRAY -> '[' (VALUE (',' VALUE)*)? ']' $Array;
```

The token definitions demonstrate Java regex handling for quoted escapes, `\u` hexadecimal escapes, signed numbers, decimals and exponents. String token values retain their quoted source lexemes; the example does not deserialize them into Java objects.

```java
var tree = JsonExample.parse("{\"x\":[1,false,null]}");
System.out.println(tree.root().label()); // Object
```

`[1,]`, `{"x":}`, `01`, an unescaped newline inside a string, and two adjacent top-level values are rejected.

## 5. Standalone DFA lexer and legacy parser

```sh
./bin/lexengine examples legacy "12 + count * (7 - 4)"
```

The parser specification uses the legacy format and direct left recursion:

```text
EXPR -> EXPR + TERM | EXPR - TERM | TERM
TERM -> TERM * FACTOR | TERM / FACTOR | FACTOR
FACTOR -> ident | num | ( EXPR )
```

A separate `.spec` file generates `ArithmeticLexer`. Its Java actions call token factories:

```text
"[0-9]+" { return Token.number(value); }
"\+" { return Token.symbol("+", value); }
```

The [Token record](../lexengine-examples/src/main/java/org/lexengine/examples/lexer/Token.java) implements `LegacyExpressionParser.Lexer.Token`. [ArithmeticLexerAdapter](../lexengine-examples/src/main/java/org/lexengine/examples/lexer/ArithmeticLexerAdapter.java) adapts the scanner's `nextToken()` method to the parser's `next()` method.

```java
var lexer = new ArithmeticLexerAdapter(new java.io.StringReader("12 + count"));
var tree = new LegacyExpressionParser(lexer).parse();
```

This pipeline demonstrates streaming scanner input, custom token factories, an external lexer adapter and left-recursion elimination. Primed helper nonterminals appear in the resulting predictive tree. `12 + * 3` is rejected.

## 6. Canonical LR(1) lookahead

```sh
./bin/lexengine examples lr1 acd
./bin/lexengine examples lr1 bce
./bin/lexengine examples lr1 ace
./bin/lexengine examples lr1 bcd
```

```text
S -> a A d | b A e | a B e | b B d
A -> c
B -> c
```

The Java consumer converts input characters into explicit `CanonicalParser.Token` objects. Whitespace is ignored by that example's input adapter, so `a c d` is also accepted.

This grammar requires distinct canonical LR(1) lookaheads: merging LR(0) state cores would produce reduce/reduce conflicts. It is a compact way to exercise the LR implementation without a text lexer.

```java
var tree = CanonicalLrExample.parse("acd");
System.out.println(tree.root().symbol()); // S
```

`acc`, `ac` and extra trailing symbols are rejected.

## Modify or add an example

1. Add or edit a specification under `lexengine-examples/src/main/resources/`.
2. Add a generation invocation in the module's [POM](../lexengine-examples/pom.xml), selecting the parser algorithm or DFA scanner settings.
3. Add a typed consumer under `src/main/java/org/lexengine/examples/`.
4. Register a runnable demo in [ExampleCatalog](../lexengine-examples/src/main/java/org/lexengine/examples/ExampleCatalog.java).
5. Add accepted and rejected inputs to the module's tests.
6. Run `mvn clean verify`.

Generated `.java` files belong under `target/generated-sources/lexengine/`; Maven compiles them and includes the resulting classes in the example JAR and CLI distribution. They are regenerated from specifications and are not checked into source control.
