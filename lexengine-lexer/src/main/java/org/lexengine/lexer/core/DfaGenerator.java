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

public final class DfaGenerator {

  private final Nfa nfa;
  private Dfa dfa;
  private final Map<BitSet, BitSet> stsetEpsilonClosureCache;
  private final Map<Integer, BitSet> stEpsilonTransitionCache;

  public DfaGenerator(Nfa nfa) {
    this.nfa = nfa;
    stsetEpsilonClosureCache = new HashMap<>();
    stEpsilonTransitionCache = new HashMap<>();
  }

  public Dfa generate() {
    this.dfa = new Dfa(nfa.statesCount(), nfa.languageAlphabets(), nfa.alphabetIndex());
    Stack<BitSet> workList = new Stack<>();
    BitSet startState = reverseEpsilonClosureOf(nfa.startState());
    workList.push(startState);

    Map<BitSet, Integer> dfaStates = new HashMap<>();
    dfaStates.put(startState, dfa.createState());

    int alphabetCount = nfa.alphabetSize();
    while (!workList.isEmpty()) {
      BitSet curState = workList.pop();
      setFinalStateIfAny(curState, dfaStates.get(curState));

      for (int a = 0; a < alphabetCount; a++) {
        BitSet nextStateForAlphabet_a = epsilonClosureOf(deltaTransition(curState, a));
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
    int dfaStartState = dfaStates.get(startState);
    dfa.setStartState(dfaStartState);
    Out.info(
        "DFA generated. Num States: %d, Num Final States: %d",
        dfa.statesCount(), dfa.finalStatesCount());
    return dfa;
  }

  private void setFinalStateIfAny(BitSet curState, int dfaStateIndex) {
    IntSummaryStatistics summary = curState.stream().filter(nfa::isFinalState).summaryStatistics();
    if (summary.getCount() == 0) {
      return;
    }
    if (summary.getCount() > 1) {
      // Out.warn("stateSet has more than 1 final state. Highest priority is given for first
      // declared state!");
    }
    dfa.addFinalState(dfaStateIndex, nfa.action(summary.getMin()));
  }

  private BitSet deltaTransition(BitSet state, int alphabet) {
    BitSet alphaTransitions = new BitSet();
    state.stream()
        .mapToObj(s -> nfa.transition(s, alphabet))
        .filter(Objects::nonNull)
        .forEach(alphaTransitions::or);
    return alphaTransitions;
  }

  private BitSet epsilonClosureOf(BitSet state) {
    BitSet fromCache = stsetEpsilonClosureCache.get(state);
    if (fromCache != null) {
      return (BitSet) fromCache.clone();
    }
    BitSet epsilonTransitions = new BitSet();
    state.stream().mapToObj(this::reverseEpsilonClosureOf).forEach(epsilonTransitions::or);
    stsetEpsilonClosureCache.put(state, epsilonTransitions);
    return epsilonTransitions;
  }

  private BitSet reverseEpsilonClosureOf(int state) {
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
        .mapToObj(this::reverseEpsilonClosureOf)
        .forEach(epsilonTransitions::or);
    stEpsilonTransitionCache.put(state, epsilonTransitions);
    return epsilonTransitions;
  }
}
