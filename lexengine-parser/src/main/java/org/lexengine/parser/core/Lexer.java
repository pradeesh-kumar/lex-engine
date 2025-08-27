/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

public interface Lexer {

  boolean hasNext();

  Token peek();

  Token next();

  interface Token {
    String name();

    String value();
  }
}
