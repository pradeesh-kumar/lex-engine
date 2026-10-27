/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.examples;

import java.util.List;
import java.util.function.Function;
import org.lexengine.parser.core.ParserGenerator.ParserType;

/** A runnable catalog of feature examples, all generated at Maven build time. */
public final class ExampleCatalog {
  private ExampleCatalog() {}

  public record Example(
      String name,
      ParserType algorithm,
      String grammar,
      String description,
      String sample,
      Function<String, String> render) {}

  private static final List<Example> EXAMPLES =
      List.of(
          new Example(
              "arithmetic",
              ParserType.LR1,
              "arithmetic.lg",
              "Precedence, nested groups and labeled expression trees",
              "12 + 3 * (7 - 4)",
              ArithmeticExample::format),
          new Example(
              "list",
              ParserType.LL1,
              "list.lg",
              "Optional groups and repeated comma-separated numbers",
              "[12, 34, 56]",
              ListExample::format),
          new Example(
              "config",
              ParserType.RECURSIVE_DESCENT,
              "configuration.lg",
              "Assignments, comments, optional semicolons and nested lists",
              "# settings\nthreads = 8; enabled = true\nnames = [\"alpha\", \"beta\"];",
              ConfigurationExample::format),
          new Example(
              "json",
              ParserType.LR1,
              "json.lg",
              "Recursive objects and arrays, string escapes and numeric exponents",
              "{\"name\":\"Lex-Engine\",\"versions\":[21,25],\"enabled\":true,\"value\":null}",
              JsonExample::format),
          new Example(
              "legacy",
              ParserType.RECURSIVE_DESCENT,
              "legacy-expression.grammar + arithmetic.spec",
              "A generated DFA lexer adapted to a legacy expression parser",
              "12 + count * (7 - 4)",
              LexerParserExample::format),
          new Example(
              "lr1",
              ParserType.LR1,
              "canonical-lr1.grammar",
              "Canonical LR(1) lookaheads and explicit token-list input",
              "acd",
              CanonicalLrExample::format));

  public static List<Example> examples() {
    return EXAMPLES;
  }

  public static Example find(String name) {
    return EXAMPLES.stream()
        .filter(example -> example.name().equals(name))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unknown example: " + name + ". Use examples --list."));
  }
}
