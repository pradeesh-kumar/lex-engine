# Parser algorithms

All three generators consume the same validated grammar model and emit standalone Java with the same `ParseTree`, `Node`, `Token` and `Lexer` API. Their parsing strategy and the grammars they can accept differ.

| Type | Generated implementation | Grammar preparation | Conflict reporting |
| --- | --- | --- | --- |
| `RECURSIVE_DESCENT` | A predictive method per nonterminal. | Direct and ordered indirect left-recursion elimination. | LL(1) table conflicts by nonterminal and lookahead. |
| `LL1` | An iterative work stack and predictive table. | The same recursion elimination. | The same LL(1) conflict checks. |
| `LR1` | An iterative shift/reduce parser using canonical LR(1) states. | Preserves the original productions and left recursion. | Shift/reduce or reduce/reduce conflicts by state and lookahead. |

Use `--parser-type` when generating a parser:

```sh
./bin/lexengine parser --grammar language.lg --parser-type LR1 --output-directory target/generated
```

## Shared grammar analysis

A grammar consists of productions, alternatives, terminal/nonterminal symbols and a start symbol. Epsilon is stored as an empty alternative. The grammar validates references before source generation.

FIRST and FOLLOW sets are computed to a fixed point, so cyclic dependencies do not require recursive set computation:

- `FIRST(A)` contains terminals that can begin a string derived from `A`, and epsilon when `A` is nullable.
- `FOLLOW(A)` contains terminals that can follow `A` in a derivation.
- The start symbol's FOLLOW set contains the reserved end-of-input terminal `$end`.
- For a sequence, FIRST proceeds past nullable prefixes and stops when a symbol must consume input.

The predictive table places `A -> α` under terminals in `FIRST(α)`. If `α` is nullable, it also places that alternative under `FOLLOW(A)`. Two alternatives assigned to the same table entry are a conflict.

## Recursive descent

Recursive descent emits one Java method for each nonterminal. A method examines the next terminal, selects an alternative, consumes terminal leaves, invokes methods for child nonterminals, and returns an immutable node.

This is useful when you want generated control flow that can be inspected method by method. Its generated methods use the same conflict-checked predictive table as LL(1); they do not backtrack through alternatives.

The configuration example uses recursive descent:

```sh
./bin/lexengine examples config --input-file lexengine-examples/src/main/resources/samples/settings.conf
```

### Left-recursion elimination

A directly left-recursive grammar:

```text
EXPR -> EXPR + TERM | TERM
```

is transformed into the equivalent shape:

```text
EXPR  -> TERM EXPR'
EXPR' -> + TERM EXPR' | ε
```

The transformation also substitutes earlier nonterminals when eliminating ordered indirect recursion, and chooses fresh primed names to avoid collisions. A rule without a terminating alternative is rejected by the transformation.

Parse trees expose the transformed productions: `EXPR'` nodes are present, and the tree shape can differ from LR(1) output for the original left-recursive grammar. Alternative labels are retained on the productions they accompany.

The transformation does not perform general left factoring or turn every context-free grammar into an LL(1) grammar. Grammars that still conflict are rejected. The [legacy lexer/parser example](examples.md#5-standalone-dfa-lexer-and-legacy-parser) demonstrates this transformation with real scanner tokens.

## Table-driven LL(1)

The LL(1) generator uses a stack of pending symbols and node builders. It looks up each nonterminal/terminal pair in a predictive table, pushes the selected right-hand side, and closes nodes when their children are complete.

The parsing loop is iterative. Epsilon creates a node with no children, and input must be exhausted when the start production finishes.

The number-list example is built as LL(1):

```sh
./bin/lexengine examples list "[1, 2, 3]"
./bin/lexengine examples list "[]"
```

Its optional list body is selected using the closing bracket in FOLLOW when the input is empty.

### LL(1) conflict example

```text
S -> a A | a B
A -> b
B -> c
```

Both alternatives for `S` begin with `a`, so one lookahead token cannot select an alternative. Generation reports an `LL(1) conflict` for `S` on `a`.

A factored form is:

```text
S -> a T
T -> b | c
```

Common-prefix factoring is a grammar design choice; Lex-Engine does not silently choose one conflicting alternative.

## Canonical LR(1)

LR(1) builds an augmented grammar, canonical item sets, state transitions, an ACTION table and a GOTO table. An item contains:

1. A production.
2. A dot position showing how much of its right-hand side has been seen.
3. A lookahead terminal on which a completed item may reduce.

Closure expands a nonterminal after the dot using FIRST of the remaining suffix followed by the item's lookahead. GOTO advances the dot across a symbol and closes the resulting item set. States retain complete LR(1) items, including their lookaheads.

The runtime uses stacks of states and nodes:

- **Shift:** consume the current token, push its leaf node and the next state.
- **Reduce:** pop a production's children, create its labeled node, and follow GOTO for the left-hand side.
- **Accept:** return the start node when the augmented production is complete at end of input.

The arithmetic and JSON examples use this algorithm:

```sh
./bin/lexengine examples arithmetic "1 + 2 * (3 - 4)"
./bin/lexengine examples json '{"items":[1,true,null]}'
```

### A grammar that needs distinct LR(1) lookaheads

```text
S -> a A d | b A e | a B e | b B d
A -> c
B -> c
```

This grammar accepts `acd`, `bce`, `ace` and `bcd`. Canonical LR(1) keeps the relevant lookaheads separate. Merging states that have the same LR(0) cores would introduce reduce/reduce conflicts.

```sh
./bin/lexengine examples lr1 acd
./bin/lexengine examples lr1 bce
```

The [canonical example source](../lexengine-examples/src/main/java/org/lexengine/examples/CanonicalLrExample.java) supplies an explicit list of tokens to the generated parser.

### LR conflicts and precedence

A grammar such as:

```text
EXPR -> EXPR + EXPR | EXPR * EXPR | num
```

does not state whether to shift or reduce when operators overlap. Generation rejects the conflicting table entry. Likewise, `S -> A | B`, `A -> a`, `B -> a` produces a reduce/reduce conflict.

Express precedence with separate nonterminals instead:

```text
EXPR -> TERM (('+' | '-') TERM)*;
TERM -> FACTOR (('*' | '/') FACTOR)*;
FACTOR -> num | '(' EXPR ')';
```

No associativity or precedence directives are implemented. Parse trees preserve grammar structure and labels; an application can derive its semantic AST from them.

## Choosing an implementation

Use recursive descent when per-nonterminal methods are helpful for understanding the output. Use LL(1) for the same predictive grammar restrictions with an iterative parsing stack. Use canonical LR(1) for grammars that need its broader lookahead-state construction or when preserving left-recursive productions matters.

Canonical state sets and dense tables can grow substantially for large grammars. Predictive methods can consume the Java call stack on deeply recursive input; LL(1) and LR(1) use explicit stacks. Generated Java remains subject to normal Java compiler and class-file size limits.

## Generation details

After `generate()`, the Java API exposes:

```java
var details = generator.details();
System.out.println(details.sourceFile());
System.out.println(details.type());
System.out.println(details.productionCount());
System.out.println(details.stateCount());
```

`stateCount()` is the canonical state count for LR(1), and zero for predictive generators. Calling `details()` before generation raises an `IllegalStateException`.
