/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.examples.lexer;

import java.io.Reader;
import org.lexengine.examples.generated.LegacyExpressionParser;

/** Adapts the standalone DFA scanner to the generated parser's token contract. */
public final class ArithmeticLexerAdapter implements LegacyExpressionParser.Lexer {
  private final ArithmeticLexer lexer;

  public ArithmeticLexerAdapter(Reader input) {
    lexer = new ArithmeticLexer(input);
  }

  @Override
  public boolean hasNext() {
    return lexer.hasNext();
  }

  @Override
  public Token peek() {
    return lexer.peek();
  }

  @Override
  public Token next() {
    return lexer.nextToken();
  }
}
