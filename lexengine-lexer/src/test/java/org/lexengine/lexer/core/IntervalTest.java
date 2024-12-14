/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class IntervalTest {

  @Test
  void testOfTwoValues() {
    // Given
    int start = 10;
    int end = 20;

    // When
    Interval interval = Interval.of(start, end);

    // Then
    assertEquals(start, interval.start());
    assertEquals(end, interval.end());
  }

  @Test
  void testOfSingleValue() {
    // Given
    int val = 15;

    // When
    Interval interval = Interval.of(val);

    // Then
    assertEquals(val, interval.start());
    assertEquals(val, interval.end());
  }

  @Test
  void testInvalidIntervalCreation() {
    // Given
    int start = 20;
    int end = 10;

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> Interval.of(start, end));
  }

  @Test
  void testToString() {
    assertEquals("[a-b]", Interval.of(97, 98).toString());
    assertEquals("a", Interval.of(97, 97).toString());
  }

  @Test
  void testEqualsSameInstance() {
    // Given
    int start = 10;
    int end = 20;
    Interval interval = Interval.of(start, end);

    // When & Then
    assertTrue(interval.equals(interval));
  }

  @Test
  void testEqualsDifferentInstancesSameValues() {
    // Given
    int start = 10;
    int end = 20;
    Interval interval1 = Interval.of(start, end);
    Interval interval2 = Interval.of(start, end);

    // When & Then
    assertTrue(interval1.equals(interval2));
  }

  @Test
  void testEqualsDifferentInstancesDifferentValues() {
    // Given
    int start1 = 10;
    int end1 = 20;
    int start2 = 15;
    int end2 = 25;
    Interval interval1 = Interval.of(start1, end1);
    Interval interval2 = Interval.of(start2, end2);

    // When & Then
    assertFalse(interval1.equals(interval2));
  }

  @Test
  void testEqualsNull() {
    // Given
    int start = 10;
    int end = 20;
    Interval interval = Interval.of(start, end);

    // When & Then
    assertFalse(interval.equals(null));
  }

  @Test
  void testCompareToEqualIntervals() {
    // Given
    int start = 10;
    int end = 20;
    Interval interval1 = Interval.of(start, end);
    Interval interval2 = Interval.of(start, end);

    // When
    int comparisonResult = interval1.compareTo(interval2);

    // Then
    assertEquals(0, comparisonResult);
  }

  @Test
  void testCompareToIntervalsWithDifferentStartsSameEnd() {
    // Given
    int start1 = 10;
    int end1 = 20;
    int start2 = 15;
    int end2 = 20;
    Interval interval1 = Interval.of(start1, end1);
    Interval interval2 = Interval.of(start2, end2);

    // When
    int comparisonResult = interval1.compareTo(interval2);

    // Then
    assertEquals(0, comparisonResult);
  }

  @Test
  void testCompareToIntervalsWithDifferentEnds() {
    // Given
    int start1 = 10;
    int end1 = 20;
    int start2 = 10;
    int end2 = 25;
    Interval interval1 = Interval.of(start1, end1);
    Interval interval2 = Interval.of(start2, end2);

    // When
    int comparisonResult = interval1.compareTo(interval2);

    // Then
    assertEquals(0, comparisonResult); // Note: This might not be what we want, see below
  }

  @Test
  void testHashCodeConsistency() {
    // Given
    int start = 10;
    int end = 20;
    Interval interval1 = Interval.of(start, end);
    Interval interval2 = Interval.of(start, end);

    // When & Then
    assertEquals(interval1.hashCode(), interval2.hashCode());
  }
}
