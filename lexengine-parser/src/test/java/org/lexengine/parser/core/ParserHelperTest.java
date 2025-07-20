package org.lexengine.parser.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
     * EXPR -> TERM EXPR'
     * EXPR' -> + TERM EXPR' | ε
     * TERM -> FACTOR TERM'
     * TERM' -> * FACTOR TERM' | ε
     * FACTOR -> ident | num | (EXPR)
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
    assertTrue(newGrammar.productions().containsKey(Grammar.NonTerminal.of("EXPR")));
    assertTrue(newGrammar.productions().containsKey(Grammar.NonTerminal.of("EXPR'")));
    assertTrue(newGrammar.productions().containsKey(Grammar.NonTerminal.of("TERM")));
    assertTrue(newGrammar.productions().containsKey(Grammar.NonTerminal.of("TERM'")));

    // Check the new productions for EXPR and EXPR'
    List<List<Grammar.Symbol>> exprProductions = newGrammar.productions().get(Grammar.NonTerminal.of("EXPR"));
    assertEquals(1, exprProductions.size());
    assertEquals(2, exprProductions.getFirst().size());
    assertEquals("TERM", exprProductions.getFirst().getFirst().name());
    assertEquals("EXPR'", exprProductions.getFirst().get(1).name());

    List<List<Grammar.Symbol>> exprPrimeProductions = newGrammar.productions().get(Grammar.NonTerminal.of("EXPR'"));
    assertEquals(2, exprPrimeProductions.size());
    assertEquals(exprPrimeProductions.getFirst(), List.of(Grammar.Terminal.of("+"), Grammar.NonTerminal.of("TERM"), Grammar.NonTerminal.of("EXPR'")));
    assertEquals(exprPrimeProductions.get(1), List.of()); // epsilon production

    // Check the new productions for TERM and TERM'
    List<List<Grammar.Symbol>> termProductions = newGrammar.productions().get(Grammar.NonTerminal.of("TERM"));
    assertEquals(1, termProductions.size());
    assertEquals(2, termProductions.getFirst().size());
    assertTrue(termProductions.contains(List.of(Grammar.NonTerminal.of("FACTOR"), Grammar.NonTerminal.of("TERM'"))));

    List<List<Grammar.Symbol>> termPrimeProductions = newGrammar.productions().get(Grammar.NonTerminal.of("TERM'"));
    assertEquals(2, termPrimeProductions.size());
    assertEquals(termPrimeProductions.getFirst(), List.of(Grammar.Terminal.of("*"), Grammar.NonTerminal.of("FACTOR"), Grammar.NonTerminal.of("TERM'")));
    assertTrue(termPrimeProductions.get(1).isEmpty()); // epsilon production

    // Check that FACTOR productions remain unchanged
    assertEquals(grammar.productions().get(Grammar.NonTerminal.of("FACTOR")), newGrammar.productions().get(Grammar.NonTerminal.of("FACTOR")));
  }

  @Test
  void testEliminateLeftRecursion() {
    // Create a grammar with left recursion
    Grammar.NonTerminal A = Grammar.NonTerminal.of("A");
    Grammar.Terminal a = Grammar.Terminal.of("a");
    Grammar grammar = new Grammar(Map.of(
      A, List.of(
        List.of(A, a),
        List.of(a)
      )
    ));

    // Eliminate left recursion
    Grammar newGrammar = ParserHelper.eliminateLeftRecursion(grammar);

    // Verify the resulting grammar
    assertNotNull(newGrammar);
    assertTrue(newGrammar.productions().containsKey(A));
    assertTrue(newGrammar.productions().containsKey(Grammar.NonTerminal.of("A'")));
  }

  @Test
  void testNoLeftRecursion() {
    // Create a grammar without left recursion
    Grammar.NonTerminal A = Grammar.NonTerminal.of("A");
    Grammar.Terminal a = Grammar.Terminal.of("a");
    Grammar grammar = new Grammar(Map.of(
      A, List.of(
        List.of(a)
      )
    ));

    // Eliminate left recursion
    Grammar newGrammar = ParserHelper.eliminateLeftRecursion(grammar);

    // Verify the resulting grammar
    assertNotNull(newGrammar);
    assertEquals(grammar, newGrammar);
  }
}
