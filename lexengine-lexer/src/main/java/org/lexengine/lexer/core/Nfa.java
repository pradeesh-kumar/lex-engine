/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Nfa {

  private static final int MAX_STATE_BUFFER = 512;
  private static final int INITIAL_SIZE = 50;
  private static final int EPSILON_ALPHABET_INDEX = 0;

  /**
   * transitionTbl[current_state][next_char] gives the set of states which can be reached from
   * current_state with an input next_char
   */
  private BitSet[][] transitionTbl;

  private final DisjointIntSet languageAlphabets;
  private final Map<Interval, Integer> alphabetIndex;
  private final int alphabetSize;
  private final BitSet finalStates;
  private final Map<Integer, Action> actionMap;
  private int statesCount;
  private int startState;

  public Nfa(DisjointIntSet languageAlphabets, Map<Interval, Integer> alphabetIndex) {
    this.languageAlphabets = languageAlphabets;
    this.alphabetIndex = alphabetIndex;
    this.alphabetSize = languageAlphabets.size() + 1; // +1 extra for epsilon
    this.transitionTbl = new BitSet[INITIAL_SIZE][this.alphabetSize];
    this.statesCount = 0;
    this.finalStates = new BitSet();
    this.actionMap = new HashMap<>();
  }

  private int createState() {
    ensureCapacity();
    return statesCount++;
  }

  private void ensureCapacity() {
    if (statesCount < transitionTbl.length) {
      return;
    }
    // New capacity is the minimum of currentSize + (currentSize * 1.5) or currentSize +
    // MAX_STATE_BUFFER
    int newCapacity = transitionTbl.length + Math.min(transitionTbl.length >> 1, MAX_STATE_BUFFER);
    BitSet[][] newTransitionTbl = new BitSet[newCapacity][this.alphabetSize];
    System.arraycopy(transitionTbl, 0, newTransitionTbl, 0, transitionTbl.length);
    transitionTbl = newTransitionTbl;
  }

  private void addTransition(int fromState, int alphabet, int toState) {
    if (transitionTbl[fromState][alphabet] == null) {
      transitionTbl[fromState][alphabet] = new BitSet();
    }
    transitionTbl[fromState][alphabet].set(toState);
  }

  public void setStartState(int startState) {
    if (startState < 0 || startState >= this.transitionTbl.length) {
      throw new IllegalArgumentException("Invalid start state: " + startState);
    }
    this.startState = startState;
  }

  public int startState() {
    return startState;
  }

  public int statesCount() {
    return statesCount;
  }

  Action test(String input) {
    return testRecursive(input, 0, this.startState);
  }

  private Action testRecursive(String input, int pos, int curState) {
    if (finalStates.get(curState) && pos >= input.length()) {
      return actionMap.get(curState);
    }
    BitSet transitions = null;
    if (pos < input.length()) {
      char ch = input.charAt(pos);
      Interval interval = languageAlphabets.getInterval(ch);
      Integer alphaIndex = alphabetIndex.get(interval);
      if (alphaIndex == null) {
        return null;
      }
      transitions = transitionTbl[curState][alphaIndex];
      if (transitions != null) {
        Optional<Action> action = transitions.stream()
                .mapToObj(nextState -> testRecursive(input, pos + 1, nextState))
                .filter(Objects::nonNull)
                .findFirst();
        if (action.isPresent()) {
          return action.get();
        }
      }
    }
    transitions = transitionTbl[curState][EPSILON_ALPHABET_INDEX];
    if (transitions != null) {
      Optional<Action> action =
              transitions.stream()
                      .mapToObj(nextState -> testRecursive(input, pos, nextState))
                      .filter(Objects::nonNull)
                      .findFirst();
      if (action.isPresent()) {
        return action.get();
      }
    }
    return null;
  }

  class NfaState {

    private int start;
    private int accept;
    private boolean closureDone;
    private boolean alternateDone;

    NfaState(int alphabet) {
      this.start = createState();
      this.accept = createState();
      addTransition(start, alphabet, accept);
      finalStates.set(this.accept);
    }

    public int start() {
      return start;
    }

    public int accept() {
      return accept;
    }

    void registerAction(Action action) {
      actionMap.put(this.accept, action);
    }

    void concat(NfaState other) {
      finalStates.clear(this.accept);
      addTransition(this.accept, EPSILON_ALPHABET_INDEX, other.start);
      this.accept = other.accept;
      finalStates.set(this.accept);
      closureDone = false;
      alternateDone = false;
    }

    void alternate(NfaState other) {
      if (alternateDone) {
        finalStates.clear(other.accept);
        addTransition(this.start, EPSILON_ALPHABET_INDEX, other.start);
        addTransition(other.accept, EPSILON_ALPHABET_INDEX, this.accept);
        return;
      }

      finalStates.clear(this.accept);
      finalStates.clear(other.accept);

      int newStart = createState();
      int newAccept = createState();

      addTransition(newStart, EPSILON_ALPHABET_INDEX, this.start);
      addTransition(newStart, EPSILON_ALPHABET_INDEX, other.start);
      addTransition(this.accept, EPSILON_ALPHABET_INDEX, newAccept);
      addTransition(other.accept, EPSILON_ALPHABET_INDEX, newAccept);

      this.start = newStart;
      this.accept = newAccept;
      finalStates.set(newAccept);
      alternateDone = true;
      closureDone = false;
    }

    NfaState alternateWithoutNewAccept(NfaState other) {
      if (alternateDone) {
        addTransition(this.start, EPSILON_ALPHABET_INDEX, other.start);
        return this;
      }
      int newStart = createState();
      addTransition(newStart, EPSILON_ALPHABET_INDEX, this.start);
      addTransition(newStart, EPSILON_ALPHABET_INDEX, other.start);
      this.start = newStart;
      alternateDone = true;
      return this;
    }

    void closure() {
      if (closureDone) {
        return;
      }
      finalStates.clear(this.accept);
      int newStart = createState();
      int newAccept = createState();

      addTransition(newStart, EPSILON_ALPHABET_INDEX, this.start);
      addTransition(this.accept, EPSILON_ALPHABET_INDEX, newAccept);
      addTransition(this.accept, EPSILON_ALPHABET_INDEX, this.start);
      addTransition(newStart, EPSILON_ALPHABET_INDEX, newAccept);

      this.start = newStart;
      this.accept = newAccept;
      finalStates.set(this.accept);
      closureDone = true;
      alternateDone = false;
    }

    void zeroOrOne() {
      int newStart = createState();
      int newAccept = createState();

      finalStates.clear(this.accept);

      addTransition(newStart, EPSILON_ALPHABET_INDEX, newAccept);
      addTransition(newStart, EPSILON_ALPHABET_INDEX, this.start);
      addTransition(this.accept, EPSILON_ALPHABET_INDEX, newAccept);

      finalStates.set(newAccept);
      this.start = newStart;
      this.accept = newAccept;
    }

    void oneOrMore() {
      int newStart = createState();
      int newAccept = createState();

      finalStates.clear(this.accept);

      addTransition(newStart, EPSILON_ALPHABET_INDEX, this.start);
      addTransition(this.accept, EPSILON_ALPHABET_INDEX, newAccept);
      addTransition(newStart, EPSILON_ALPHABET_INDEX, newAccept);

      finalStates.set(newAccept);
      this.start = newStart;
      this.accept = newAccept;
    }
  }
}
