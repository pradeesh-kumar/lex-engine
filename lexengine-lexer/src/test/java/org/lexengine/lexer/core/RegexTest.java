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
    assertEquals(Interval.of(97, 97), token.interval());
    assertEquals(RegexToken.Type.Literal, token.type());
    assertNull(token.intervals());

    // b
    assertTrue(iterator.hasNext());
    token = iterator.next();
    assertFalse(token.hasQuantifier());
    assertEquals('\0', token.quantifier());
    assertEquals('b', token.chVal());
    assertEquals(Interval.of(98, 98), token.interval());
    assertEquals(RegexToken.Type.Literal, token.type());

    // [0-1]*
    assertTrue(iterator.hasNext());
    token = iterator.next();
    assertTrue(token.hasQuantifier());
    assertEquals('?', token.quantifier());
    assertEquals('\0', token.chVal());
    assertEquals(1, token.intervals().size());
    assertNull(token.interval());
    assertEquals(RegexToken.Type.CharClass, token.type());

    // (
    assertTrue(iterator.hasNext());
    token = iterator.next();
    assertFalse(token.hasQuantifier());
    assertEquals('\0', token.quantifier());
    assertEquals('(', token.chVal());
    assertNull(token.intervals());
    assertNotNull(token.interval());
    assertEquals(RegexToken.Type.LParen, token.type());

    // (abc)+  // a
    assertTrue(iterator.hasNext());
    token = iterator.next();
    assertFalse(token.hasQuantifier());
    assertEquals('\0', token.quantifier());
    assertEquals('a', token.chVal());
    assertNull(token.intervals());
    assertNotNull(token.interval());
    assertEquals(RegexToken.Type.Literal, token.type());

    // (abc)+  // b
    assertTrue(iterator.hasNext());
    token = iterator.next();
    assertFalse(token.hasQuantifier());
    assertEquals('\0', token.quantifier());
    assertEquals('b', token.chVal());
    assertNull(token.intervals());
    assertNotNull(token.interval());
    assertEquals(RegexToken.Type.Literal, token.type());

    // (abc)+  // c
    assertTrue(iterator.hasNext());
    token = iterator.next();
    assertFalse(token.hasQuantifier());
    assertEquals('\0', token.quantifier());
    assertEquals('c', token.chVal());
    assertNull(token.intervals());
    assertNotNull(token.interval());
    assertEquals(RegexToken.Type.Literal, token.type());

    // (abc)+  // )+
    assertTrue(iterator.hasNext());
    token = iterator.next();
    assertTrue(token.hasQuantifier());
    assertEquals('+', token.quantifier());
    assertEquals(')', token.chVal());
    assertNull(token.intervals());
    assertNotNull(token.interval());
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
    assertEquals(3, token.intervals().size());
  }

  @Test
  void testParseRange() {
    String regexStr = "[a-c]";
    Regex regex = Regex.fromString(regexStr);
    RegexToken token = regex.iterator().next();
    assertNotNull(token);
    assertEquals(RegexToken.Type.CharClass, token.type());
    assertEquals(1, token.intervals().size());
    Interval interval = token.intervals().get(0);
    assertEquals((int) 'a', interval.start());
    assertEquals((int) 'c', interval.end());
  }

  @Test
  void testExtractAlphabets() {
    String regexStr = "abc[a-c]def";
    Regex regex = Regex.fromString(regexStr);
    List<Interval> intervals = regex.extractAlphabets();
    assertEquals(7, intervals.size());
    assertEquals((int) 'a', intervals.get(0).start());
    assertEquals((int) 'a', intervals.get(0).end());
    assertEquals((int) 'b', intervals.get(1).start());
    assertEquals((int) 'b', intervals.get(1).end());
    assertEquals((int) 'c', intervals.get(2).start());
    assertEquals((int) 'c', intervals.get(2).end());

    assertEquals((int) 'a', intervals.get(3).start());
    assertEquals((int) 'c', intervals.get(3).end());

    assertEquals((int) 'd', intervals.get(4).start());
    assertEquals((int) 'd', intervals.get(4).end());
    assertEquals((int) 'e', intervals.get(5).start());
    assertEquals((int) 'e', intervals.get(5).end());
    assertEquals((int) 'f', intervals.get(6).start());
    assertEquals((int) 'f', intervals.get(6).end());
  }

  @Test
  void testInvalidEscapeSequence() {
    assertThrows(
        GeneratorException.class,
        () -> {
          String regexStr = "\'";
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
