/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class Dfa {

  private static final int MAX_STATE_BUFFER = 512;

  private int[][] transitionTbl;
  private int startState;
  private int statesCount;
  private final int alphabetSize;
  private Map<Integer, Action> actionMap;
  private BitSet finalStates;
  private final DisjointIntSet languageAlphabets;
  private final Map<Interval, Integer> alphabetIndex;

  public Dfa(
      int nfaStateCount, DisjointIntSet languageAlphabets, Map<Interval, Integer> alphabetIndex) {
    this.alphabetSize = languageAlphabets.size();
    this.statesCount = 0;
    this.transitionTbl = new int[nfaStateCount][this.alphabetSize];
    this.finalStates = new BitSet();
    this.actionMap = new HashMap<>();
    this.languageAlphabets = languageAlphabets;
    this.alphabetIndex = alphabetIndex;
  }

  public int createState() {
    ensureCapacity();
    return statesCount++;
  }

  public int statesCount() {
    return statesCount - 1;
  }

  public int finalStatesCount() {
    return finalStates.cardinality();
  }

  public void addTransition(int fromState, int alphabet, int toState) {
    if (fromState < 0 || fromState >= statesCount) {
      throw new IllegalArgumentException("Invalid fromState: " + fromState);
    }
    if (toState < 0 || toState >= statesCount) {
      throw new IllegalArgumentException("Invalid toState: " + toState);
    }
    if (alphabet < 0 || alphabet >= alphabetSize) {
      throw new IllegalArgumentException("Invalid alphabet: " + alphabet);
    }
    this.transitionTbl[fromState][alphabet] = toState;
  }

  public void addFinalState(int state, Action action) {
    this.finalStates.set(state);
    this.actionMap.put(state, action);
  }

  public void setStartState(int startState) {
    this.startState = startState;
  }

  private void ensureCapacity() {
    if (statesCount < transitionTbl.length) {
      return;
    }
    // New capacity is the minimum of currentSize + (currentSize * 1.5) or currentSize +
    // MAX_STATE_BUFFER
    int newCapacity = transitionTbl.length + Math.min(transitionTbl.length >> 1, MAX_STATE_BUFFER);
    int[][] newTransitionTbl = new int[newCapacity][this.alphabetSize];
    System.arraycopy(transitionTbl, 0, newTransitionTbl, 0, transitionTbl.length);
    transitionTbl = newTransitionTbl;
  }

  public Action test(String input) {
    int currentState = this.startState;
    for (int i = 0; i < input.length(); i++) {
      Interval interval = languageAlphabets.getInterval(input.charAt(i));
      Integer alphaIndex = alphabetIndex.get(interval);
      if (alphaIndex == null) {
        return null;
      }
      int nextState = this.transitionTbl[currentState][alphaIndex];
      if (nextState == 0) { // phi-state
        return null;
      }
      currentState = nextState;
    }
    return finalStates.get(currentState) ? actionMap.get(currentState) : null;
  }
}
