/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import java.util.*;
import org.lexengine.commons.logging.Out;

/** Minimizes a DFA by refining action-equivalent states using their full transition signatures. */
public class DfaMinimizer {
  private final Dfa dfa;

  public DfaMinimizer(Dfa dfa) {
    this.dfa = dfa;
  }

  public Dfa minimize() {
    Map<LexRule.Action, BitSet> groups = new LinkedHashMap<>();
    for (int state = 1; state <= dfa.statesCount(); state++)
      groups.computeIfAbsent(dfa.action(state), ignored -> new BitSet()).set(state);
    List<BitSet> partitions = new ArrayList<>(groups.values());
    while (true) {
      int[] blocks = blocks(partitions);
      List<BitSet> refined = new ArrayList<>();
      for (BitSet partition : partitions) {
        Map<List<Integer>, BitSet> signatures = new LinkedHashMap<>();
        for (int state : partition.stream().toArray()) {
          List<Integer> signature = new ArrayList<>(dfa.alphabetSize());
          for (int alphabet = 0; alphabet < dfa.alphabetSize(); alphabet++)
            signature.add(blocks[dfa.transition(state, alphabet)]);
          signatures.computeIfAbsent(signature, ignored -> new BitSet()).set(state);
        }
        refined.addAll(signatures.values());
      }
      if (refined.size() == partitions.size()) break;
      partitions = refined;
    }
    if (partitions.size() == dfa.statesCount()) return dfa;
    int[] blocks = blocks(partitions);
    Dfa minimized = new Dfa(partitions.size(), dfa.languageAlphabets(), dfa.alphabetIndex());
    for (int i = 0; i < partitions.size(); i++) minimized.createState();
    for (int i = 0; i < partitions.size(); i++) {
      int representative = partitions.get(i).nextSetBit(0);
      int state = i + 1;
      if (dfa.finalStates().get(representative))
        minimized.addFinalState(state, dfa.action(representative));
      for (int alphabet = 0; alphabet < dfa.alphabetSize(); alphabet++)
        minimized.addTransition(state, alphabet, blocks[dfa.transition(representative, alphabet)]);
    }
    minimized.setStartState(blocks[dfa.startState()]);
    Out.info(
        "DFA Minimized. Num States: %d, Num Final States: %d",
        minimized.statesCount(), minimized.finalStatesCount());
    return minimized;
  }

  private int[] blocks(List<BitSet> partitions) {
    int[] blocks = new int[dfa.statesCount() + 1];
    // Zero remains the reserved dead state, distinct from every real state.
    for (int i = 0; i < partitions.size(); i++)
      for (int state : partitions.get(i).stream().toArray()) blocks[state] = i + 1;
    return blocks;
  }
}
