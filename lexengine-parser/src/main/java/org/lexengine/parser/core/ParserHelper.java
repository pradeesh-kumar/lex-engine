/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import org.lexengine.commons.logging.Out;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Provides utility methods for parsing. */
public final class ParserHelper {

  private ParserHelper() {}

  /**
   * Eliminates left recursion from a given grammar.
   *
   * <p>Left recursion occurs when a non-terminal symbol can be rewritten to itself, either directly
   * or indirectly. This method transforms the grammar to eliminate such left recursion, making it
   * suitable for top-down parsing.
   *
   * @param grammar the input grammar to eliminate left recursion from
   * @return a new grammar with left recursion eliminated
   */
  public static Grammar eliminateLeftRecursion(Grammar grammar) {
    Out.info("Eliminating left recursion from the Grammar");
    Grammar.ProductionMap newProductions = new Grammar.ProductionMap();

    for (var rule : grammar.productions().rules()) {
      var lhs = rule.lhs();
      var rhs = rule.alternatives();

      // Check if the non-terminal has any left recursive productions
      if (hasLeftRecursion(lhs, rhs)) {
        var updatedProductions = eliminateLeftRecursion(lhs, rhs);
        newProductions.addAll(updatedProductions);
      } else {
        newProductions.add(rule);
      }
    }
    Grammar convertedGrammar = new Grammar(newProductions, grammar.startSymbol());
    Out.debug("Grammar after eliminating left recursion: %s", convertedGrammar.toString());
    return convertedGrammar;
  }

  /**
   * Checks if a non-terminal has any left recursive productions.
   *
   * @param nonTerminal the non-terminal to check
   * @param alternatives the productions associated with the non-terminal
   * @return true if the non-terminal has any left recursive productions, false otherwise
   */
  private static boolean hasLeftRecursion(
      Grammar.NonTerminal nonTerminal, List<Grammar.Alternative> alternatives) {
    return alternatives.stream()
        .filter(Predicate.not(Grammar.Alternative::isEmpty))
        .map(Grammar.Alternative::first)
        .anyMatch(p -> p.equals(nonTerminal));
  }

  /**
   * Eliminates left recursion from a specific non-terminal and its associated productions.
   *
   * <p>This method is used internally by {@link #eliminateLeftRecursion(Grammar)} to eliminate left
   * recursion from a grammar.
   *
   * @param nonTerminal the non-terminal to eliminate left recursion from
   * @param productions the productions associated with the non-terminal
   * @return a List of updated productions for the non-terminal and any new non-terminals introduced
   */
  private static List<Grammar.ProductionRule> eliminateLeftRecursion(
      Grammar.NonTerminal nonTerminal, List<Grammar.Alternative> productions) {
    var newNonTerminal = Grammar.NonTerminal.of(nonTerminal.name() + "'");
    List<Grammar.Alternative> alphaProductions = new ArrayList<>();
    List<Grammar.Alternative> betaProductions = new ArrayList<>();

    for (var production : productions) {
      if (!production.isEmpty() && production.first().equals(nonTerminal)) {
        // Left recursive production
        List<Grammar.Symbol> alpha = new ArrayList<>(production.symbols().subList(1, production.size()));
        alpha.add(newNonTerminal);
        alphaProductions.add(Grammar.Alternative.create(alpha));
      } else {
        // Non-left recursive production
        List<Grammar.Symbol> beta = new ArrayList<>(production.symbols());
        beta.add(newNonTerminal);
        betaProductions.add(Grammar.Alternative.create(beta));
      }
    }

    // Add epsilon production for the new non-terminal
    alphaProductions.add(Grammar.Alternative.create(List.of()));

    // Update productions for the grammar
    List<Grammar.ProductionRule> updatedProductions = new ArrayList<>();
    updatedProductions.add(Grammar.ProductionRule.create(nonTerminal, betaProductions));
    updatedProductions.add(Grammar.ProductionRule.create(newNonTerminal, alphaProductions));
    return updatedProductions;
  }
}
