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
    List<Range> disjointed = set.ranges();
    assertEquals(4, disjointed.size());
    assertEquals(
        disjointed,
        List.of(Range.of(97, 97), Range.of(98, 99), Range.of(100, 100), Range.of(101, 108)));

    assertIntersection(set, 97, 97, List.of(Range.of(97, 97)));
    assertIntersection(set, 100, 100, List.of(Range.of(100, 100)));
    assertIntersection(set, 108, 108, List.of());
    assertIntersection(set, 100, 108, List.of(Range.of(100, 100), Range.of(101, 108)));
    assertIntersection(set, 97, 108, set.ranges());
    assertIntersection(
        set, 98, 108, List.of(Range.of(98, 99), Range.of(100, 100), Range.of(101, 108)));

    set.add(105);
    disjointed = set.ranges();
    assertEquals(6, disjointed.size());
    assertEquals(
        disjointed,
        List.of(
            Range.of(97, 97),
            Range.of(98, 99),
            Range.of(100, 100),
            Range.of(101, 104),
            Range.of(105, 105),
            Range.of(106, 108)));
  }

  @Test
  public void testGetDifference_CompleteOverlap_ReturnsEmptyList() {
    // Arrange
    DisjointIntSet setA = new DisjointIntSet();
    setA.add(Range.of(1, 3));
    setA.add(Range.of(5, 7));

    Collection<Range> setB = Arrays.asList(Range.of(1, 3), Range.of(5, 7));

    // Act
    List<Range> result = setA.getDifference(setB);

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void testGetDifference_IllegalCharacters_ThrowsIllegalArgumentException() {
    // Arrange
    DisjointIntSet setA = new DisjointIntSet();
    setA.add(Range.of(1, 3));
    setA.add(Range.of(5, 7));

    Collection<Range> setB =
        Arrays.asList(Range.of(10, 12), Range.of(15, 18)); // 18 is outside setA

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

  private void assertIntersection(DisjointIntSet set, int start, int end, List<Range> expected) {
    List<Range> intersections =
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
    Collection<Range> ranges = Arrays.asList(Range.of(1, 5), Range.of(7, 10));
    DisjointIntSet set = DisjointIntSet.from(ranges);
    assertEquals(2, set.size());
    assertFalse(set.isEmpty());
  }

  @Test
  public void testGetIntersection_SingleValue() {
    DisjointIntSet set = new DisjointIntSet();
    set.addRange(1, 5);
    assertIntersection(set, 3, 3, List.of());
    assertIntersection(set, 1, 5, List.of(Range.of(1, 5)));
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

  @Test
  public void testGetRange_Found() {
    // Arrange
    DisjointIntSet disjointIntSet = new DisjointIntSet();
    disjointIntSet.add(Range.of(10, 20));

    // Act
    Range result = disjointIntSet.getRange(15);

    // Assert
    assertNotNull(result);
    assertEquals(10, result.start());
    assertEquals(20, result.end());
  }

  @Test
  public void testGetRange_NotFound() {
    // Arrange
    DisjointIntSet disjointIntSet = new DisjointIntSet();

    // Act
    Range result = disjointIntSet.getRange(15);

    // Assert
    assertNull(result);
  }

  @Test
  public void testGetRange_Multipleranges() {
    // Arrange
    DisjointIntSet disjointIntSet = new DisjointIntSet();
    disjointIntSet.add(Range.of(10, 20));
    disjointIntSet.add(Range.of(30, 40));

    // Act
    Range result1 = disjointIntSet.getRange(15);
    Range result2 = disjointIntSet.getRange(35);

    // Assert
    assertNotNull(result1);
    assertEquals(10, result1.start());
    assertEquals(20, result1.end());

    assertNotNull(result2);
    assertEquals(30, result2.start());
    assertEquals(40, result2.end());
  }

  @Test
  public void testGetRange_ExactMatch() {
    // Arrange
    DisjointIntSet disjointIntSet = new DisjointIntSet();
    disjointIntSet.add(Range.of(10, 10)); // Single-element range

    // Act
    Range result = disjointIntSet.getRange(10);

    // Assert
    assertNotNull(result);
    assertEquals(10, result.start());
    assertEquals(10, result.end());
  }

  @Test
  public void testGetRange_Noranges() {
    // Arrange
    DisjointIntSet disjointIntSet = new DisjointIntSet();

    // Act
    Range result = disjointIntSet.getRange(15);

    // Assert
    assertNull(result);
  }
}
