/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

public class DisjointIntSetTest {

  // Input  <- [97, 99] [97, 100] [98, 108]
  // Output -> [97, 97] [98, 99], [100, 100], [101, 108]
  @Test
  void testDisjoint() {
    DisjointIntSet set = new DisjointIntSet();
    set.addRange(97, 99);
    set.addRange(97, 100);
    set.addRange(98, 108);
    List<Interval> disjointed = set.intervals();
    assertEquals(4, disjointed.size());
    assertEquals(
        disjointed,
        List.of(
            Interval.of(97, 97),
            Interval.of(98, 99),
            Interval.of(100, 100),
            Interval.of(101, 108)));

    assertIntersection(set, 97, 97, List.of(Interval.of(97, 97)));
    assertIntersection(set, 100, 100, List.of(Interval.of(100, 100)));
    assertIntersection(set, 108, 108, List.of());
    assertIntersection(set, 100, 108, List.of(Interval.of(100, 100), Interval.of(101, 108)));
    assertIntersection(set, 97, 108, set.intervals());
    assertIntersection(
        set, 98, 108, List.of(Interval.of(98, 99), Interval.of(100, 100), Interval.of(101, 108)));

    set.add(105);
    disjointed = set.intervals();
    assertEquals(6, disjointed.size());
    assertEquals(
        disjointed,
        List.of(
            Interval.of(97, 97),
            Interval.of(98, 99),
            Interval.of(100, 100),
            Interval.of(101, 104),
            Interval.of(105, 105),
            Interval.of(106, 108)));
  }

  @Test
  public void testGetDifference_CompleteOverlap_ReturnsEmptyList() {
    // Arrange
    DisjointIntSet setA = new DisjointIntSet();
    setA.add(Interval.of(1, 3));
    setA.add(Interval.of(5, 7));

    Collection<Interval> setB = Arrays.asList(Interval.of(1, 3), Interval.of(5, 7));

    // Act
    List<Interval> result = setA.getDifference(setB);

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void testGetDifference_IllegalCharacters_ThrowsIllegalArgumentException() {
    // Arrange
    DisjointIntSet setA = new DisjointIntSet();
    setA.add(Interval.of(1, 3));
    setA.add(Interval.of(5, 7));

    Collection<Interval> setB =
        Arrays.asList(Interval.of(10, 12), Interval.of(15, 18)); // 18 is outside setA

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> setA.getDifference(setB));
  }

  @Test
  public void testGetDifference_NullInput_ThrowsNullPointerException() {
    // Arrange
    DisjointIntSet setA = new DisjointIntSet();

    // Act & Assert
    assertThrows(NullPointerException.class, () -> setA.getDifference(null));
  }

  private void assertIntersection(DisjointIntSet set, int start, int end, List<Interval> expected) {
    List<Interval> intersections =
        start == end ? set.getIntersection(start) : set.getIntersection(start, end);
    assertEquals(expected, intersections);
  }

  @Test
  public void testConstructor() {
    DisjointIntSet set = new DisjointIntSet();
    assertNotNull(set);
    assertTrue(set.isEmpty());

    set = new DisjointIntSet(10);
    assertNotNull(set);
    assertTrue(set.isEmpty());
  }

  @Test
  public void testFrom() {
    Collection<Interval> intervals = Arrays.asList(Interval.of(1, 5), Interval.of(7, 10));
    DisjointIntSet set = DisjointIntSet.from(intervals);
    assertEquals(2, set.size());
    assertFalse(set.isEmpty());
  }

  @Test
  public void testGetIntersection_SingleValue() {
    DisjointIntSet set = new DisjointIntSet();
    set.addRange(1, 5);
    assertIntersection(set, 3, 3, List.of());
    assertIntersection(set, 1, 5, List.of(Interval.of(1, 5)));
  }

  @Test
  public void testIsEmpty_TrueFalse() {
    DisjointIntSet set = new DisjointIntSet();
    assertTrue(set.isEmpty());
    set.add(5);
    assertFalse(set.isEmpty());
  }

  @Test
  public void testMinMaxVal_NoValues() {
    DisjointIntSet set = new DisjointIntSet();
    assertEquals(-1, set.minVal());
    assertEquals(-1, set.maxVal());
  }

  @Test
  public void testMinMaxVal_SingleValue() {
    DisjointIntSet set = new DisjointIntSet();
    set.add(5);
    set.add(11);
    assertEquals(5, set.minVal());
    assertEquals(11, set.maxVal());
  }

  @Test
  public void testMinMaxVal_MultipleValues() {
    DisjointIntSet set = new DisjointIntSet();
    set.addRange(1, 5);
    set.addRange(7, 10);
    assertEquals(1, set.minVal());
    assertEquals(10, set.maxVal());
  }
}
