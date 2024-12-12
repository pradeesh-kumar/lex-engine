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
 * <p>This class allows you to store and manage multiple intervals of integers, ensuring that they
 * do not overlap with each other. It provides methods for adding individual values or ranges,
 * checking intersections, and retrieving the stored intervals.
 * </p>
 * <p>Note: This is a lazy disjoint data structure. Which means that, the disjoint operation doesn't
 * happen immediately whenever you add new elements. Rather It happens implicitly when you call the
 * methods getIntersections(), getDifference() and intervals().
 * </p>
 */
public class DisjointIntSet {

  private static final Comparator<Interval> INTERVAL_WITHIN_BOUND_COMPARATOR = (i1, i2) -> {
    if (i2.start() >= i1.start() && i2.end() <= i1.end()) {
      return 0;
    }
    if (i2.start() == i1.start()) {
      return i1.end() - i2.end();
    }
    return i1.start() - i2.start();
  };

  private List<Interval> intervals;
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
    this.intervals = new ArrayList<>(initialCapacity);
    this.minVal = -1;
    this.maxVal = -1;
    this.disjointed = false;
  }

  /**
   * Creates a new DisjointIntSet from a collection of Intervals.
   *
   * @param intervals the collection of Intervals to initialize the set with
   * @return a new DisjointIntSet containing the provided intervals
   */
  public static DisjointIntSet from(Collection<Interval> intervals) {
    DisjointIntSet set = new DisjointIntSet(intervals.size());
    set.intervals = new ArrayList<>(intervals);
    return set;
  }

  /**
   * Adds a single value to the set.
   *
   * @param codePoint the value to add
   */
  public void add(int codePoint) {
    add(Interval.of(codePoint));
  }

  /**
   * Adds a range of values to the set.
   *
   * @param start the starting point of the range (inclusive)
   * @param end the ending point of the range (exclusive)
   */
  public void addRange(int start, int end) {
    add(Interval.of(start, end));
  }

  public void add(Interval interval) {
    this.disjointed = false;
    this.intervals.add(interval);
    updateMinMax(interval);
  }

  /**
   * Updates the minimum and maximum values tracked by this DisjointIntSet based on the provided
   * interval.
   *
   * <p>If no previous minimum or maximum value has been recorded (-1), sets them to the
   * corresponding bounds of the given interval. Otherwise, updates these values to be the
   * smaller/larger of the current minimum/maximum and the respective bound of the interval.
   *
   * @param interval the interval whose bounds will influence the updated minimum and maximum values
   */
  private void updateMinMax(Interval interval) {
    this.minVal = this.minVal == -1 ? interval.start() : Math.min(minVal, interval.start());
    this.maxVal = Math.max(maxVal, interval.end());
  }

  /**
   * Computes the difference between this DisjointIntSet and another collection of intervals.
   *
   * <p>This method returns a list of intervals that are present in this set but not in the provided
   * collection. Note that the resulting intervals may not be disjoint due to the nature of the
   * difference operation.
   *
   * @param other the collection of intervals to compute the difference against
   * @return a list of intervals in this set but not in the provided collection
   * @throws IllegalArgumentException if the provided collection contains intervals that are not
   *     contained within this set
   */
  public List<Interval> getDifference(Collection<Interval> other) {
    checkAndDoDisjoint();
    Set<Interval> a = Set.copyOf(this.intervals);
    Set<Interval> b = Set.copyOf(other);
    boolean illegalChars = b.stream().anyMatch(Predicate.not(a::contains));
    if (illegalChars) {
      throw new IllegalArgumentException("Illegal characters in the input list");
    }
    return a.stream().filter(Predicate.not(b::contains)).toList();
  }

  /**
   * Retrieves the interval that contains the specified code point, if such an interval exists in the set.
   *
   * @param codePoint the code point to search for
   * @return the interval containing the code point, or null if no such interval exists
   */
  public Interval getInterval(int codePoint) {
    Interval key = Interval.of(codePoint, codePoint);
    int pos = Collections.binarySearch(this.intervals, key, INTERVAL_WITHIN_BOUND_COMPARATOR);
    if (pos < 0) {
      return null;
    }
    return intervals.get(pos);
  }

  /**
   * Retrieves all intervals intersecting with the given value.
   *
   * @param codePoint the value to find intersections for
   * @return a list of intervals intersecting with the given value
   */
  public List<Interval> getIntersection(int codePoint) {
    return getIntersection(codePoint, codePoint);
  }

  /**
   * Retrieves all intervals intersecting with the given interval.
   *
   * @param interval the value to find intersections for
   * @return a list of intervals intersecting with the given value
   */
  public List<Interval> getIntersection(Interval interval) {
    return getIntersection(interval.start(), interval.end());
  }

  /**
   * Retrieves all unique intervals from this set that intersect with any of the intervals in the
   * given collection.
   *
   * @param intervals the collection of intervals to find intersections with
   * @return a list of unique intervals from this set that intersect with at least one interval in
   *     the given collection
   */
  public List<Interval> getIntersection(Collection<Interval> intervals) {
    return intervals.stream().map(this::getIntersection).flatMap(List::stream).distinct().toList();
  }

  /**
   * Retrieves all intervals intersecting with the given range.
   *
   * @param start the starting point of the range (inclusive)
   * @param end the ending point of the range (exclusive)
   * @return a list of intervals intersecting with the given range
   */
  public List<Interval> getIntersection(int start, int end) {
    checkAndDoDisjoint();
    Interval key = Interval.of(start, end);
    int pos = Collections.binarySearch(this.intervals, key);
    if (pos < 0) {
      return List.of();
    }
    Interval interval = intervals.get(pos);
    if (start == end && interval.start() != interval.end()) {
      return List.of();
    }
    int left = pos;
    while (left >= 0 && intervals.get(left).start() > start) {
      left--;
    }
    if (left < 0) {
      return List.of();
    }
    int right = pos;
    while (right < size() && intervals.get(right).end() < end) {
      right++;
    }
    if (right >= size()) {
      return List.of();
    }
    List<Interval> result = new ArrayList<>(right - left + 1);
    for (int i = left; i <= right; i++) {
      result.add(intervals.get(i));
    }
    return result;
  }

  /** Ensures that the internal state is disjointed. */
  private void checkAndDoDisjoint() {
    if (!disjointed) {
      doDisjoint();
    }
  }

  /** Merges overlapping intervals in the set, ensuring it remains disjointed. */
  private void doDisjoint() {
    if (disjointed || isEmpty()) {
      return;
    }
    // type = 1 if event is a start event, otherwise it's -1
    record Event(int position, int type) {}
    List<Event> events = new ArrayList<>();
    for (Interval interval : intervals) {
      events.add(new Event(interval.start(), 1));
      events.add(new Event(interval.end() + 1, -1));
    }
    events.sort(
        (e1, e2) ->
            e1.position != e2.position
                ? Integer.compare(e1.position, e2.position)
                : Integer.compare(e1.type, e2.type));
    List<Interval> result = new ArrayList<>(intervals.size());
    int active = 0;
    int prevPosition = Integer.MAX_VALUE;
    for (Event e : events) {
      if (active > 0 && prevPosition < e.position) {
        result.add(Interval.of(prevPosition, e.position - 1));
      }
      active += e.type;
      prevPosition = e.position;
    }
    this.intervals = new LinkedList<>(result);
    this.disjointed = true;
  }

  /**
   * Returns the APPROXIMATE number of intervals currently stored in the set. The size is
   * approximate since this data structure is Lazy. The actual size is determined when implicit call
   * to doDisjoint() method is invoked.
   *
   * @return the size of the set
   */
  public int size() {
    return this.intervals.size();
  }

  public boolean isEmpty() {
    return this.intervals.isEmpty();
  }

  /**
   * Returns an immutable copy of the stored intervals.
   *
   * @return a list of intervals in the set
   */
  public List<Interval> intervals() {
    checkAndDoDisjoint();
    return List.copyOf(this.intervals);
  }

  /**
   * Returns the minimum value present in the set.
   *
   * @return the smallest value among all intervals in the set
   */
  public int minVal() {
    return minVal;
  }

  /**
   * Returns the maximum value present in the set.
   *
   * @return the largest value among all intervals in the set
   */
  public int maxVal() {
    return maxVal;
  }

  @Override
  public String toString() {
    return this.intervals.stream().map(Interval::toString).collect(Collectors.joining(", "));
  }
}
