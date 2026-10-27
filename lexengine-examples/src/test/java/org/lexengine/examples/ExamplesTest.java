/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.examples;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ExamplesTest {
  @TempDir Path directory;

  static Stream<String> names() {
    return ExampleCatalog.examples().stream().map(ExampleCatalog.Example::name);
  }

  @ParameterizedTest
  @MethodSource("names")
  void compiledExamplesAcceptTheirSamples(String name) {
    var example = ExampleCatalog.find(name);
    assertFalse(example.render().apply(example.sample()).isBlank());
  }

  @Test
  void arithmeticAndListExposeTypedTrees() {
    assertEquals("Expression", ArithmeticExample.parse("12 + 3 * (7 - 4)").root().label());
    assertEquals("NumberList", ListExample.parse("[]").root().label());
    assertThrows(RuntimeException.class, () -> ArithmeticExample.parse("1 + )"));
    assertThrows(RuntimeException.class, () -> ListExample.parse("[1,]"));
  }

  @Test
  void recursiveDescentConfigurationHandlesCommentsAndNestedOptionalLists() {
    assertEquals("Configuration", ConfigurationExample.parse("# empty\n").root().label());
    assertEquals(
        "Configuration",
        ConfigurationExample.parse("a = [true, [1, 2]]; b = \"value\"").root().label());
    assertThrows(RuntimeException.class, () -> ConfigurationExample.parse("a [1, 2]"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "[]",
        "null",
        "true",
        "-1.25e+3",
        "{\"x\":[1,false,null,{}]}",
        "\"line\\nquote\\\"unicode\\u0041\""
      })
  void jsonHandlesItsValueTypesAndEscapes(String source) {
    assertNotNull(JsonExample.parse(source));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"[1,]", "{\"x\":}", "01", "\"unfinished", "true false", "{\"x\":\"a\nb\"}"})
  void jsonRejectsInvalidInput(String source) {
    assertThrows(RuntimeException.class, () -> JsonExample.parse(source));
  }

  @Test
  void standaloneDfaScannerFeedsTheLegacyParser() {
    assertEquals("EXPR", LexerParserExample.parse("12 + count * (7 - 4)").root().symbol());
    assertThrows(RuntimeException.class, () -> LexerParserExample.parse("12 + * 3"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"acd", "bce", "ace", "bcd", "a c d"})
  void canonicalLr1KeepsDistinctReductionLookaheads(String input) {
    assertEquals("S", CanonicalLrExample.parse(input).root().symbol());
  }

  @Test
  void exampleRunnerLoadsInputFilesAndReportsErrors() throws Exception {
    Path input = directory.resolve("numbers with spaces.txt");
    Files.writeString(input, "[1, 2, 3]");
    var output = new ByteArrayOutputStream();
    var errors = new ByteArrayOutputStream();
    var out = new PrintStream(output);
    var err = new PrintStream(errors);
    assertEquals(0, Main.run(new String[] {"list", "--input-file", input.toString()}, out, err));
    assertTrue(output.toString().contains("Accepted list using LL1"));
    assertEquals(1, Main.run(new String[] {"list", "[1,]"}, out, err));
    assertTrue(errors.toString().contains("expected"));
    assertEquals(1, Main.run(new String[] {"unknown"}, out, err));
    assertEquals(1, Main.run(new String[] {"list", "--input-file"}, out, err));
    assertTrue(errors.toString().contains("Missing path"));
  }
}
