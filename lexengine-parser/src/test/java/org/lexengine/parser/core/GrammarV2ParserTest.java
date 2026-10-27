/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.parser.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.lexengine.commons.error.GeneratorException;

class GrammarV2ParserTest {
  private GrammarV2 parse(String tokens, String productions) {
    return new GrammarV2Parser(
            new StringReader("public class Example {}\n---\n" + tokens + "\n---\n" + productions))
        .parse();
  }

  @Test
  void preservesRegexEscapesHashesAndMetadataNewlines() {
    GrammarV2 grammar =
        parse(
            "hash: \"#\"\nnumber: \"\\d+\"\nTOKEN_IGNORE: \"[ \\t]+\"",
            "ROOT -> '#' number $Root;");
    assertEquals("#", grammar.tokenDefinition().tokenRules().getFirst().regex());
    assertEquals("\\d+", grammar.tokenDefinition().tokenRules().get(1).regex());
    assertTrue(grammar.metadata().compilableUnit().contains("\n"));
    assertEquals("Example", grammar.toGrammarSpec().parserClassName());
    assertEquals("", grammar.toGrammarSpec().parserPackageName());
  }

  @Test
  void supportsMultilineNestedGroupsLabelsAndRepeatedProductions() {
    GrammarV2 grammar =
        parse("a: \"a\"\nb: \"b\"", "ROOT -> ('a' | ('b' 'a')?)\n 'b'* $Root;\nROOT -> ε $Empty;");
    assertTrue(grammar.grammar().productions().size() > 1);
    assertEquals(
        2,
        grammar.grammar().productions().get(Grammar.NonTerminal.of("ROOT")).alternatives().size());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ROOT -> ('a';",
        "ROOT -> 'a'",
        "ROOT -> 'a' | ;",
        "ROOT -> ε 'a';",
        "ROOT -> 'a' $Label 'b';",
        "ROOT -> UNKNOWN;",
        "ROOT -> (ε)*;",
        "ROOT -> ('a' | ε)+;",
        "ROOT -> '';",
        "ROOT -> ;",
        "ROOT -> 'unterminated;"
      })
  void rejectsMalformedGrammarsPromptly(String production) {
    assertTimeoutPreemptively(
        Duration.ofSeconds(2),
        () -> assertThrows(GeneratorException.class, () -> parse("a: \"a\"", production)));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "a: \"a*\"",
        "a: \"[\"",
        "a: \"a\"\na: \"b\"",
        "Group: { a: \"a\"",
        "a: \"unterminated",
        "a \"a\""
      })
  void rejectsInvalidTokenRules(String tokenRules) {
    assertTimeoutPreemptively(
        Duration.ofSeconds(2),
        () -> assertThrows(GeneratorException.class, () -> parse(tokenRules, "ROOT -> 'a';")));
  }

  @Test
  void errorsIncludeSourceLocation() {
    GeneratorException error =
        assertThrows(GeneratorException.class, () -> parse("a: \"a\"", "ROOT -> ('a';"));
    assertTrue(error.line() >= 5);
    assertTrue(error.column() > 0);
  }

  @Test
  void metadataSkipsDeclarationsAndBracesInsideJavaCommentsAndStrings() {
    String metadata =
        "/* public class Wrong {} */\npackage generated;\npublic final class Actual { String text = \"}\"; }\n";
    var grammar =
        new GrammarV2Parser(new StringReader(metadata + "---\na: \"a\"\n---\nROOT -> 'a';"))
            .parse();
    assertEquals("Actual", grammar.toGrammarSpec().parserClassName());
    assertEquals("generated", grammar.toGrammarSpec().parserPackageName());
    assertThrows(
        GeneratorException.class,
        () ->
            new GrammarV2Parser(
                    new StringReader("public class Broken {\n---\na: \"a\"\n---\nROOT -> 'a';"))
                .parse());
  }

  @Test
  void requiresAllThreeSections() {
    assertThrows(
        GeneratorException.class,
        () ->
            new GrammarV2Parser(new StringReader("public class X {}\n---\nROOT -> 'a';")).parse());
  }
}
