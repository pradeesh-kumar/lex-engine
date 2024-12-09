/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.util.BitSet;
import java.util.Collection;

public class Nfa {

  private static final int MAX_STATE_BUFFER = 512;
  private static final int INITIAL_SIZE = 50;
  private static final int EPSILON_STATE_INDEX = 0;

  /**
   * transitionTbl[current_state][next_char] gives the set of states which can be reached from
   * current_state with an input next_char
   */
  private final int alphabetSize;

  private BitSet[][] transitionTbl;
  private int statesCount;
  private NfaState state;
  private BitSet finalStates;
  private Action[] actions;

  public Nfa(int alphabetSize) {
    this.alphabetSize = alphabetSize + 1; // +1 extra for epsilon
    this.transitionTbl = new BitSet[INITIAL_SIZE][this.alphabetSize];
    this.statesCount = 0;
    this.finalStates = new BitSet();
  }

  private int createState() {
    ensureCapacity();
    return statesCount++;
  }

  private void ensureCapacity() {
    if (statesCount >= transitionTbl.length) {
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
    this.transitionTbl[fromState][alphabet].set(toState);
  }

  public void setState(NfaState state) {
    this.state = state;
  }

  class NfaState {

    private int start;
    private int accept;

    NfaState(int alphabet) {
      this.start = createState();
      this.accept = createState();
      addTransition(start, alphabet, accept);
      finalStates.set(this.accept);
    }

    private NfaState(int start, int accept) {
      this.start = start;
      this.accept = accept;
    }

    void concat(NfaState other) {
      finalStates.clear(this.accept);
      addTransition(this.accept, EPSILON_STATE_INDEX, other.accept);
      this.accept = other.accept;
      finalStates.set(this.accept);
    }

    void alternateAndConcat(Collection<NfaState> others) {
      finalStates.clear(this.accept);

      int newStart = createState();
      int newAccept = createState();

      others.forEach(
          o -> {
            finalStates.clear(o.accept);
            addTransition(newStart, EPSILON_STATE_INDEX, o.start);
            addTransition(o.accept, EPSILON_STATE_INDEX, newAccept);
          });
      NfaState newState = new NfaState(newStart, newAccept);
      this.concat(newState);
    }

    void alternate(NfaState other) {
      finalStates.clear(this.accept);
      finalStates.clear(other.accept);

      int newStart = createState();
      int newAccept = createState();

      addTransition(newStart, EPSILON_STATE_INDEX, this.start);
      addTransition(newStart, EPSILON_STATE_INDEX, other.start);
      addTransition(this.accept, EPSILON_STATE_INDEX, newAccept);
      addTransition(other.accept, EPSILON_STATE_INDEX, newAccept);

      this.start = newStart;
      this.accept = newAccept;
      finalStates.set(this.accept);
    }

    void closure() {
      finalStates.clear(this.accept);
      int newStart = createState();
      int newAccept = createState();

      addTransition(newStart, EPSILON_STATE_INDEX, this.start);
      addTransition(this.accept, EPSILON_STATE_INDEX, newAccept);
      addTransition(this.accept, EPSILON_STATE_INDEX, this.start);
      addTransition(newStart, EPSILON_STATE_INDEX, newAccept);

      this.start = newStart;
      this.accept = newAccept;
      finalStates.set(this.accept);
    }
  }
}
