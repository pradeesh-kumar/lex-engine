package org.lexengine.parser.core;

public interface Lexer {

  boolean hasNext();

  Token peek();

  Token next();
}
