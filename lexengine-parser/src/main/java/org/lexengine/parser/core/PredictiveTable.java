/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import java.util.*;
import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;

/** A conflict-checked LL(1) table shared by recursive descent and the stack-driven parser. */
public final class PredictiveTable {
  public record Rule(Grammar.NonTerminal lhs, Grammar.Alternative alternative) {}

  public record Key(Grammar.NonTerminal nonTerminal, Grammar.Terminal terminal) {}

  private final List<Rule> rules = new ArrayList<>();
  private final Map<Key, Integer> entries = new LinkedHashMap<>();

  public PredictiveTable(Grammar grammar) {
    for (Grammar.ProductionRule production : grammar.productions().rules()) {
      if (grammar.productions().getFirstSet(production.lhs()).isEmpty())
        throw GeneratorException.create(
            ErrorType.ERR_GRAMMAR_PRODUCTION_INVALID,
            "Nonterminal %s cannot derive terminal input",
            production.lhs());
      for (Grammar.Alternative alternative : production.alternatives()) {
        int index = rules.size();
        rules.add(new Rule(production.lhs(), alternative));
        Set<Grammar.Terminal> lookaheads =
            new LinkedHashSet<>(grammar.productions().firstOf(alternative.symbols()));
        if (lookaheads.remove(Grammar.Terminal.createEpsilon()))
          lookaheads.addAll(grammar.productions().getFollowSet(production.lhs()));
        for (Grammar.Terminal lookahead : lookaheads) {
          Integer old = entries.putIfAbsent(new Key(production.lhs(), lookahead), index);
          if (old != null)
            throw GeneratorException.create(
                ErrorType.ERR_GRAMMAR_INVALID,
                "LL(1) conflict for %s on %s: alternatives %s and %s",
                production.lhs(),
                lookahead,
                rules.get(old).alternative().symbols(),
                alternative.symbols());
        }
      }
    }
  }

  public List<Rule> rules() {
    return List.copyOf(rules);
  }

  public Map<Key, Integer> entries() {
    return Collections.unmodifiableMap(entries);
  }
}
