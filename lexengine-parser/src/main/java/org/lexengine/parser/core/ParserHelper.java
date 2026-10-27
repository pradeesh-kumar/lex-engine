/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import java.util.*;
import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;

/** Grammar transformations used by the predictive generators. */
public final class ParserHelper {
  private ParserHelper() {}

  /**
   * Eliminates ordered indirect recursion and direct left recursion without modifying the input.
   */
  public static Grammar eliminateLeftRecursion(Grammar grammar) {
    Map<Grammar.NonTerminal, List<Grammar.Alternative>> result = new LinkedHashMap<>();
    List<Grammar.NonTerminal> order = new ArrayList<>(grammar.productions().nonTerminals());
    Set<Grammar.NonTerminal> names = new HashSet<>(order);
    for (int i = 0; i < order.size(); i++) {
      Grammar.NonTerminal lhs = order.get(i);
      List<Grammar.Alternative> alternatives =
          new ArrayList<>(grammar.productions().get(lhs).alternatives());
      for (int j = 0; j < i; j++) {
        Grammar.NonTerminal previous = order.get(j);
        List<Grammar.Alternative> expanded = new ArrayList<>();
        for (Grammar.Alternative alternative : alternatives) {
          if (!alternative.isEmpty() && alternative.first().equals(previous)) {
            for (Grammar.Alternative prefix : result.get(previous)) {
              List<Grammar.Symbol> symbols = new ArrayList<>(prefix.symbols());
              symbols.addAll(alternative.symbols().subList(1, alternative.size()));
              expanded.add(Grammar.Alternative.create(symbols, alternative.label()));
            }
          } else expanded.add(alternative);
        }
        alternatives = expanded;
      }
      List<Grammar.Alternative> alpha = new ArrayList<>();
      List<Grammar.Alternative> beta = new ArrayList<>();
      for (Grammar.Alternative alternative : alternatives) {
        if (!alternative.isEmpty() && alternative.first().equals(lhs)) {
          // A -> A contributes no strings and can be discarded.
          if (alternative.size() > 1)
            alpha.add(
                Grammar.Alternative.create(
                    alternative.symbols().subList(1, alternative.size()), alternative.label()));
        } else beta.add(alternative);
      }
      if (beta.isEmpty())
        throw GeneratorException.create(
            ErrorType.ERR_GRAMMAR_PRODUCTION_INVALID,
            "Nonterminal %s has no terminating alternative",
            lhs);
      if (alpha.isEmpty()) {
        result.put(lhs, beta);
        continue;
      }
      String name = lhs.name() + "'";
      while (names.contains(Grammar.NonTerminal.of(name))) name += "'";
      Grammar.NonTerminal prime = Grammar.NonTerminal.of(name);
      names.add(prime);
      result.put(lhs, append(beta, prime));
      List<Grammar.Alternative> tails = append(alpha, prime);
      tails.add(Grammar.Alternative.create());
      result.put(prime, tails);
    }
    Grammar.ProductionMap productions = new Grammar.ProductionMap();
    result.forEach(
        (lhs, alternatives) -> {
          Grammar.ProductionRule original = grammar.productions().get(lhs);
          productions.add(
              original != null && original.alternatives().equals(alternatives)
                  ? original
                  : Grammar.ProductionRule.create(lhs, alternatives));
        });
    return new Grammar(productions, grammar.startSymbol());
  }

  private static List<Grammar.Alternative> append(
      List<Grammar.Alternative> alternatives, Grammar.NonTerminal tail) {
    List<Grammar.Alternative> result = new ArrayList<>();
    for (Grammar.Alternative alternative : alternatives) {
      List<Grammar.Symbol> symbols = new ArrayList<>(alternative.symbols());
      symbols.add(tail);
      result.add(Grammar.Alternative.create(symbols, alternative.label()));
    }
    return result;
  }
}
