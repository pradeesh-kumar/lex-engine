/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.examples;

import org.lexengine.examples.generated.CanonicalParser;

/** This grammar is canonical LR(1), but merging its LR(0) state cores would lose lookaheads. */
public final class CanonicalLrExample {
  private CanonicalLrExample() {}

  public static CanonicalParser.ParseTree parse(String input) {
    var tokens =
        input
            .codePoints()
            .filter(c -> !Character.isWhitespace(c))
            .mapToObj(c -> new String(Character.toChars(c)))
            .map(name -> new CanonicalParser.Token(name, name))
            .toList();
    return new CanonicalParser(tokens).parse();
  }

  public static String format(String input) {
    return TreePrinter.format(
        parse(input).root(),
        CanonicalParser.Node::symbol,
        CanonicalParser.Node::label,
        CanonicalParser.Node::value,
        CanonicalParser.Node::children);
  }
}
