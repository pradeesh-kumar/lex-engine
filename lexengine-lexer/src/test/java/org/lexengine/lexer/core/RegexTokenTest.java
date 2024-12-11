/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

public class RegexTokenTest {

  @Test
  void testOfLiteral_NoQuantifier_LiteralTokenCreated() {
    // Arrange & Act
    RegexToken token = RegexToken.ofLiteral('a');

    // Assert
    assertEquals(RegexToken.Type.Literal, token.type());
    assertEquals((int) 'a', token.chVal());
    assertNotNull(token.interval());
    assertEquals(Interval.of(97, 97), token.interval()); // ASCII value of 'a'
    assertFalse(token.hasQuantifier());
  }

  @Test
  void testOfLiteral_WithQuantifier_LiteralTokenCreated() {
    // Arrange & Act
    RegexToken token = RegexToken.ofLiteral('a', '*');

    // Assert
    assertEquals(RegexToken.Type.Literal, token.type());
    assertEquals((int) 'a', token.chVal());
    assertNotNull(token.interval());
    assertEquals(Interval.of(97, 97), token.interval()); // ASCII value of 'a'
    assertTrue(token.hasQuantifier());
    assertEquals('*', token.quantifier());
  }

  @Test
  void testOfClass_CharacterClassTokenCreated() {
    // Arrange
    List<Interval> intervals = List.of(Interval.of(65, 90)); // A-Z

    // Act
    RegexToken token = RegexToken.ofClass(intervals, false, '\0');

    // Assert
    assertEquals(RegexToken.Type.CharClass, token.type());
    assertEquals(intervals, token.intervals());
    assertNull(token.interval());
    assertEquals('\0', token.chVal());
    assertFalse(token.hasQuantifier());
  }

  @Test
  void testOfClass_InvertedCharacterClassTokenCreated() {
    // Arrange
    List<Interval> intervals = List.of(Interval.of(65, 90)); // A-Z

    // Act
    RegexToken token = RegexToken.ofClass(intervals, true, '\0');

    // Assert
    assertEquals(RegexToken.Type.InvertedCharClass, token.type());
    assertEquals(intervals, token.intervals());
    assertNull(token.interval());
    assertEquals('\0', token.chVal());
    assertFalse(token.hasQuantifier());
  }

  @Test
  void testOfLiteral_MetaCharacters_TokenTypeCorrectlyAssigned() {
    // Act & Assert
    assertEquals(RegexToken.Type.LParen, RegexToken.ofLiteral('(').type());
    assertEquals(RegexToken.Type.RParen, RegexToken.ofLiteral(')').type());
    assertEquals(RegexToken.Type.Dot, RegexToken.ofLiteral('.').type());
    assertEquals(RegexToken.Type.Bar, RegexToken.ofLiteral('|').type());
  }
}
