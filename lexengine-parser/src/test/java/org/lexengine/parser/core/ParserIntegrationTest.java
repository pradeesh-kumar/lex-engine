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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.lexengine.commons.error.GeneratorException;

class ParserIntegrationTest {
  @TempDir Path directory;
  static final String COMBINED =
      """
      package generated;
      public class ArithmeticParser {
        public String custom() { return "#custom$"; }
      }
      ---
      ident: "[a-zA-Z_][a-zA-Z0-9_]*"
      num: "[0-9]+"
      Operator: { plus: "\\+", minus: "-", times: "\\*", divide: "/", open: "\\(", close: "\\)" }
      TOKEN_IGNORE: { "[ \\t\\r\\n]+" }
      ---
      EXPR -> TERM (('+' | '-') TERM)* $Expression;
      TERM -> FACTOR (('*' | '/') FACTOR)* $Term;
      FACTOR -> ident $Identifier | num $Number | '(' EXPR ')' $Grouped;
      """;

  @ParameterizedTest
  @EnumSource(ParserGenerator.ParserType.class)
  void combinedGrammarTokenizesAndBuildsLabeledTree(ParserGenerator.ParserType type)
      throws Exception {
    try (GeneratedParser parser = GeneratedParser.compile(directory, COMBINED, type, true)) {
      Object tree = parser.parseText("12 + 3 * (alpha-4)\n");
      assertEquals(
          List.of("12", "+", "3", "*", "(", "alpha", "-", "4", ")"), GeneratedParser.leaves(tree));
      assertEquals("Expression", GeneratedParser.property(GeneratedParser.root(tree), "label"));
      assertEquals(
          "#custom$",
          parser
              .parserClass
              .getMethod("custom")
              .invoke(parser.parserClass.getConstructor(String.class).newInstance("1")));
      assertThrows(RuntimeException.class, () -> parser.parseText("1 + )"));
      assertThrows(RuntimeException.class, () -> parser.parseText("1 @ 2"));
      assertThrows(RuntimeException.class, () -> parser.parseText("1 2"));
      assertThrows(RuntimeException.class, () -> parser.parseText(""));
    }
  }

  @ParameterizedTest
  @EnumSource(ParserGenerator.ParserType.class)
  void nullableSequencesAndEndOfInput(ParserGenerator.ParserType type) throws Exception {
    String grammar =
        "class=NullableParser\npackage=generated\n---\nS -> A B\nA -> a | ε\nB -> b | ε\n";
    try (GeneratedParser parser = GeneratedParser.compile(directory, grammar, type, false)) {
      for (String[] input :
          List.of(new String[] {}, new String[] {"a"}, new String[] {"b"}, new String[] {"a", "b"}))
        assertEquals(List.of(input), GeneratedParser.leaves(parser.parseTokens(input)));
      assertThrows(RuntimeException.class, () -> parser.parseTokens("b", "a"));
    }
  }

  @ParameterizedTest
  @EnumSource(ParserGenerator.ParserType.class)
  void optionalAndOneOrMoreTerms(ParserGenerator.ParserType type) throws Exception {
    String grammar =
        """
        public class SequenceParser {}
        ---
        a: "a"
        b: "b"
        ---
        ROOT -> 'a'? 'b'+ $Sequence;
        """;
    try (GeneratedParser parser = GeneratedParser.compile(directory, grammar, type, true)) {
      assertEquals(List.of("a", "b", "b"), GeneratedParser.leaves(parser.parseText("abb")));
      assertEquals(List.of("b"), GeneratedParser.leaves(parser.parseText("b")));
      assertThrows(RuntimeException.class, () -> parser.parseText("a"));
      assertThrows(RuntimeException.class, () -> parser.parseText(""));
    }
  }

  @ParameterizedTest
  @EnumSource(ParserGenerator.ParserType.class)
  void epsilonOnlyGrammar(ParserGenerator.ParserType type) throws Exception {
    try (GeneratedParser parser =
        GeneratedParser.compile(directory, "---\nS -> ε\n", type, false)) {
      assertEquals(List.of(), GeneratedParser.leaves(parser.parseTokens()));
      assertEquals(
          List.of(),
          GeneratedParser.property(GeneratedParser.root(parser.parseTokens()), "children"));
      assertThrows(RuntimeException.class, () -> parser.parseTokens("a"));
    }
  }

  @Test
  void canonicalLr1DistinguishesLookaheadsThatLalrWouldMerge() throws Exception {
    String grammar = "---\nS -> a A d | b A e | a B e | b B d\nA -> c\nB -> c\n";
    try (GeneratedParser parser =
        GeneratedParser.compile(directory, grammar, ParserGenerator.ParserType.LR1, false)) {
      for (String[] input :
          List.of(
              new String[] {"a", "c", "d"},
              new String[] {"b", "c", "e"},
              new String[] {"a", "c", "e"},
              new String[] {"b", "c", "d"}))
        assertEquals(List.of(input), GeneratedParser.leaves(parser.parseTokens(input)));
      assertThrows(RuntimeException.class, () -> parser.parseTokens("a", "c", "c"));
      assertTrue(parser.generator.details().stateCount() > 0);
    }
  }

  @ParameterizedTest
  @EnumSource(
      value = ParserGenerator.ParserType.class,
      names = {"LL1", "RECURSIVE_DESCENT"})
  void reportsPredictiveConflicts(ParserGenerator.ParserType type) throws Exception {
    Path file = directory.resolve("conflict.grammar");
    Files.writeString(file, "---\nS -> a A | a B\nA -> b\nB -> c\n");
    var generator = ParserGenerator.Factory.create(file.toFile(), type, directory, null);
    assertTrue(
        assertThrows(GeneratorException.class, generator::generate)
            .getMessage()
            .contains("LL(1) conflict"));
  }

  @Test
  void reportsLrShiftReduceAndReduceReduceConflicts() throws Exception {
    for (String grammar : List.of("---\nS -> S S | a\n", "---\nS -> A | B\nA -> a\nB -> a\n")) {
      Path file = directory.resolve("ambiguous.grammar");
      Files.writeString(file, grammar);
      var generator =
          ParserGenerator.Factory.create(
              file.toFile(), ParserGenerator.ParserType.LR1, directory, null);
      assertTrue(
          assertThrows(GeneratorException.class, generator::generate)
              .getMessage()
              .contains("conflict"));
    }
  }
}
