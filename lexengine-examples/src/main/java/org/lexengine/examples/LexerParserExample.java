/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.examples;

import java.io.StringReader;
import org.lexengine.examples.generated.LegacyExpressionParser;
import org.lexengine.examples.lexer.ArithmeticLexerAdapter;

/** Connects a generated DFA lexer to a recursive-descent parser built from a legacy grammar. */
public final class LexerParserExample {
  private LexerParserExample() {}

  public static LegacyExpressionParser.ParseTree parse(String input) {
    return new LegacyExpressionParser(new ArithmeticLexerAdapter(new StringReader(input))).parse();
  }

  public static String format(String input) {
    return TreePrinter.format(
        parse(input).root(),
        LegacyExpressionParser.Node::symbol,
        LegacyExpressionParser.Node::label,
        LegacyExpressionParser.Node::value,
        LegacyExpressionParser.Node::children);
  }
}
