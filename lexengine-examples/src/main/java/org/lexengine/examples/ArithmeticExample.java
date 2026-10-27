/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.examples;

import org.lexengine.examples.generated.ArithmeticParser;

/** A typed example backed by Java source generated and compiled during the Maven build. */
public final class ArithmeticExample {
  private ArithmeticExample() {}

  public static ArithmeticParser.ParseTree parse(String input) {
    return new ArithmeticParser(input).parse();
  }

  public static String format(String input) {
    return TreePrinter.format(
        parse(input).root(),
        ArithmeticParser.Node::symbol,
        ArithmeticParser.Node::label,
        ArithmeticParser.Node::value,
        ArithmeticParser.Node::children);
  }
}
