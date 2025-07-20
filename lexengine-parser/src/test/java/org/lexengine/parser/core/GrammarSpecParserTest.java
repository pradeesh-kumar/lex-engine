package org.lexengine.parser.core;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lexengine.parser.error.ParserGeneratorException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GrammarSpecParserTest {

  private Path tempFile;

  @BeforeEach
  void setup() throws IOException {
    tempFile = Files.createTempFile("grammar", ".txt");
  }

  @Test
  void testParseValidGrammarFile() throws IOException {
    String content = """
                class=Parser
                package=org.lexengine.parser.generated
                ---
                EXPR -> EXPR + TERM | TERM
                TERM -> TERM * FACTOR | FACTOR
                FACTOR -> ident | num | (EXPR)
                """;
    Files.writeString(tempFile, content);

    // Act
    GrammarSpecParser parser = new GrammarSpecParser(tempFile.toFile());
    GrammarSpec spec = parser.parse();

    // Assert
    assertNotNull(spec);
    assertEquals("Parser", spec.parserClassName());
    assertEquals("org.lexengine.parser.generated", spec.parserPackageName());
    assertNotNull(spec.grammar());

    Grammar grammar = spec.grammar();
    assertEquals(3, grammar.productions().size());

    Grammar.NonTerminal expr = Grammar.NonTerminal.of("EXPR");
    List<List<Grammar.Symbol>> exprProductions = grammar.productions().get(expr);
    assertEquals(2, exprProductions.size());
    assertEquals(List.of(Grammar.Symbol.parse("EXPR"), Grammar.Symbol.parse("+"), Grammar.Symbol.parse("TERM")), exprProductions.get(0));
    assertEquals(List.of(Grammar.Symbol.parse("TERM")), exprProductions.get(1));

    Grammar.NonTerminal term = Grammar.NonTerminal.of("TERM");
    List<List<Grammar.Symbol>> termProductions = grammar.productions().get(term);
    assertEquals(2, termProductions.size());
    assertEquals(List.of(Grammar.Symbol.parse("TERM"), Grammar.Symbol.parse("*"), Grammar.Symbol.parse("FACTOR")), termProductions.get(0));
    assertEquals(List.of(Grammar.Symbol.parse("FACTOR")), termProductions.get(1));

    Grammar.NonTerminal factor = Grammar.NonTerminal.of("FACTOR");
    List<List<Grammar.Symbol>> factorProductions = grammar.productions().get(factor);
    assertEquals(3, factorProductions.size());
    assertEquals(List.of(Grammar.Symbol.parse("ident")), factorProductions.get(0));
    assertEquals(List.of(Grammar.Symbol.parse("num")), factorProductions.get(1));
    assertEquals(List.of(Grammar.Symbol.parse("("), Grammar.Symbol.parse("EXPR"), Grammar.Symbol.parse(")")), factorProductions.get(2));
  }

  @Test
  void testParseInvalidGrammarFile_MissingDivider() throws IOException {
    String content = """
                class=Parser
                package=org.lexengine.parser.generated
                EXPR -> EXPR + TERM | TERM
                TERM -> TERM * FACTOR | FACTOR
                FACTOR -> ident | num | (EXPR)
                """;
    Files.writeString(tempFile, content);

    GrammarSpecParser parser = new GrammarSpecParser(tempFile.toFile());
    assertThrows(ParserGeneratorException.class, parser::parse);
  }

  @Test
  void testParseInvalidGrammarFile_InvalidProperty() throws IOException {
    String content = """
                invalidProperty=Parser
                package=org.lexengine.parser.generated
                ---
                EXPR -> EXPR + TERM | TERM
                TERM -> TERM * FACTOR | FACTOR
                FACTOR -> ident | num | (EXPR)
                """;
    Files.writeString(tempFile, content);

    GrammarSpecParser parser = new GrammarSpecParser(tempFile.toFile());
    assertThrows(ParserGeneratorException.class, parser::parse);
  }

  @Test
  void testParseInvalidGrammarFile_InvalidProductionRule() throws IOException {
    String content = """
                class=Parser
                package=org.lexengine.parser.generated
                ---
                invalidRule
                """;
    Files.writeString(tempFile, content);

    GrammarSpecParser parser = new GrammarSpecParser(tempFile.toFile());
    assertThrows(ParserGeneratorException.class, parser::parse);
  }

  @Test
  void testParseNonExistentFile() {
    File nonExistentFile = new File("non_existent_file.txt");

    GrammarSpecParser parser = new GrammarSpecParser(nonExistentFile);
    assertThrows(ParserGeneratorException.class, parser::parse);
  }

}
