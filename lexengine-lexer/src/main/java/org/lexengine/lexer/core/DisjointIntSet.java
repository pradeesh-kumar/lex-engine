/*
 * Copyright (c) 2024 lex-engine
 * Author: Pradeesh Kumar
 */
package org.lexengine.lexer.core;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A data structure representing a set of non-overlapping integer ranges.
 *
 * <p>This class allows you to store and manage multiple ranges of integers, ensuring that they do
 * not overlap with each other. It provides methods for adding individual values or ranges, checking
 * intersections, and retrieving the stored ranges.
 *
 * <p>Note: This is a lazy disjoint data structure. Which means that, the disjoint operation doesn't
 * happen immediately whenever you add new elements. Rather It happens implicitly when you call the
 * methods getIntersections(), getDifference() and ranges().
 */
public class DisjointIntSet {

  private static final Comparator<Range> INTERVAL_WITHIN_BOUND_COMPARATOR =
      (i1, i2) -> {
        if (i2.start() >= i1.start() && i2.end() <= i1.end()) {
          return 0;
        }
        if (i2.start() == i1.start()) {
          return i1.end() - i2.end();
        }
        return i1.start() - i2.start();
      };

  private List<Range> ranges;
  private boolean disjointed;
  private int minVal;
  private int maxVal;

  /** Constructs an empty DisjointIntSet with default initial capacity. */
  public DisjointIntSet() {
    this(10);
  }

  /**
   * Constructs an empty DisjointIntSet with specified initial capacity.
   *
   * @param initialCapacity the initial capacity of the underlying collection
   */
  public DisjointIntSet(int initialCapacity) {
    this.ranges = new ArrayList<>(initialCapacity);
    this.minVal = -1;
    this.maxVal = -1;
    this.disjointed = false;
  }

  /**
   * Creates a new DisjointIntSet from a collection of ranges.
   *
   * @param ranges the collection of ranges to initialize the set with
   * @return a new DisjointIntSet containing the provided ranges
   */
  public static DisjointIntSet from(Collection<Range> ranges) {
    DisjointIntSet set = new DisjointIntSet(ranges.size());
    set.ranges = new ArrayList<>(ranges);
    return set;
  }

  /**
   * Adds a single value to the set.
   *
   * @param codePoint the value to add
   */
  public void add(int codePoint) {
    add(Range.of(codePoint));
  }

  /**
   * Adds a range of values to the set.
   *
   * @param start the starting point of the range (inclusive)
   * @param end the ending point of the range (exclusive)
   */
  public void addRange(int start, int end) {
    add(Range.of(start, end));
  }

  public void add(Range range) {
    this.disjointed = false;
    this.ranges.add(range);
    updateMinMax(range);
  }

  /**
   * Updates the minimum and maximum values tracked by this DisjointIntSet based on the provided
   * range.
   *
   * <p>If no previous minimum or maximum value has been recorded (-1), sets them to the
   * corresponding bounds of the given range. Otherwise, updates these values to be the
   * smaller/larger of the current minimum/maximum and the respective bound of the range.
   *
   * @param range the range whose bounds will influence the updated minimum and maximum values
   */
  private void updateMinMax(Range range) {
    this.minVal = this.minVal == -1 ? range.start() : Math.min(minVal, range.start());
    this.maxVal = Math.max(maxVal, range.end());
  }

  /**
   * Computes the difference between this DisjointIntSet and another collection of ranges.
   *
   * <p>This method returns a list of ranges that are present in this set but not in the provided
   * collection. Note that the resulting ranges may not be disjoint due to the nature of the
   * difference operation.
   *
   * @param other the collection of ranges to compute the difference against
   * @return a list of ranges in this set but not in the provided collection
   * @throws IllegalArgumentException if the provided collection contains ranges that are not
   *     contained within this set
   */
  public List<Range> getDifference(Collection<Range> other) {
    checkAndDoDisjoint();
    Set<Range> a = Set.copyOf(this.ranges);
    Set<Range> b = Set.copyOf(other);
    boolean illegalChars = b.stream().anyMatch(Predicate.not(a::contains));
    if (illegalChars) {
      throw new IllegalArgumentException("Illegal characters in the input list");
    }
    return a.stream().filter(Predicate.not(b::contains)).toList();
  }

