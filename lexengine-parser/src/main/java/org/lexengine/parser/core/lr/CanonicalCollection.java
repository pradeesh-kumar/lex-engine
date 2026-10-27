/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core.lr;

import java.util.*;
import org.lexengine.parser.core.Grammar;

/** Canonical LR(1) item sets and transitions, including per-item lookaheads. */
public final class CanonicalCollection {
  public record Production(Grammar.NonTerminal lhs, Grammar.Alternative alternative) {}

  public record Item(int production, int dot, Grammar.Terminal lookahead) {}

  public record Transition(int state, Grammar.Symbol symbol) {}

  private final Grammar grammar;
  private final List<Production> productions = new ArrayList<>();
  private final Map<Grammar.NonTerminal, List<Integer>> byLhs = new LinkedHashMap<>();
  private final List<Set<Item>> states = new ArrayList<>();
  private final Map<Transition, Integer> transitions = new LinkedHashMap<>();

  public CanonicalCollection(Grammar grammar) {
    this.grammar = grammar;
    String name = grammar.startSymbol().name() + "'";
    while (grammar.productions().containsNonTerminal(Grammar.NonTerminal.of(name))) name += "'";
    productions.add(
        new Production(
            Grammar.NonTerminal.of(name),
            Grammar.Alternative.create(List.of(grammar.startSymbol()))));
    for (Grammar.ProductionRule rule : grammar.productions().rules()) {
      for (Grammar.Alternative alternative : rule.alternatives()) {
        byLhs.computeIfAbsent(rule.lhs(), ignored -> new ArrayList<>()).add(productions.size());
        productions.add(new Production(rule.lhs(), alternative));
      }
    }
    build();
  }

  public List<Production> productions() {
    return List.copyOf(productions);
  }

  public List<Set<Item>> states() {
    return Collections.unmodifiableList(states);
  }

  public Map<Transition, Integer> transitions() {
    return Collections.unmodifiableMap(transitions);
  }

  public Set<Item> closure(Collection<Item> seed) {
    Set<Item> result = new LinkedHashSet<>(seed);
    Deque<Item> queue = new ArrayDeque<>(seed);
    while (!queue.isEmpty()) {
      Item item = queue.removeFirst();
      List<Grammar.Symbol> rhs = productions.get(item.production()).alternative().symbols();
      if (item.dot() == rhs.size() || !(rhs.get(item.dot()) instanceof Grammar.NonTerminal nt))
        continue;
      List<Grammar.Symbol> suffix = new ArrayList<>(rhs.subList(item.dot() + 1, rhs.size()));
      suffix.add(item.lookahead());
      List<Grammar.Terminal> first =
          grammar.productions().firstOf(suffix).stream()
              .filter(t -> !t.isEpsilon())
              .sorted(Comparator.comparing(Grammar.Symbol::name))
              .toList();
      for (int production : byLhs.get(nt)) {
        for (Grammar.Terminal lookahead : first) {
          Item added = new Item(production, 0, lookahead);
          if (result.add(added)) queue.addLast(added);
        }
      }
    }
    return Collections.unmodifiableSet(result);
  }

  private void build() {
    Set<Item> initial = closure(List.of(new Item(0, 0, Grammar.Terminal.endOfInput())));
    states.add(initial);
    Map<Set<Item>, Integer> indices = new HashMap<>();
    indices.put(initial, 0);
    for (int state = 0; state < states.size(); state++) {
      Map<Grammar.Symbol, Set<Item>> shifted = new LinkedHashMap<>();
      for (Item item : states.get(state)) {
        List<Grammar.Symbol> rhs = productions.get(item.production()).alternative().symbols();
        if (item.dot() < rhs.size())
          shifted
              .computeIfAbsent(rhs.get(item.dot()), ignored -> new LinkedHashSet<>())
              .add(new Item(item.production(), item.dot() + 1, item.lookahead()));
      }
      for (var entry : shifted.entrySet()) {
        Set<Item> next = closure(entry.getValue());
        Integer index = indices.get(next);
        if (index == null) {
          index = states.size();
          indices.put(next, index);
          states.add(next);
        }
        transitions.put(new Transition(state, entry.getKey()), index);
      }
    }
  }
}
