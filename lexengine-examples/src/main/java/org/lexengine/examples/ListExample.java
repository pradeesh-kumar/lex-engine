/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.examples;

import org.lexengine.examples.generated.ListParser;

/** A typed example backed by Java source generated and compiled during the Maven build. */
public final class ListExample {
  private ListExample() {}

  public static ListParser.ParseTree parse(String input) {
    return new ListParser(input).parse();
  }

  public static String format(String input) {
    return TreePrinter.format(
        parse(input).root(),
        ListParser.Node::symbol,
        ListParser.Node::label,
        ListParser.Node::value,
        ListParser.Node::children);
  }
}