  /**
   * Retrieves the range that contains the specified code point, if such an range exists in the set.
   *
   * @param codePoint the code point to search for
   * @return the range containing the code point, or null if no such range exists
   */
  public Range getRange(int codePoint) {
    Range key = Range.of(codePoint, codePoint);
    int pos = Collections.binarySearch(this.ranges, key, INTERVAL_WITHIN_BOUND_COMPARATOR);
    if (pos < 0) {
      return null;
    }
    return ranges.get(pos);
  }

  /**
   * Retrieves all ranges intersecting with the given value.
   *
   * @param codePoint the value to find intersections for
   * @return a list of ranges intersecting with the given value
   */
  public List<Range> getIntersection(int codePoint) {
    return getIntersection(codePoint, codePoint);
  }

  /**
   * Retrieves all ranges intersecting with the given range.
   *
   * @param range the value to find intersections for
   * @return a list of ranges intersecting with the given value
   */
  public List<Range> getIntersection(Range range) {
    return getIntersection(range.start(), range.end());
  }

  /**
   * Retrieves all unique ranges from this set that intersect with any of the ranges in the given
   * collection.
   *
   * @param ranges the collection of ranges to find intersections with
   * @return a list of unique ranges from this set that intersect with at least one range in the
   *     given collection
   */
  public List<Range> getIntersection(Collection<Range> ranges) {
    return ranges.stream().map(this::getIntersection).flatMap(List::stream).distinct().toList();
  }

  /**
   * Retrieves all ranges intersecting with the given range.
   *
   * @param start the starting point of the range (inclusive)
   * @param end the ending point of the range (exclusive)
   * @return a list of ranges intersecting with the given range
   */
  public List<Range> getIntersection(int start, int end) {
    checkAndDoDisjoint();
    Range key = Range.of(start, end);
    int pos = Collections.binarySearch(this.ranges, key);
    if (pos < 0) {
      return List.of();
    }
    Range range = ranges.get(pos);
    if (start == end && range.start() != range.end()) {
      return List.of();
    }
    int left = pos;
    while (left >= 0 && ranges.get(left).start() > start) {
      left--;
    }
    if (left < 0) {
      return List.of();
    }
    int right = pos;
    while (right < size() && ranges.get(right).end() < end) {
      right++;
    }
    if (right >= size()) {
      return List.of();
    }
    List<Range> result = new ArrayList<>(right - left + 1);
    for (int i = left; i <= right; i++) {
      result.add(ranges.get(i));
    }
    return result;
  }

  /** Ensures that the internal state is disjointed. */
  private void checkAndDoDisjoint() {
    if (!disjointed) {
      doDisjoint();
    }
  }

  /** Merges overlapping ranges in the set, ensuring it remains disjointed. */
  private void doDisjoint() {
    if (disjointed || isEmpty()) {
      return;
    }
    // type = 1 if event is a start event, otherwise it's -1
    record Event(int position, int type) {}
    List<Event> events = new ArrayList<>();
    for (Range range : ranges) {
      events.add(new Event(range.start(), 1));
      events.add(new Event(range.end() + 1, -1));
    }
    events.sort(
        (e1, e2) ->
            e1.position != e2.position
                ? Integer.compare(e1.position, e2.position)
                : Integer.compare(e1.type, e2.type));
    List<Range> result = new ArrayList<>(ranges.size());
    int active = 0;
    int prevPosition = Integer.MAX_VALUE;
    for (Event e : events) {
      if (active > 0 && prevPosition < e.position) {
        result.add(Range.of(prevPosition, e.position - 1));
      }
      active += e.type;
      prevPosition = e.position;
    }
    this.ranges = new LinkedList<>(result);
    this.disjointed = true;
  }

  /**
   * Returns the APPROXIMATE number of ranges currently stored in the set. The size is approximate
   * since this data structure is Lazy. The actual size is determined when implicit call to
   * doDisjoint() method is invoked.
   *
   * @return the size of the set
   */
  public int size() {
    return this.ranges.size();
  }

  public boolean isEmpty() {
    return this.ranges.isEmpty();
  }

  /**
   * Returns an immutable copy of the stored ranges.
   *
   * @return a list of ranges in the set
   */
  public List<Range> ranges() {
    checkAndDoDisjoint();
    return List.copyOf(this.ranges);
  }

  /**
   * Returns the minimum value present in the set.
   *
   * @return the smallest value among all ranges in the set
   */
  public int minVal() {
    return minVal;
  }

  /**
   * Returns the maximum value present in the set.
   *
   * @return the largest value among all ranges in the set
   */
  public int maxVal() {
    return maxVal;
  }

  @Override
  public String toString() {
    return this.ranges.stream().map(Range::toString).collect(Collectors.joining(", "));
  }
}
