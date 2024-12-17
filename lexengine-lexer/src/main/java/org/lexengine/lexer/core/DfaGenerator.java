/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.util.BitSet;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import org.lexengine.lexer.logging.Out;

/**
 * Responsible for generating a Deterministic Finite Automaton (DFA) from a Non-Deterministic Finite
 * Automaton (NFA). It uses Subset/Powerset Construction Algorithm to convert the epsilon NFA to
 * DFA.
 */
public final class DfaGenerator {

  private final Nfa nfa;
  private Dfa dfa;
  private final Map<BitSet, BitSet> stsetEpsilonClosureCache;
  private final Map<Integer, BitSet> stEpsilonTransitionCache;

  /**
   * Constructs a new instance of DfaGenerator with the specified NFA.
   *
   * @param nfa the non-deterministic finite automaton to convert
   */
  public DfaGenerator(Nfa nfa) {
    this.nfa = nfa;
    stsetEpsilonClosureCache = new HashMap<>();
    stEpsilonTransitionCache = new HashMap<>();
  }

  /**
   * Generates a DFA equivalent to the provided NFA.
   *
   * @return the generated DFA
   */
  public Dfa generate() {
    this.dfa = new Dfa(nfa.statesCount(), nfa.languageAlphabets(), nfa.alphabetIndex());
    Stack<BitSet> workList = new Stack<>();
    BitSet startState = epsilonClosure(nfa.startState());
    workList.push(startState);

    Map<BitSet, Integer> dfaStates = new HashMap<>();
    dfaStates.put(startState, dfa.createState());

    int alphabetCount = nfa.alphabetSize();
    while (!workList.isEmpty()) {
      BitSet curState = workList.pop();
      setFinalStateIfAny(curState, dfaStates.get(curState));

      for (int a = 0; a < alphabetCount; a++) {
        BitSet nextStateForAlphabet_a = epsilonClosure(deltaTransition(curState, a));
        if (nextStateForAlphabet_a.isEmpty()) {
          continue;
        }
        Integer dfaStateIndex = dfaStates.get(nextStateForAlphabet_a);
        if (dfaStateIndex == null) {
          dfaStateIndex = dfa.createState();
          dfaStates.put(nextStateForAlphabet_a, dfaStateIndex);
          workList.push(nextStateForAlphabet_a);
        }
        dfa.addTransition(dfaStates.get(curState), a, dfaStateIndex);
      }
    }
    // DFA start state is the epsilonClosure of nfa start state
    int dfaStartState = dfaStates.get(startState);
    dfa.setStartState(dfaStartState);
    Out.info(
        "DFA generated. Num States: %d, Num Final States: %d",
        dfa.statesCount(), dfa.finalStatesCount());
    stsetEpsilonClosureCache.clear();
    stEpsilonTransitionCache.clear();
    return dfa;
  }

  /**
   * Sets the final state and its corresponding action in the DFA if any of the states in the given
   * bit set are final states. If there are more than 1 final state present in the stateSet, the
   * action from the lowest state will be set to DFA state action.
   *
   * @param stateSet the current state
   * @param dfaStateIndex the index of the DFA state
   */
  private void setFinalStateIfAny(BitSet stateSet, int dfaStateIndex) {
    IntSummaryStatistics summary = stateSet.stream().filter(nfa::isFinalState).summaryStatistics();
    if (summary.getCount() == 0) {
      return;
    }
    if (summary.getCount() > 1) {
      // Out.warn("stateSet has more than 1 final state. Highest priority is given for first
      // declared state!");
    }
    dfa.addFinalState(dfaStateIndex, nfa.action(summary.getMin()));
  }

  /**
   * Computes the delta transition function for the given state and alphabet.
   *
   * @param state the current state
   * @param alphabet the input alphabet
   * @return the resulting state after applying the delta transition function
   */
  private BitSet deltaTransition(BitSet state, int alphabet) {
    BitSet alphaTransitions = new BitSet();
    state.stream()
        .mapToObj(s -> nfa.transition(s, alphabet))
        .filter(Objects::nonNull)
        .forEach(alphaTransitions::or);
    return alphaTransitions;
  }

  /**
   * Computes the epsilon closure of the given state set.
   *
   * @param stateSet the current state
   * @return the epsilon closure of the state
   */
  private BitSet epsilonClosure(BitSet stateSet) {
    BitSet fromCache = stsetEpsilonClosureCache.get(stateSet);
    if (fromCache != null) {
      return (BitSet) fromCache.clone();
    }
    BitSet epsilonTransitions = new BitSet();
    stateSet.stream().mapToObj(this::epsilonClosure).forEach(epsilonTransitions::or);
    stsetEpsilonClosureCache.put(stateSet, epsilonTransitions);
    return epsilonTransitions;
  }

  /**
   * Computes the reverse epsilon closure of the given state.
   *
   * @param state the current state
   * @return the reverse epsilon closure of the state
   */
  private BitSet epsilonClosure(int state) {
    BitSet fromCache = stEpsilonTransitionCache.get(state);
    if (fromCache != null) {
      return (BitSet) fromCache.clone();
    }
    BitSet epsilonTransitions = new BitSet();
    epsilonTransitions.set(state); // epsilon = state U epsilon(state)

    BitSet epsilonOfState = nfa.epsilonTransition(state);
    if (epsilonOfState == null || epsilonOfState.isEmpty()) {
      return epsilonTransitions;
    }
    epsilonOfState.stream()
        .peek(epsilonTransitions::set)
        .mapToObj(this::epsilonClosure)
        .forEach(epsilonTransitions::or);
    stEpsilonTransitionCache.put(state, epsilonTransitions);
    return epsilonTransitions;
  }
}
