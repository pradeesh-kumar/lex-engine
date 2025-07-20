/*
* Copyright (c) 2024 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.lexengine.commons.logging.Out;

/**
 * Responsible for generating a minimal Deterministic Finite Automaton (DFA) from a given DFA. It
 * uses Hopcroft’s Algorithm to convert the DFA to minimal DFA.
 */
public class DfaMinimizer {

  private final Dfa dfa;
  private final int alphabetSize;

  /**
   * Constructs a new DfaMinimizer instance with the specified DFA.
   *
   * @param dfa the DFA to be minimized
   */
  public DfaMinimizer(Dfa dfa) {
    this.dfa = dfa;
    this.alphabetSize = dfa.alphabetSize();
  }

  /**
   * Minimizes the DFA using Hopcroft’s Algorithm.
   *
   * @return the minimized DFA
   */
  public Dfa minimize() {
    List<BitSet> partitions = getInitialPartitions();
    List<BitSet> newPartitions = new LinkedList<>();
    while (partitions.size() != newPartitions.size()) {
      newPartitions = partitions;
      partitions = new LinkedList<>();

      for (BitSet partition : newPartitions) {
        List<BitSet> split = split(partition);
        partitions.addAll(split);
      }
    }
    Out.debug("DFA Minimization partitions: %s", partitions);
    if (!requireMinimization(partitions)) {
      Out.info("No equivalent states found in the DFA! Minimization not required.");
      return this.dfa;
    }
    Dfa minDfa = createMinDfa(partitions);
    Out.info(
        "DFA Minimized. Num States: %d, Num Final States: %d",
        minDfa.statesCount(), minDfa.finalStatesCount());
    return minDfa;
  }

  /**
   * Splits a stateSet if its transition for any alphabet isn't the part of itself.
   *
   * @param stateSet the partition to be split
   * @return a list of sub-partitions
   */
  private List<BitSet> split(BitSet stateSet) {
    int[] states = stateSet.stream().toArray();
    if (states.length < 2) {
      return List.of(stateSet);
    }
    for (int state : states) {
      boolean noTransition = false;
      for (int c = 0; c < alphabetSize; c++) {
        int transition = dfa.transition(state, c);
        if (transition != 0) {
          if (!stateSet.get(transition)) {
            stateSet.clear(state);
            BitSet newStateSet = new BitSet();
            newStateSet.set(state);
            return List.of(stateSet, newStateSet);
          } else {
            noTransition = true;
          }
        }
      }
      // If there is any state with no transition at all, then split it.
      if (!noTransition) {
        stateSet.clear(state);
        BitSet newStateSet = new BitSet();
        newStateSet.set(state);
        return List.of(stateSet, newStateSet);
      }
    }
    return List.of(stateSet);
  }

  /**
   * Gets the initial partitions of the DFA states.
   *
   * @return a list of initial partitions
   */
  private List<BitSet> getInitialPartitions() {
    BitSet finalStates = dfa.finalStates();
    BitSet nonFinalStates = new BitSet();
    for (int s = 1; s <= dfa.statesCount(); s++) {
      if (!finalStates.get(s)) {
        nonFinalStates.set(s);
      }
    }
    /*
     * If there are more than 1 final actions with different states,
     * then we split them now itself to avoid them being clubbed as a single state
     */
    Map<Action, List<Integer>> finalStatesByAction =
        finalStates.stream().boxed().collect(Collectors.groupingBy(dfa::action));
    List<BitSet> partitions = new LinkedList<>();
    if (!nonFinalStates.isEmpty()) {
      partitions.add(nonFinalStates);
    }
    finalStatesByAction.values().stream()
        .filter(fs -> !fs.isEmpty())
        .forEach(
            fs -> {
              BitSet finalStateSet = new BitSet();
              fs.forEach(finalStateSet::set);
              partitions.add(finalStateSet);
            });
    return partitions;
  }

  /**
   * Creates a new minimized DFA from the given partitions.
   *
   * @param partitions the partitions of the original DFA states
   * @return the minimized DFA
   */
  private Dfa createMinDfa(List<BitSet> partitions) {
    Dfa minDfa = new Dfa(partitions.size(), dfa.languageAlphabets(), dfa.alphabetIndex());
    minDfa.createState(); // For phi-state
    for (int i = 0; i < partitions.size(); i++) {
      minDfa.createState();
    }
    Map<Integer, Integer> newStates = new HashMap<>(partitions.size());
    BitSet dfaFinalState = dfa.finalStates();
    int newStateIndex = 1;
    for (BitSet partition : partitions) {
      int[] states = partition.stream().toArray();
      for (int state : states) {
        newStates.put(state, newStateIndex);
        if (dfaFinalState.get(state)) {
          minDfa.addFinalState(newStateIndex, dfa.action(state));
          Out.debug(
              "MinDfa attaching action to the minDfa final State %d -> %s",
              newStateIndex, dfa.action(state));
        }
      }
      ++newStateIndex;
    }

    for (BitSet stateSet : partitions) {
      int[] dfaStates = stateSet.stream().toArray();
      for (int s : dfaStates) {
        int minDfaState = newStates.get(s);
        for (int a = 0; a < alphabetSize; a++) {
          int transition = dfa.transition(s, a);
          if (transition != 0) {
            minDfa.addTransition(minDfaState, a, newStates.get(transition));
          }
        }
      }
    }
    minDfa.setStartState(newStates.get(dfa.startState()));
    return minDfa;
  }

  /**
   * Checks if minimization is required based on the given partitions.
   *
   * @param partitions the partitions of the original DFA states
   * @return true if minimization is required, false otherwise
   */
  private boolean requireMinimization(List<BitSet> partitions) {
    return partitions.stream().anyMatch(b -> b.cardinality() > 1);
  }
}
