/*
* Copyright (c) 2026 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.lexer.core;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lexengine.commons.Options;

class LexerGeneratorIntegrationTest {
  @TempDir Path directory;

  @Test
  void rejectsEmptyMatchingLexerRules() {
    assertThrows(
        org.lexengine.commons.error.GeneratorException.class,
        () ->
            new LexerGenerator(
                    Options.builder().build(),
                    new StringReader("\"a*\" { return Token.integer(value); }\n"))
                .generate());
  }

  @Test
  void plusRequiresAtLeastOneAndRepeatsThePrecedingExpression() {
    LexRule.Action action = new LexRule.Action("{ return Token.integer(value); }");
    Dfa dfa =
        TestUtils.generateDfa(List.of(new LexRule(null, null, Regex.fromString("[0-9]+"), action)));
    assertNull(dfa.test(""));
    for (String number : List.of("1", "12", "12345")) assertNotNull(dfa.test(number));
  }

  @Test
  void epsilonClosureTerminatesForNullableNestedRepetitions() {
    LexRule.Action action = new LexRule.Action("{ return Token.integer(value); }");
    org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(
        java.time.Duration.ofSeconds(2),
        () -> {
          Dfa dfa =
              TestUtils.generateDfa(
                  List.of(new LexRule(null, null, Regex.fromString("(a?)*"), action)));
          assertNotNull(dfa.test(""));
          assertNotNull(dfa.test("aaa"));
        });
  }

  @Test
  void generatedDfaScannerCompilesAndTokenizesInput() throws Exception {
    String spec = "\"[0-9]+\" { return Token.integer(value); }\n\"[ \\t\\r\\n]+\" { /* skip */ }\n";
    Options options =
        Options.builder()
            .lexerClassName("NumberLexer")
            .lexerPackageName("generated")
            .outDir(directory.toString())
            .build();
    new LexerGenerator(options, new StringReader(spec))
        .generate()
        .writeToDirectory(options.outDir());
    Path token = directory.resolve("Token.java");
    Files.writeString(
        token,
        "package generated; public record Token(String value) { public static Token integer(String value) { return new Token(value); } }");
    ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
    int result =
        ToolProvider.getSystemJavaCompiler()
            .run(
                null,
                diagnostics,
                diagnostics,
                "--release",
                "21",
                "-d",
                directory.toString(),
                directory.resolve("NumberLexer.java").toString(),
                token.toString());
    assertEquals(0, result, diagnostics.toString());
    try (var loader =
        new URLClassLoader(
            new java.net.URL[] {directory.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
      Class<?> lexerClass = loader.loadClass("generated.NumberLexer");
      String large = "7".repeat(2500);
      Object lexer =
          lexerClass
              .getConstructor(Reader.class)
              .newInstance(new StringReader("12 345\n6 " + large + " 8"));
      Object first = lexerClass.getMethod("peek").invoke(lexer);
      assertEquals("12", first.getClass().getMethod("value").invoke(first));
      List<String> values = new ArrayList<>();
      while ((boolean) lexerClass.getMethod("hasNext").invoke(lexer)) {
        Object next = lexerClass.getMethod("nextToken").invoke(lexer);
        values.add((String) next.getClass().getMethod("value").invoke(next));
      }
      assertEquals(List.of("12", "345", "6", large, "8"), values);
    }
  }
}
