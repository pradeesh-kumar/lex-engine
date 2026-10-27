/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.examples;

import org.lexengine.examples.generated.JsonParser;

/** A typed example backed by Java source generated and compiled during the Maven build. */
public final class JsonExample {
  private JsonExample() {}

  public static JsonParser.ParseTree parse(String input) {
    return new JsonParser(input).parse();
  }

  public static String format(String input) {
    return TreePrinter.format(
        parse(input).root(),
        JsonParser.Node::symbol,
        JsonParser.Node::label,
        JsonParser.Node::value,
        JsonParser.Node::children);
  }
}
