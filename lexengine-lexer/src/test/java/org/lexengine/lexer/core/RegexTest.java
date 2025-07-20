/*
 * Copyright (c) 2024 lex-engine
 * Author: Pradeesh Kumar
 */
package org.lexengine.lexer.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.lexengine.lexer.error.GeneratorException;

public class RegexTest {

  @Test
  void regexIteratorTest() {
    String regexStr = "a*b[0-1]?(abc)+";
    Regex regex = Regex.fromString(regexStr);
    assertEquals(regexStr, regex.toString());
    Iterator<RegexToken> iterator = regex.iterator();
    assertTrue(iterator.hasNext());

    // a*
    RegexToken token = iterator.next();
    assertTrue(token.hasQuantifier());
    assertEquals('*', token.quantifier());
    assertEquals('a', token.chVal());
    assertEquals(Range.of(97, 97), token.range());
    assertEquals(RegexToken.Type.Literal, token.type());
    assertNull(token.ranges());

    // b
    assertTrue(iterator.hasNext());
    token = iterator.next();
    assertFalse(token.hasQuantifier());
    assertEquals('\0', token.quantifier());
    assertEquals('b', token.chVal());
    assertEquals(Range.of(98, 98), token.range());
    assertEquals(RegexToken.Type.Literal, token.type());

    // [0-1]*
    assertTrue(iterator.hasNext());
    token = iterator.next();
    assertTrue(token.hasQuantifier());
    assertEquals('?', token.quantifier());
    assertEquals('\0', token.chVal());
    assertEquals(1, token.ranges().size());
    assertNull(token.range());
    assertEquals(RegexToken.Type.CharClass, token.type());

    // (
    assertTrue(iterator.hasNext());
    token = iterator.next();
    assertFalse(token.hasQuantifier());
    assertEquals('\0', token.quantifier());
    assertEquals('(', token.chVal());
    assertNull(token.ranges());
    assertNotNull(token.range());
    assertEquals(RegexToken.Type.LParen, token.type());

    // (abc)+  // a
    assertTrue(iterator.hasNext());
    token = iterator.next();
    assertFalse(token.hasQuantifier());
    assertEquals('\0', token.quantifier());
    assertEquals('a', token.chVal());
    assertNull(token.ranges());
    assertNotNull(token.range());
    assertEquals(RegexToken.Type.Literal, token.type());

    // (abc)+  // b
    assertTrue(iterator.hasNext());
    token = iterator.next();
    assertFalse(token.hasQuantifier());
    assertEquals('\0', token.quantifier());
    assertEquals('b', token.chVal());
    assertNull(token.ranges());
    assertNotNull(token.range());
    assertEquals(RegexToken.Type.Literal, token.type());

    // (abc)+  // c
    assertTrue(iterator.hasNext());
    token = iterator.next();
    assertFalse(token.hasQuantifier());
    assertEquals('\0', token.quantifier());
    assertEquals('c', token.chVal());
    assertNull(token.ranges());
    assertNotNull(token.range());
    assertEquals(RegexToken.Type.Literal, token.type());

    // (abc)+  // )+
    assertTrue(iterator.hasNext());
    token = iterator.next();
    assertTrue(token.hasQuantifier());
    assertEquals('+', token.quantifier());
    assertEquals(')', token.chVal());
    assertNull(token.ranges());
    assertNotNull(token.range());
    assertEquals(RegexToken.Type.RParen, token.type());

    assertFalse(iterator.hasNext());
  }

  @Test
  void testConvertCharClasses() {
    String regexStr = "[abc]";
    Regex regex = Regex.fromString(regexStr);
    RegexToken token = regex.iterator().next();
    assertNotNull(token);
    assertEquals(RegexToken.Type.CharClass, token.type());
    assertEquals(3, token.ranges().size());
  }

  @Test
  void testParseRange() {
    String regexStr = "[a-c]";
    Regex regex = Regex.fromString(regexStr);
    RegexToken token = regex.iterator().next();
    assertNotNull(token);
    assertEquals(RegexToken.Type.CharClass, token.type());
    assertEquals(1, token.ranges().size());
    Range range = token.ranges().get(0);
    assertEquals((int) 'a', range.start());
    assertEquals((int) 'c', range.end());
  }

  @Test
  void testExtractAlphabets() {
    String regexStr = "abc[a-c]def";
    Regex regex = Regex.fromString(regexStr);
    List<Range> ranges = regex.extractAlphabets();
    assertEquals(7, ranges.size());
    assertEquals((int) 'a', ranges.get(0).start());
    assertEquals((int) 'a', ranges.get(0).end());
    assertEquals((int) 'b', ranges.get(1).start());
    assertEquals((int) 'b', ranges.get(1).end());
    assertEquals((int) 'c', ranges.get(2).start());
    assertEquals((int) 'c', ranges.get(2).end());

    assertEquals((int) 'a', ranges.get(3).start());
    assertEquals((int) 'c', ranges.get(3).end());

    assertEquals((int) 'd', ranges.get(4).start());
    assertEquals((int) 'd', ranges.get(4).end());
    assertEquals((int) 'e', ranges.get(5).start());
    assertEquals((int) 'e', ranges.get(5).end());
    assertEquals((int) 'f', ranges.get(6).start());
    assertEquals((int) 'f', ranges.get(6).end());
  }

  @Test
  void testInvalidEscapeSequence() {
    assertThrows(
        GeneratorException.class,
        () -> {
          String regexStr = "\\a";
          Regex regex = Regex.fromString(regexStr);
          regex.iterator().next();
        });
  }

  @Test
  void testInvalidCharClass() {
    assertThrows(
        GeneratorException.class,
        () -> {
          String regexStr = "[";
          Regex regex = Regex.fromString(regexStr);
          regex.iterator().next();
        });
  }

  @Test
  void testInvalidRangeInCharClass() {
    assertThrows(
        GeneratorException.class,
        () -> {
          String regexStr = "[a-]";
          Regex regex = Regex.fromString(regexStr);
          regex.iterator().next();
        });
  }

  @Test
  void testMixedDigitsAndLettersInRangeClass() {
    assertThrows(
        GeneratorException.class,
        () -> {
          String regexStr = "[a-9]";
          Regex regex = Regex.fromString(regexStr);
          regex.iterator().next();
        });
  }
}
