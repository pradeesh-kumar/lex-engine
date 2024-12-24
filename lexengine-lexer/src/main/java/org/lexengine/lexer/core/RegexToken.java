/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.util.List;
import org.lexengine.lexer.error.ErrorType;
import org.lexengine.lexer.error.GeneratorException;
import org.lexengine.lexer.logging.Out;

/** Represents a regular expression token used for regex tokenization. */
public class RegexToken {

  /** Represents various token types. */
  public enum Type {
    Literal,
    CharClass,
    InvertedCharClass,
    LParen,
    RParen,
    Dot,
    Bar;

    /**
     * Returns whether this token type represents a meta-character.
     *
     * @return true if this token type is a meta-character, false otherwise
     */
    public boolean isMetaChar() {
      return this == LParen || this == RParen || this == Dot || this == Bar;
    }
  }

  private Type type;
  private char chVal;
  private List<Interval> intervals;
  private Interval interval;
  // Currently range quantifier {min, max} is not supported
  private char quantifier;

  /** Private constructor to prevent direct instantiation. Use the factory methods instead. */
  private RegexToken() {}

  /**
   * Creates a new literal token from a given character.
   *
   * @param literal the character to represent as a literal token
   * @return a new RegexToken instance
   */
  public static RegexToken ofLiteral(char literal) {
    return ofLiteral(literal, '\0', false);
  }

  /**
   * Creates a new literal token from a given character and optional quantifier.
   *
   * @param literal the character to represent as a literal token
   * @param quantifier the quantifier to apply to the token, or '\0' for no quantifier
   * @return a new RegexToken instance
   */
  public static RegexToken ofLiteral(char literal, char quantifier, boolean escaped) {
    RegexToken token = new RegexToken();
    token.quantifier = quantifier;
    token.chVal = literal;
    token.interval = Interval.of(literal);
    token.type = escaped ? Type.Literal :
        switch (literal) {
          case '(' -> Type.LParen;
          case ')' -> Type.RParen;
          case '.' -> Type.Dot;
          case '|' -> Type.Bar;
          default -> Type.Literal;
        };
    if ((token.type == Type.Dot || token.type == Type.Bar || token.type == Type.LParen)
        && token.quantifier != '\0') {
      Out.error("Invalid regular expression: " + literal);
      throw GeneratorException.error(ErrorType.ERR_REGEX_ERR);
    }
    return token;
  }

  /**
   * Creates a new character class token from a list of intervals and an optional inversion flag.
   *
   * @param intervals the list of intervals defining the character class
   * @param inverted whether the character class should match characters outside the specified
   *     ranges
   * @param quantifier the quantifier to apply to the token, or '\0' for no quantifier
   * @return a new RegexToken instance
   */
  public static RegexToken ofClass(List<Interval> intervals, boolean inverted, char quantifier) {
    RegexToken token = new RegexToken();
    token.intervals = intervals;
    token.quantifier = quantifier;
    token.type = inverted ? Type.InvertedCharClass : Type.CharClass;
    return token;
  }

  /**
   * Returns the type of this token.
   *
   * @return the type of this token
   */
  public Type type() {
    return type;
  }

  /**
   * Returns the list of intervals associated with this token, if it is a character class token.
   *
   * @return the list of intervals, or null if this token is not a character class token
   */
  public List<Interval> intervals() {
    return intervals;
  }

  /**
   * Returns the interval associated with this token, if it is a literal token.
   *
   * @return the interval, or null if this token is not a literal token
   */
  public Interval interval() {
    return interval;
  }

  /**
   * Returns the character value associated with this token, if it is a literal token.
   *
   * @return the character value, or '\0' if this token is not a literal token
   */
  public char chVal() {
    return chVal;
  }

  /**
   * Returns whether this token has a quantifier applied.
   *
   * @return true if this token has a quantifier, false otherwise
   */
  public boolean hasQuantifier() {
    return quantifier != '\0';
  }

  /**
   * Returns the quantifier applied to this token, if any.
   *
   * @return the quantifier, or '\0' if no quantifier is applied
   */
  public char quantifier() {
    return quantifier;
  }
}
