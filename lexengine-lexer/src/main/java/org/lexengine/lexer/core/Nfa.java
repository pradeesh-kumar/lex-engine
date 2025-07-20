/*
 * Copyright (c) 2024 lex-engine
 * Author: Pradeesh Kumar
 */
package org.lexengine.lexer.core;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Nfa {

  private static final int MAX_STATE_BUFFER = 512;
  private static final int INITIAL_SIZE = 50;

  /**
   * transitionTbl[current_state][next_char] gives the set of states which can be reached from
   * current_state with an input next_char
   */
  private BitSet[][] transitionTbl;

  private final DisjointIntSet languageAlphabets;
  private final Map<Range, Integer> alphabetIndex;
  private final int alphabetSize;
  private final BitSet finalStates;
  private final Map<Integer, Action> actionMap;
  private int statesCount;
  private int startState;
  private final int epsilonAlphabetIndex;

  public Nfa(DisjointIntSet languageAlphabets, Map<Range, Integer> alphabetIndex) {
    this.languageAlphabets = languageAlphabets;
    this.alphabetIndex = alphabetIndex;
    this.alphabetSize = languageAlphabets.size() + 1; // + 1 extra for epsilon
    this.transitionTbl = new BitSet[INITIAL_SIZE][this.alphabetSize];
    this.epsilonAlphabetIndex =
        this.alphabetSize - 1; // Last alphabet index is dedicated for epsilon
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

  public int epsilonAlphabetIndex() {
    return epsilonAlphabetIndex;
  }

  public int statesCount() {
    return statesCount - 1;
  }

  public int finalStatesCount() {
    return finalStates.cardinality();
  }

  public int alphabetSize() {
    return alphabetSize - 1; // Excluding epsilon
  }

  public boolean isFinalState(int state) {
    return finalStates.get(state);
  }

  public Action action(int state) {
    return actionMap.get(state);
  }

  public DisjointIntSet languageAlphabets() {
    return languageAlphabets;
  }

  public Map<Range, Integer> alphabetIndex() {
    return alphabetIndex;
  }

  public BitSet transition(int fromState, int alphabet) {
    if (fromState < 0 || fromState >= statesCount) {
      throw new IllegalArgumentException("Invalid fromState: " + fromState);
    }
    if (alphabet < 0 || alphabet >= alphabetSize) {
      throw new IllegalArgumentException("Invalid alphabet: " + alphabet);
    }
    return transitionTbl[fromState][alphabet];
  }

  public BitSet epsilonTransition(int fromState) {
    if (fromState < 0 || fromState >= statesCount) {
      throw new IllegalArgumentException("Invalid epsilon transition from: " + fromState);
    }
    return transitionTbl[fromState][epsilonAlphabetIndex];
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
      Range range = languageAlphabets.getRange(ch);
      Integer alphaIndex = alphabetIndex.get(range);
      if (alphaIndex == null) {
        return null;
      }
      transitions = transitionTbl[curState][alphaIndex];
      if (transitions != null) {
        Optional<Action> action =
            transitions.stream()
                .mapToObj(nextState -> testRecursive(input, pos + 1, nextState))
                .filter(Objects::nonNull)
                .findFirst();
        if (action.isPresent()) {
          return action.get();
        }
      }
    }
    transitions = transitionTbl[curState][epsilonAlphabetIndex];
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
      addTransition(this.accept, epsilonAlphabetIndex, other.start);
      this.accept = other.accept;
      finalStates.set(this.accept);
      closureDone = false;
      alternateDone = false;
    }

    void alternate(NfaState other) {
      if (alternateDone) {
        finalStates.clear(other.accept);
        addTransition(this.start, epsilonAlphabetIndex, other.start);
        addTransition(other.accept, epsilonAlphabetIndex, this.accept);
        return;
      }

      finalStates.clear(this.accept);
      finalStates.clear(other.accept);

      int newStart = createState();
      int newAccept = createState();

      addTransition(newStart, epsilonAlphabetIndex, this.start);
      addTransition(newStart, epsilonAlphabetIndex, other.start);
      addTransition(this.accept, epsilonAlphabetIndex, newAccept);
      addTransition(other.accept, epsilonAlphabetIndex, newAccept);

      this.start = newStart;
      this.accept = newAccept;
      finalStates.set(newAccept);
      alternateDone = true;
      closureDone = false;
    }

    NfaState alternateWithoutNewAccept(NfaState other) {
      if (alternateDone) {
        addTransition(this.start, epsilonAlphabetIndex, other.start);
        return this;
      }
      int newStart = createState();
      addTransition(newStart, epsilonAlphabetIndex, this.start);
      addTransition(newStart, epsilonAlphabetIndex, other.start);
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

      addTransition(newStart, epsilonAlphabetIndex, this.start);
      addTransition(this.accept, epsilonAlphabetIndex, newAccept);
      addTransition(this.accept, epsilonAlphabetIndex, this.start);
      addTransition(newStart, epsilonAlphabetIndex, newAccept);

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

      addTransition(newStart, epsilonAlphabetIndex, newAccept);
      addTransition(newStart, epsilonAlphabetIndex, this.start);
      addTransition(this.accept, epsilonAlphabetIndex, newAccept);

      finalStates.set(newAccept);
      this.start = newStart;
      this.accept = newAccept;
    }

    void oneOrMore() {
      int newStart = createState();
      int newAccept = createState();

      finalStates.clear(this.accept);

      addTransition(newStart, epsilonAlphabetIndex, this.start);
      addTransition(this.accept, epsilonAlphabetIndex, newAccept);
      addTransition(newStart, epsilonAlphabetIndex, newAccept);

      finalStates.set(newAccept);
      this.start = newStart;
      this.accept = newAccept;
    }
  }
}
