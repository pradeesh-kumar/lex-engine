package org.lexengine.parser.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Provides utility methods for parsing.
 */
public final class ParserHelper {

  private ParserHelper() {}

  /**
   * Eliminates left recursion from a given grammar.
   *
   * Left recursion occurs when a non-terminal symbol can be rewritten to itself, either directly or indirectly.
   * This method transforms the grammar to eliminate such left recursion, making it suitable for top-down parsing.
   *
   * @param grammar the input grammar to eliminate left recursion from
   * @return a new grammar with left recursion eliminated
   */
  static Grammar eliminateLeftRecursion(Grammar grammar) {
    Map<Grammar.NonTerminal, List<List<Grammar.Symbol>>> newProductions = new HashMap<>();

    for (var entry : grammar.productions().entrySet()) {
      var nonTerminal = entry.getKey();
      var productions = entry.getValue();

      // Check if the non-terminal has any left recursive productions
      if (hasLeftRecursion(nonTerminal, productions)) {
        var updatedProductions = eliminateLeftRecursion(nonTerminal, productions);
        newProductions.putAll(updatedProductions);
      } else {
        newProductions.put(nonTerminal, productions);
      }
    }
    return new Grammar(newProductions);
  }

  /**
   * Checks if a non-terminal has any left recursive productions.
   *
   * @param nonTerminal the non-terminal to check
   * @param productions the productions associated with the non-terminal
   * @return true if the non-terminal has any left recursive productions, false otherwise
   */
  private static boolean hasLeftRecursion(Grammar.NonTerminal nonTerminal, List<List<Grammar.Symbol>> productions) {
    return productions.stream().filter(Predicate.not(List::isEmpty)).map(List::getFirst).anyMatch(p -> p.equals(nonTerminal));
  }

  /**
   * Eliminates left recursion from a specific non-terminal and its associated productions.
   *
   * This method is used internally by {@link #eliminateLeftRecursion(Grammar)} to eliminate left recursion from a grammar.
   *
   * @param nonTerminal the non-terminal to eliminate left recursion from
   * @param productions the productions associated with the non-terminal
   * @return a map of updated productions for the non-terminal and any new non-terminals introduced
   */
  private static Map<Grammar.NonTerminal, List<List<Grammar.Symbol>>> eliminateLeftRecursion(Grammar.NonTerminal nonTerminal, List<List<Grammar.Symbol>> productions) {
    var newNonTerminal = Grammar.NonTerminal.of(nonTerminal.name() + "'");
    List<List<Grammar.Symbol>> alphaProductions = new ArrayList<>();
    List<List<Grammar.Symbol>> betaProductions = new ArrayList<>();

    for (var production : productions) {
      if (!production.isEmpty() && production.getFirst().equals(nonTerminal)) {
        // Left recursive production
        List<Grammar.Symbol> alpha = new ArrayList<>(production.subList(1, production.size()));
        alpha.add(newNonTerminal);
        alphaProductions.add(alpha);
      } else {
        // Non-left recursive production
        List<Grammar.Symbol> beta = new ArrayList<>(production);
        beta.add(newNonTerminal);
        betaProductions.add(beta);
      }
    }

    // Add epsilon production for the new non-terminal
    alphaProductions.add(List.of());

    // Update productions for the grammar
    Map<Grammar.NonTerminal, List<List<Grammar.Symbol>>> updatedProductions = new HashMap<>();
    updatedProductions.put(nonTerminal, betaProductions);
    updatedProductions.put(newNonTerminal, alphaProductions);
    return updatedProductions;
  }
}
