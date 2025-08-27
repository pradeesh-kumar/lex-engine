/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ParserHelperTest {

  private Path tempFile;

  @BeforeEach
  void setup() throws IOException {
    tempFile = Files.createTempFile("grammar", ".txt");
  }

  @Test
  void testEliminateLeftRecursion_expression() throws IOException {
    // Define the input grammar as a string
    String content = """
      ---
      EXPR -> EXPR + TERM | TERM
      TERM -> TERM * FACTOR | FACTOR
      FACTOR -> ident | num | (EXPR)
      """;

    /**
     * Expected transformation:
     *
     * <p>
     *   EXPR -> TERM EXPR'
     *   EXPR' -> + TERM EXPR' | ε
     *   TERM -> FACTOR TERM'
     *   TERM' -> * FACTOR TERM'| ε
     *   FACTOR -> ident | num | (EXPR)
     */

    // Parse the grammar spec into a Grammar object
    Files.writeString(tempFile, content);

    GrammarSpecParser parser = new GrammarSpecParser(tempFile.toFile());

    // Eliminate left recursion
    Grammar grammar = parser.parse().grammar();
    Grammar newGrammar = ParserHelper.eliminateLeftRecursion(grammar);

    // Verify the resulting grammar
    assertNotNull(newGrammar);

    // Check that left recursion has been eliminated
    assertTrue(newGrammar.productions().containsNonTerminal(Grammar.NonTerminal.of("EXPR")));
    assertTrue(newGrammar.productions().containsNonTerminal(Grammar.NonTerminal.of("EXPR'")));
    assertTrue(newGrammar.productions().containsNonTerminal(Grammar.NonTerminal.of("TERM")));
    assertTrue(newGrammar.productions().containsNonTerminal(Grammar.NonTerminal.of("TERM'")));

    // Check the new productions for EXPR and EXPR'
    Grammar.ProductionRule exprProductions =
        newGrammar.productions().get(Grammar.NonTerminal.of("EXPR"));
    assertEquals(1, exprProductions.alternatives().size());
    assertEquals(2, exprProductions.alternatives().getFirst().size());
    assertEquals("TERM", exprProductions.alternatives().getFirst().first().name());
    assertEquals("EXPR'", exprProductions.alternatives().getFirst().get(1).name());

    Grammar.ProductionRule exprPrimeProductions =
        newGrammar.productions().get(Grammar.NonTerminal.of("EXPR'"));
    assertEquals(2, exprPrimeProductions.alternatives().size());
    assertEquals(
        exprPrimeProductions.alternatives().getFirst(),
        Grammar.Alternative.create(List.of(
            Grammar.Terminal.of("+"),
            Grammar.NonTerminal.of("TERM"),
            Grammar.NonTerminal.of("EXPR'"))));
    assertEquals(exprPrimeProductions.alternatives().get(1), Grammar.Alternative.create()); // epsilon production

    // Check the new productions for TERM and TERM'
    Grammar.ProductionRule termProductions =
        newGrammar.productions().get(Grammar.NonTerminal.of("TERM"));
    assertEquals(1, termProductions.alternatives().size());
    assertEquals(2, termProductions.alternatives().getFirst().size());
    assertTrue(
        termProductions.alternatives().contains(
            Grammar.Alternative.create(List.of(Grammar.NonTerminal.of("FACTOR"), Grammar.NonTerminal.of("TERM'")))));

    Grammar.ProductionRule termPrimeProductions =
        newGrammar.productions().get(Grammar.NonTerminal.of("TERM'"));
    assertEquals(2, termPrimeProductions.alternatives().size());
    assertEquals(
        termPrimeProductions.alternatives().getFirst(),
        Grammar.Alternative.create(List.of(
            Grammar.Terminal.of("*"),
            Grammar.NonTerminal.of("FACTOR"),
            Grammar.NonTerminal.of("TERM'"))));
    assertTrue(termPrimeProductions.alternatives().get(1).isEmpty()); // epsilon production

    // Check that FACTOR productions remain unchanged
    assertEquals(
        grammar.productions().get(Grammar.NonTerminal.of("FACTOR")),
        newGrammar.productions().get(Grammar.NonTerminal.of("FACTOR")));
  }

  @Test
  void testEliminateLeftRecursion() {
    // Create a grammar with left recursion
    Grammar.NonTerminal A = Grammar.NonTerminal.of("A");
    Grammar.Terminal a = Grammar.Terminal.of("a");
    Grammar.ProductionMap productions = new Grammar.ProductionMap();
    productions.add(Grammar.ProductionRule.create(A, List.of(Grammar.Alternative.create(List.of(A, a)), Grammar.Alternative.create(List.of(a)))));
    Grammar grammar = new Grammar(productions, A);

    // Eliminate left recursion
    Grammar newGrammar = ParserHelper.eliminateLeftRecursion(grammar);

    // Verify the resulting grammar
    assertNotNull(newGrammar);
    assertTrue(newGrammar.productions().containsNonTerminal(A));
    assertTrue(newGrammar.productions().containsNonTerminal(Grammar.NonTerminal.of("A'")));
  }

  @Test
  void testNoLeftRecursion() {
    // Create a grammar without left recursion
    Grammar.NonTerminal A = Grammar.NonTerminal.of("A");
    Grammar.Terminal a = Grammar.Terminal.of("a");
    Grammar.ProductionMap productions = new Grammar.ProductionMap();
    productions.add(Grammar.ProductionRule.create(A, List.of(Grammar.Alternative.create(List.of(a)))));
    Grammar grammar = new Grammar(productions, A);

    // Eliminate left recursion
    Grammar newGrammar = ParserHelper.eliminateLeftRecursion(grammar);

    // Verify the resulting grammar
    assertNotNull(newGrammar);
    assertEquals(grammar.productions().size(), newGrammar.productions().size());
  }
}
