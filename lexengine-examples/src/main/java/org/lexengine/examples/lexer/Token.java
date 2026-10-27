/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.examples.lexer;

import org.lexengine.examples.generated.LegacyExpressionParser;

/** Token factories used by the Java actions in arithmetic.spec. */
public record Token(String name, String value) implements LegacyExpressionParser.Lexer.Token {
  public static Token number(String value) {
    return new Token("num", value);
  }

  public static Token identifier(String value) {
    return new Token("ident", value);
  }

  public static Token symbol(String name, String value) {
    return new Token(name, value);
  }
}
