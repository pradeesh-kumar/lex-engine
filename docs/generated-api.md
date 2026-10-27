# Generated APIs, parse trees and integration

Generated parsers are standalone Java 21+ classes. Each generated parser contains its own runtime types; an application can compile that source and use it without adding the generator modules to its runtime classpath.

## Parser constructors

For a generated class named `ListParser`:

```java
ListParser(ListParser.Lexer lexer)
ListParser(java.util.List<? extends ListParser.Lexer.Token> tokens)
ListParser(String source) // combined .lg grammars only
```

The text constructor uses the token rules from the combined grammar. Legacy grammars accept a token list or lexer adapter. Create a new parser for each input; the parsing state and token position are consumed by `parse()`.

```java
var parser = new ListParser("[12, 34, 56]");
ListParser.ParseTree tree = parser.parse();
```

A successful parse consumes the complete input. Invalid or trailing tokens raise the generated `ParserException`.

## Tree types

```java
public record ParseTree(Node root) {}
public record Node(
    String symbol,
    String label,
    String value,
    java.util.List<Node> children) {}
```

| Field | Nonterminal node | Terminal leaf |
| --- | --- | --- |
| `symbol()` | The grammar nonterminal name. | The matched grammar terminal name. |
| `label()` | The alternative's `$Label`, or `DefaultLabel`. | `null`. |
| `value()` | `null`. | The original token value. |
| `children()` | Ordered child nodes; empty for epsilon. | Empty. |

Children are copied into immutable lists. Labels identify alternatives and do not evaluate expressions or execute semantic actions. Build an application-specific AST or evaluator by visiting these nodes.

### Inspect a tree

```java
var root = new ListParser("[1, 2]").parse().root();
System.out.println(root.symbol());   // LIST
System.out.println(root.label());    // NumberList
System.out.println(root.children());
```

### Traverse terminal values

The following example preserves input order without requiring recursive traversal:

```java
var pending = new java.util.ArrayDeque<ListParser.Node>();
var values = new java.util.ArrayList<String>();
pending.push(root);
while (!pending.isEmpty()) {
  var node = pending.pop();
  if (node.value() != null) values.add(node.value());
  for (int i = node.children().size() - 1; i >= 0; i--) {
    pending.push(node.children().get(i));
  }
}
System.out.println(values); // Token values include "[", "1", ",", "2", "]".
```

For formatted output, the example module's [TreePrinter](../lexengine-examples/src/main/java/org/lexengine/examples/TreePrinter.java) accepts method references for any generated node type:

```java
String formatted = TreePrinter.format(
    root,
    ListParser.Node::symbol,
    ListParser.Node::label,
    ListParser.Node::value,
    ListParser.Node::children);
```

`TreePrinter` is an example utility. It is not a dependency of generated parsers.

### Helper nodes and transformed grammars

EBNF groups and suffix operators introduce `EBNF_*` nonterminals. Predictive generation may introduce primed nonterminals when removing left recursion. These helper nodes are visible in the tree, and usually have `DefaultLabel`.

Tree shape can differ across algorithms when one uses transformed productions and another preserves left recursion. Original labels identify the relevant productions; an AST conversion can ignore or flatten helper nodes according to the language's semantics.

## Explicit token input

Every parser exposes a convenience record:

```java
public record Token(String name, String value) implements Lexer.Token {}
```

For the list grammar:

```java
var tokens = java.util.List.of(
    new ListParser.Token("open", "["),
    new ListParser.Token("num", "12"),
    new ListParser.Token("close", "]"));
var tree = new ListParser(tokens).parse();
```

Terminal classification checks token names first, then token values. `num` matches by name, while `'['` can match the value of a token named `open`. The reserved end marker is supplied internally; callers do not append an EOF token.

The [canonical LR example](../lexengine-examples/src/main/java/org/lexengine/examples/CanonicalLrExample.java) uses explicit tokens with a legacy grammar.

## External lexer input

The generated interface is:

```java
public interface Lexer {
  boolean hasNext();
  Token peek();
  Token next();

  interface Token {
    String name();
    String value();
  }
}
```

A scanner can use a record implementing that token contract. `next()` must return a non-null token while `hasNext()` is true. The parser buffers lookahead internally and relies on `hasNext()` and `next()` to advance.

The example module adapts a standalone DFA scanner to `LegacyExpressionParser.Lexer`:

```java
public final class ArithmeticLexerAdapter implements LegacyExpressionParser.Lexer {
  private final ArithmeticLexer lexer;

  public ArithmeticLexerAdapter(java.io.Reader input) {
    lexer = new ArithmeticLexer(input);
  }

  public boolean hasNext() { return lexer.hasNext(); }
  public Token peek() { return lexer.peek(); }
  public Token next() { return lexer.nextToken(); }
}
```

Its companion `Token` implements `LegacyExpressionParser.Lexer.Token`. See the [complete adapter source](../lexengine-examples/src/main/java/org/lexengine/examples/lexer/ArithmeticLexerAdapter.java).

## Programmatic parser generation

Generator code uses the parser, lexer and commons modules at development/build time:

```java
import java.nio.file.Path;
import org.lexengine.parser.core.ParserGenerator;

var generator = ParserGenerator.Factory.create(
    Path.of("language.lg").toFile(),
    ParserGenerator.ParserType.LR1,
    Path.of("target/generated"),
    null); // use the built-in source template

generator.generate();
System.out.println(generator.details().sourceFile());
```

The explicit output/template overload avoids relying on the static CLI options. Pass a template `Path` instead of `null` to customize emission. `grammarSpec()` exposes the parsed specification; `details()` is available after generation.

Generated source compilation is a separate step. The example module shows how Maven performs it automatically; applications can also invoke `javac` directly.

## Errors

There are two main stages:

| Stage | Error type | Typical cause |
| --- | --- | --- |
| Generation | `org.lexengine.commons.error.GeneratorException` | Invalid grammar, unknown nonterminal, invalid regex, conflicting table entry, template or output failure. |
| Generated runtime | The generated class's `ParserException` | Unexpected or trailing tokens, no matching text-lexer rule, or zero-length token match. |

A standalone DFA scanner exposes its own generated `LexerException`.

Parser syntax errors identify a token index and expected terminals. Text-lexer errors identify the character offset. Source diagnostics include line/column information where available; model validation and I/O errors may not have a source position. `GeneratorException` exposes `errorType()`, `line()` and `column()`.

## Custom parser templates

The built-in template is [recursive-descent-parser-class.template](../lexengine-parser/src/main/resources/recursive-descent-parser-class.template), shared by all parser types:

```text
${classHeader}
${runtimeMembers}
${parserMembers}
${lexerMembers}
${classFooter}
```

| Placeholder | Content |
| --- | --- |
| `classHeader` | Package/class prefix, or combined metadata up to the parser class body. |
| `runtimeMembers` | Tokens, lexer contract, immutable tree types, buffering and syntax errors. |
| `parserMembers` | Grammar arrays and algorithm-specific methods/tables. |
| `lexerMembers` | Combined-file text lexer and its constructor; empty for legacy grammars. |
| `classFooter` | Closing class text and preserved custom members from combined metadata. |

A template can add comments or change layout around these sections. Keep the runtime and parser sections together for a working standalone class. Missing model placeholders raise a generation error, and custom Java additions must compile normally. Template replacement preserves dollar signs and backslashes in inserted Java source.
