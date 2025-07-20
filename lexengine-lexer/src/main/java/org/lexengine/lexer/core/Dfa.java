/*
 * Copyright (c) 2024 lex-engine
 * Author: Pradeesh Kumar
 */
package org.lexengine.lexer.core;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Deterministic Finite Automaton (DFA).
 *
 * <p>A DFA is a mathematical model used to recognize patterns in strings. It consists of a finite
 * number of states, transitions between those states, and actions associated with certain states.
 *
 * <p>This implementation provides methods for creating and manipulating DFAs, including setting up
 * transitions, marking final states, and testing input strings against the DFA.
 */
public class Dfa {

  /** Maximum buffer size for state arrays. */
  private static final int MAX_STATE_BUFFER = 512;

  /** Transition table mapping states to next states based on input alphabets. */
  private int[][] transitionTbl;

  /** Starting state of the DFA. */
  private int startState;

  /** Number of states in the DFA. */
  private int statesCount;

  private final int alphabetSize;
  private final Map<Integer, Action> actionMap;
  private final BitSet finalStates;
  private final DisjointIntSet languageAlphabets;
  private final Map<Range, Integer> alphabetIndex;

  /**
   * Constructs a new DFA with the specified parameters.
   *
   * @param statesCount number of states in the DFA
   * @param languageAlphabets set of language alphabets used by the DFA
   * @param alphabetIndex mapping of ranges to alphabet indices
   */
  public Dfa(int statesCount, DisjointIntSet languageAlphabets, Map<Range, Integer> alphabetIndex) {
    this.alphabetSize = languageAlphabets.size();
    this.statesCount = 1; // 0 is dedicated for phi state
    this.transitionTbl = new int[statesCount + 1][this.alphabetSize];
    this.finalStates = new BitSet();
    this.actionMap = new HashMap<>();
    this.languageAlphabets = languageAlphabets;
    this.alphabetIndex = alphabetIndex;
  }

  /**
   * Creates a new state in the DFA.
   *
   * @return index of the newly created state
   */
  public int createState() {
    ensureCapacity();
    return statesCount++;
  }

  /**
   * Returns the number of states in the DFA.
   *
   * @return number of states
   */
  public int statesCount() {
    return statesCount - 1;
  }

  /**
   * Returns a clone of the bitset indicating which states are final.
   *
   * @return cloned bitset
   */
  public BitSet finalStates() {
    return (BitSet) finalStates.clone();
  }

  /**
   * Returns the action map
   *
   * @return action map
   */
  public Map<Integer, Action> actions() {
    return actionMap;
  }

  /**
   * Returns a copy of the transition table, excluding the phi-state.
   *
   * <p>The returned table maps states to next states based on input alphabets. Each row represents
   * a state, and each column represents an alphabet. The cell at row `i` and column `j` contains
   * the index of the next state reached by transitioning from state `i` on alphabet `j`.
   *
   * @return a copy of the transition table
   */
  public int[][] transitionTbl() {
    int[][] result = new int[statesCount - 1][alphabetSize];
    System.arraycopy(this.transitionTbl, 0, result, 0, statesCount - 1);
    return result;
  }

  /**
   * Returns the set of language alphabets used by the DFA.
   *
   * @return set of language alphabets
   */
  public DisjointIntSet languageAlphabets() {
    return languageAlphabets;
  }

  /**
   * Returns the action associated with the specified state.
   *
   * @param state index of the state
   * @return associated action, or null if none
   */
  public Action action(int state) {
    return actionMap.get(state);
  }

  /**
   * Returns the mapping of Ranges to alphabet indices.
   *
   * @return mapping of Ranges to alphabet indices
   */
  public Map<Range, Integer> alphabetIndex() {
    return alphabetIndex;
  }

  /**
   * Returns the size of the alphabet used by the DFA.
   *
   * @return alphabet size
   */
  public int alphabetSize() {
    return alphabetSize;
  }

  /**
   * Returns the number of final states in the DFA.
   *
   * @return number of final states
   */
  public int finalStatesCount() {
    return finalStates.cardinality();
  }

  /**
   * Adds a transition from one state to another based on an input alphabet.
   *
   * @param fromState index of the source state
   * @param alphabet index of the input alphabet
   * @param toState index of the destination state
   */
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

  /**
   * Marks a state as final and associates an action with it.
   *
   * @param state index of the state
   * @param action associated action
   */
  public void addFinalState(int state, Action action) {
    this.finalStates.set(state);
    this.actionMap.put(state, action);
  }

  /**
   * Returns the starting state of the DFA.
   *
   * @return starting state index
   */
  public int startState() {
    return startState;
  }

  /**
   * Returns the transitions from one state to another based on an input alphabet.
   *
   * @param fromState index of the source state
   * @param alphabet index of the input alphabet
   * @return index of the destination state
   */
  public int transition(int fromState, int alphabet) {
    if (fromState < 0 || fromState >= statesCount) {
      throw new IllegalArgumentException("Invalid fromState: " + fromState);
    }
    if (alphabet < 0 || alphabet >= alphabetSize) {
      throw new IllegalArgumentException("Invalid alphabet: " + alphabet);
    }
    return transitionTbl[fromState][alphabet];
  }

  /**
   * Sets the starting state of the DFA.
   *
   * @param startState index of the new starting state
   */
  public void setStartState(int startState) {
    this.startState = startState;
  }

  /** Ensures that the internal state array has sufficient capacity. */
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

  /**
   * Tests whether the DFA accepts a given input string.
   *
   * @param input string to test
   * @return associated action if accepted, or null if rejected
   */
  public Action test(String input) {
    int currentState = this.startState;
    for (int i = 0; i < input.length(); i++) {
      Range range = languageAlphabets.getRange(input.charAt(i));
      Integer alphaIndex = alphabetIndex.get(range);
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
