/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

public class TestToken implements Lexer.Token {

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

  @Override
  public String name() {
    return this.type.name();
  }

  @Override
  public String value() {
    return this.strVal;
  }

  public static TestToken identifier(String identifier) {
    TestToken testToken = new TestToken();
    testToken.strVal = identifier;
    testToken.type = Type.IDENTIFIER;
    return testToken;
  }

  public static TestToken integer(String integer) {
    TestToken testToken = new TestToken();
    testToken.type = Type.INTEGER;
    testToken.intVal = Integer.parseInt(integer);
    return testToken;
  }

  public static TestToken string(String string) {
    TestToken testToken = new TestToken();
    testToken.type = Type.STRING;
    testToken.strVal = string;
    return testToken;
  }

  public static TestToken of(Type type) {
    TestToken testToken = new TestToken();
    testToken.type = type;
    return testToken;
  }

  public static TestToken comment() {
    TestToken testToken = new TestToken();
    testToken.type = Type.COMMENT;
    return testToken;
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
