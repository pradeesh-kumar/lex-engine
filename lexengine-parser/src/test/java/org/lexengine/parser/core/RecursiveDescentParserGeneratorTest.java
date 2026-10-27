/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RecursiveDescentParserGeneratorTest {
  @TempDir Path directory;

  @ParameterizedTest
  @EnumSource(ParserGenerator.ParserType.class)
  void generatedParserCompilesAndParsesLeftRecursiveExpressions(ParserGenerator.ParserType type)
      throws Exception {
    String grammar = Files.readString(Path.of(getClass().getResource("/program.grammar").toURI()));
    try (GeneratedParser parser = GeneratedParser.compile(directory, grammar, type, false)) {
      String[] input = {"num", "+", "ident", "*", "(", "num", "-", "num", ")"};
      Object tree = parser.parseTokens(input);
      assertEquals("EXPR", GeneratedParser.property(GeneratedParser.root(tree), "symbol"));
      assertEquals(List.of(input), GeneratedParser.leaves(tree));
      assertThrows(RuntimeException.class, () -> parser.parseTokens("num", "+", "*", "num"));
      assertThrows(RuntimeException.class, () -> parser.parseTokens("num", "num"));
      assertThrows(RuntimeException.class, () -> parser.parseTokens("unknown"));
      assertThrows(RuntimeException.class, parser::parseTokens);
      assertTrue(parser.generator.details().productionCount() > 0);
    }
  }
}
