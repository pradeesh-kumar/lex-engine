/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Utility class providing helper methods for lexical analysis tasks. */
public final class LexGenUtils {

  /**
   * Extracts alphabets from a list of regular expressions and adds them to a disjoint set of
   * language alphabets.
   *
   * @param regexActions a list of regular expression actions
   * @param languageAlphabets a disjoint set of language alphabets
   */
  static void extractAlphabetsFromRegex(
      List<RegexAction> regexActions, DisjointIntSet languageAlphabets) {
    regexActions.stream()
        .map(RegexAction::regex)
        .map(Regex::extractAlphabets)
        .flatMap(List::stream)
        .forEach(languageAlphabets::add);
  }

  /**
   * Creates an index mapping intervals to unique integers.
   *
   * @param intervals a list of intervals
   * @return a map where each interval is associated with a unique integer index
   */
  static Map<Interval, Integer> createAlphabetsIndex(List<Interval> intervals) {
    Map<Interval, Integer> alphabetIndex = new HashMap<>(intervals.size());
    int index = 0;
    for (var interval : intervals) {
      alphabetIndex.put(interval, index++);
    }
    return alphabetIndex;
  }
}
