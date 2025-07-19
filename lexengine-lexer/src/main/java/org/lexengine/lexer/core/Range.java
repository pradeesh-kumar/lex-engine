/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

/**
 * Represents a range defined by a start and end value. Ranges are immutable and can be
 * created using the provided factory methods.
 */
public class Range implements Comparable<Range> {

  private final int start;
  private final int end;
  private final int hashCode;

  /**
   * Private constructor to prevent direct instantiation. Use the factory methods instead.
   *
   * @param start the starting point of the range
   * @param end the ending point of the range
   */
  private Range(int start, int end) {
    this.start = start;
    this.end = end;
    this.hashCode = calcHashCode();
  }

  /**
   * Creates an range from two values.
   *
   * @param start the starting point of the range
   * @param end the ending point of the range
   * @return a new range instance
   * @throws IllegalArgumentException if start is greater than end
   */
  public static Range of(int start, int end) {
    if (start > end) {
      throw new IllegalArgumentException(
          String.format("start %d must be less than end %d", start, end));
    }
    return new Range(start, end);
  }

  /**
   * Creates an range from a single value, representing a closed range containing only that
   * value.
   *
   * @param val the value used as both start and end points
   * @return a new range instance
   */
  public static Range of(int val) {
    return new Range(val, val);
  }

  @Override
  public String toString() {
    return start == end ? String.format("%c", start) : String.format("[%c-%c]", start, end);
  }

  private int calcHashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + end;
    result = prime * result + start;
    return result;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null) {
      return false;
    }
    if (other instanceof Range range) {
      return range.start == start && range.end == end;
    }
    return false;
  }

  /**
   * Checks whether this range is equal to another range.
   *
   * @param other the range to compare with
   * @return true if the ranges are equal, false otherwise
   */
  public boolean equals(Range other) {
    return other != null && start == other.start && end == other.end;
  }

  /**
   * Compares this range with another range based on their start and end values.
   *
   * @param other the range to compare with
   * @return a negative integer, zero, or a positive integer as this range is less than, equal
   *     to, or greater than the specified range
   */
  @Override
  public int compareTo(Range other) {
    if (this.start == other.start || this.end == other.end) {
      return 0;
    }
    return Integer.compare(this.start, other.start);
  }

  /**
   * Returns the start value of this range.
   *
   * @return the start value of the range
   */
  public int start() {
    return start;
  }

  /**
   * Returns the end value of this range.
   *
   * @return the end value of the range
   */
  public int end() {
    return end;
  }
}
