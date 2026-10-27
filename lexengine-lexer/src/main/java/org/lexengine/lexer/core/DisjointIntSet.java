/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Lazily partitions inclusive integer ranges at every input boundary. A balanced interval index
 * supports point lookup in O(log n) and partition lookup in O(log n + k).
 */
public class DisjointIntSet {
  private List<Range> ranges;
  private final NavigableMap<Integer, Range> index = new TreeMap<>();
  private boolean disjointed;
  private int minVal = -1;
  private int maxVal = -1;

  public DisjointIntSet() {
    this(10);
  }

  public DisjointIntSet(int initialCapacity) {
    ranges = new ArrayList<>(initialCapacity);
  }

  public static DisjointIntSet from(Collection<Range> ranges) {
    DisjointIntSet set = new DisjointIntSet(ranges.size());
    ranges.forEach(set::add);
    return set;
  }

  public void add(int value) {
    add(Range.of(value));
  }

  /** Adds an inclusive range. */
  public void addRange(int start, int end) {
    add(Range.of(start, end));
  }

  public void add(Range range) {
    Objects.requireNonNull(range);
    boolean empty = ranges.isEmpty();
    ranges.add(range);
    minVal = empty ? range.start() : Math.min(minVal, range.start());
    maxVal = empty ? range.end() : Math.max(maxVal, range.end());
    disjointed = false;
  }

  public List<Range> getDifference(Collection<Range> other) {
    checkAndDoDisjoint();
    Set<Range> removed = Set.copyOf(other);
    if (!new HashSet<>(ranges).containsAll(removed))
      throw new IllegalArgumentException("Illegal characters in the input list");
    return ranges.stream().filter(range -> !removed.contains(range)).toList();
  }

  public Range getRange(int codePoint) {
    checkAndDoDisjoint();
    var entry = index.floorEntry(codePoint);
    return entry != null && entry.getValue().end() >= codePoint ? entry.getValue() : null;
  }

  public List<Range> getIntersection(int value) {
    return getIntersection(value, value);
  }

  public List<Range> getIntersection(Range range) {
    return getIntersection(range.start(), range.end());
  }

  public List<Range> getIntersection(Collection<Range> ranges) {
    Set<Range> result = new LinkedHashSet<>();
    for (Range range : ranges) result.addAll(getIntersection(range));
    return List.copyOf(result);
  }

  /** Returns complete alphabet partitions contained in the inclusive query, without clipping. */
  public List<Range> getIntersection(int start, int end) {
    Range.of(start, end);
    checkAndDoDisjoint();
    return index.subMap(start, true, end, true).values().stream()
        .filter(range -> range.end() <= end)
        .toList();
  }

  private void checkAndDoDisjoint() {
    if (disjointed) return;
    // Long boundary coordinates prevent overflow at Integer.MAX_VALUE + 1.
    NavigableMap<Long, Integer> events = new TreeMap<>();
    for (Range range : ranges) {
      events.merge((long) range.start(), 1, Integer::sum);
      events.merge((long) range.end() + 1, -1, Integer::sum);
    }
    List<Range> result = new ArrayList<>(events.size());
    int active = 0;
    long previous = 0;
    for (var event : events.entrySet()) {
      long position = event.getKey();
      if (active > 0 && previous < position)
        result.add(Range.of((int) previous, (int) (position - 1)));
      active += event.getValue();
      previous = position;
    }
    ranges = result;
    index.clear();
    for (Range range : ranges) index.put(range.start(), range);
    disjointed = true;
  }

  /** Number of ranges currently stored; overlaps are normalized on the next lookup. */
  public int size() {
    return ranges.size();
  }

  public boolean isEmpty() {
    return ranges.isEmpty();
  }

  public List<Range> ranges() {
    checkAndDoDisjoint();
    return List.copyOf(ranges);
  }

  public int minVal() {
    return minVal;
  }

  public int maxVal() {
    return maxVal;
  }

  @Override
  public String toString() {
    return ranges().stream().map(Range::toString).collect(Collectors.joining(", "));
  }
}
