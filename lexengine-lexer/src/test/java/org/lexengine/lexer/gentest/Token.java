/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.gentest;

public class Token {

  public enum Type {
    PACKAGE,
    IMPORT,
    PUBLIC,
    CLASS,
    PRIVATE,
    STATIC,
    FINAL,
    INT,
    IF,
    THROW,
    THIS,
    NEW,
    STRING,
    INTEGER,
    OPERATOR,
    IDENTIFIER,
    MUL,
    CLOSE_PAREN,
    OR,
    SEMICOLON,
    GREATEREQ,
    PERCENTAGE,
    ADD,
    GREATER,
    DOUBLE_OR,
    DOT,
    EQ,
    OPEN_BRACE,
    LESSEQ,
    SUB,
    LESS,
    CLOSE_BRACE,
    DIV,
    OPEN_PAREN,
    COMMENT
  }

  private String strVal;
  private Integer intVal;
  private Type type;

  public static Token identifier(String identifier) {
    Token token = new Token();
    token.strVal = identifier;
    token.type = Type.IDENTIFIER;
    return token;
  }

  public static Token integer(String integer) {
    Token token = new Token();
    token.type = Type.INTEGER;
    token.intVal = Integer.parseInt(integer);
    return token;
  }

  public static Token string(String string) {
    Token token = new Token();
    token.type = Type.STRING;
    token.strVal = string;
    return token;
  }

  public static Token of(Type type) {
    Token token = new Token();
    token.type = type;
    return token;
  }

  public static Token comment() {
    Token token = new Token();
    token.type = Type.COMMENT;
    return token;
  }

  public String stringVal() {
    return strVal;
  }

  public Integer intVal() {
    return intVal;
  }

  @Override
  public String toString() {
    if (strVal == null && intVal == null) {
      return String.format("Token: %s", type);
    } else if (strVal != null) {
      return String.format("Token: %s %s", type, strVal);
    } else {
      return String.format("Token: %s %d", type, intVal);
    }
  }
}
