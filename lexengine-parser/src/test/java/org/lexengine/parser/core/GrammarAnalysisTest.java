/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lexengine.commons.error.GeneratorException;

class GrammarAnalysisTest {
  @TempDir Path directory;

  private Grammar grammar(String rules) throws Exception {
    Path file = directory.resolve("input.grammar");
    Files.writeString(file, "---\n" + rules);
    return new GrammarSpecParser(file.toFile()).parse().grammar();
  }

  @Test
  void firstAndFollowConvergeAcrossNullableCycles() throws Exception {
    Grammar grammar = grammar("S -> A c\nA -> B | ε\nB -> A b | a\n");
    assertEquals(
        Set.of(Grammar.Terminal.of("a"), Grammar.Terminal.of("b"), Grammar.Terminal.of("c")),
        grammar.productions().getFirstSet(Grammar.NonTerminal.of("S")));
    assertEquals(
        Set.of(Grammar.Terminal.of("b"), Grammar.Terminal.of("c")),
        grammar.productions().getFollowSet(Grammar.NonTerminal.of("A")));
    assertEquals(
        Set.of(Grammar.Terminal.endOfInput()),
        grammar.productions().getFollowSet(Grammar.NonTerminal.of("S")));
    assertThrows(
        UnsupportedOperationException.class,
        () -> grammar.productions().getFirstSet(Grammar.NonTerminal.of("S")).clear());
  }

  @Test
  void followsConsiderEveryOccurrenceAndNullableSuffixes() throws Exception {
    Grammar grammar = grammar("S -> A A B\nA -> a | ε\nB -> b | ε\n");
    assertEquals(
        Set.of(Grammar.Terminal.of("a"), Grammar.Terminal.of("b"), Grammar.Terminal.endOfInput()),
        grammar.productions().getFollowSet(Grammar.NonTerminal.of("A")));
  }

  @Test
  void eliminatesIndirectRecursionAndKeepsLabels() throws Exception {
    Grammar original = grammar("A -> B a | c\nB -> A b | d\n");
    Grammar transformed = ParserHelper.eliminateLeftRecursion(original);
    assertTrue(transformed.productions().containsNonTerminal(Grammar.NonTerminal.of("B'")));
    for (var rule : transformed.productions().rules())
      for (var alternative : rule.alternatives())
        assertFalse(!alternative.isEmpty() && alternative.first().equals(rule.lhs()));
    assertEquals(2, original.productions().size());
  }

  @Test
  void rejectsNonTerminatingRecursion() throws Exception {
    Grammar grammar = grammar("A -> A a\n");
    assertThrows(GeneratorException.class, () -> ParserHelper.eliminateLeftRecursion(grammar));
  }

  @Test
  void copiedAlternativesCannotBeMutated() {
    List<Grammar.Symbol> symbols = new ArrayList<>(List.of(Grammar.Terminal.of("a")));
    Grammar.Alternative alternative = Grammar.Alternative.create(symbols);
    symbols.clear();
    assertEquals(1, alternative.size());
    assertThrows(UnsupportedOperationException.class, () -> alternative.symbols().clear());
  }
}
