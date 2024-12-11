/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

/**
 * Represents an interval defined by a start and end value. Intervals are immutable and can be
 * created using the provided factory methods.
 */
public class Interval implements Comparable<Interval> {

  private final int start;
  private final int end;
  private final int hashCode;

  /**
   * Private constructor to prevent direct instantiation. Use the factory methods instead.
   *
   * @param start the starting point of the interval
   * @param end the ending point of the interval
   */
  private Interval(int start, int end) {
    this.start = start;
    this.end = end;
    this.hashCode = calcHashCode();
  }

  /**
   * Creates an interval from two values.
   *
   * @param start the starting point of the interval
   * @param end the ending point of the interval
   * @return a new Interval instance
   * @throws IllegalArgumentException if start is greater than end
   */
  public static Interval of(int start, int end) {
    if (start > end) {
      throw new IllegalArgumentException(
          String.format("start %d must be less than end %d", start, end));
    }
    return new Interval(start, end);
  }

  /**
   * Creates an interval from a single value, representing a closed range containing only that
   * value.
   *
   * @param val the value used as both start and end points
   * @return a new Interval instance
   */
  public static Interval of(int val) {
    return new Interval(val, val);
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
    if (other instanceof Interval interval) {
      return interval.start == start && interval.end == end;
    }
    return false;
  }

  /**
   * Checks whether this interval is equal to another interval.
   *
   * @param other the interval to compare with
   * @return true if the intervals are equal, false otherwise
   */
  public boolean equals(Interval other) {
    return other != null && start == other.start && end == other.end;
  }

  /**
   * Compares this interval with another interval based on their start and end values.
   *
   * @param other the interval to compare with
   * @return a negative integer, zero, or a positive integer as this interval is less than, equal
   *     to, or greater than the specified interval
   */
  @Override
  public int compareTo(Interval other) {
    if (this.start == other.start || this.end == other.end) {
      return 0;
    }
    return Integer.compare(this.start, other.start);
  }

  /**
   * Returns the start value of this interval.
   *
   * @return the start value of the interval
   */
  public int start() {
    return start;
  }

  /**
   * Returns the end value of this interval.
   *
   * @return the end value of the interval
   */
  public int end() {
    return end;
  }
}
