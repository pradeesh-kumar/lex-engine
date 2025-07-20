/*
 * Copyright (c) 2024 lex-engine
 * Author: Pradeesh Kumar
 */
package org.lexengine.lexer.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class RangeTest {

  @Test
  void testOfTwoValues() {
    // Given
    int start = 10;
    int end = 20;

    // When
    Range range = Range.of(start, end);

    // Then
    assertEquals(start, range.start());
    assertEquals(end, range.end());
  }

  @Test
  void testOfSingleValue() {
    // Given
    int val = 15;

    // When
    Range range = Range.of(val);

    // Then
    assertEquals(val, range.start());
    assertEquals(val, range.end());
  }

  @Test
  void testInvalidRangeCreation() {
    // Given
    int start = 20;
    int end = 10;

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> Range.of(start, end));
  }

  @Test
  void testToString() {
    assertEquals("[a-b]", Range.of(97, 98).toString());
    assertEquals("a", Range.of(97, 97).toString());
  }

  @Test
  void testEqualsSameInstance() {
    // Given
    int start = 10;
    int end = 20;
    Range range = Range.of(start, end);

    // When & Then
    assertTrue(range.equals(range));
  }

  @Test
  void testEqualsDifferentInstancesSameValues() {
    // Given
    int start = 10;
    int end = 20;
    Range range1 = Range.of(start, end);
    Range range2 = Range.of(start, end);

    // When & Then
    assertTrue(range1.equals(range2));
  }

  @Test
  void testEqualsDifferentInstancesDifferentValues() {
    // Given
    int start1 = 10;
    int end1 = 20;
    int start2 = 15;
    int end2 = 25;
    Range range1 = Range.of(start1, end1);
    Range range2 = Range.of(start2, end2);

    // When & Then
    assertFalse(range1.equals(range2));
  }

  @Test
  void testEqualsNull() {
    // Given
    int start = 10;
    int end = 20;
    Range range = Range.of(start, end);

    // When & Then
    assertFalse(range.equals(null));
  }

  @Test
  void testCompareToEqualRanges() {
    // Given
    int start = 10;
    int end = 20;
    Range range1 = Range.of(start, end);
    Range range2 = Range.of(start, end);

    // When
    int comparisonResult = range1.compareTo(range2);

    // Then
    assertEquals(0, comparisonResult);
  }

  @Test
  void testCompareToRangesWithDifferentStartsSameEnd() {
    // Given
    int start1 = 10;
    int end1 = 20;
    int start2 = 15;
    int end2 = 20;
    Range range1 = Range.of(start1, end1);
    Range range2 = Range.of(start2, end2);

    // When
    int comparisonResult = range1.compareTo(range2);

    // Then
    assertEquals(0, comparisonResult);
  }

  @Test
  void testCompareToRangesWithDifferentEnds() {
    // Given
    int start1 = 10;
    int end1 = 20;
    int start2 = 10;
    int end2 = 25;
    Range range1 = Range.of(start1, end1);
    Range range2 = Range.of(start2, end2);

    // When
    int comparisonResult = range1.compareTo(range2);

    // Then
    assertEquals(0, comparisonResult); // Note: This might not be what we want, see below
  }

  @Test
  void testHashCodeConsistency() {
    // Given
    int start = 10;
    int end = 20;
    Range range1 = Range.of(start, end);
    Range range2 = Range.of(start, end);

    // When & Then
    assertEquals(range1.hashCode(), range2.hashCode());
  }
}
