/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.util.List;

public class RegexToken {

  public enum Type {
    Literal,
    CharClass,
    InvertedCharClass,
    LParen,
    RParen,
    Dollar,
    Dot,
    Bar;

    public boolean isMetaChar() {
      return this == LParen || this == RParen || this == Dollar || this == Dot || this == Bar;
    }
  }

  private Type type;
  private char chVal;
  private List<Interval> intervals;
  private Interval interval;
  // Currently range quantifier {min, max} is not supported
  private char quantifier;

  private RegexToken() {}

  public static RegexToken ofLiteral(char literal) {
    return ofLiteral(literal, '\0');
  }

  public static RegexToken ofLiteral(char literal, char quantifier) {
    RegexToken token = new RegexToken();
    token.quantifier = quantifier;
    token.chVal = literal;
    token.interval = Interval.of(literal);
    token.type =
        switch (literal) {
          case '(' -> Type.LParen;
          case ')' -> Type.RParen;
          case '$' -> Type.Dollar;
          case '.' -> Type.Dot;
          case '|' -> Type.Bar;
          default -> Type.Literal;
        };
    return token;
  }

  public static RegexToken ofClass(List<Interval> intervals, boolean inverted, char quantifier) {
    RegexToken token = new RegexToken();
    token.intervals = intervals;
    token.quantifier = quantifier;
    token.type = inverted ? Type.InvertedCharClass : Type.CharClass;
    return token;
  }

  public Type type() {
    return type;
  }

  public List<Interval> intervals() {
    return intervals;
  }

  public Interval interval() {
    return interval;
  }

  public char chVal() {
    return chVal;
  }

  public char quantifier() {
    return quantifier;
  }
}
